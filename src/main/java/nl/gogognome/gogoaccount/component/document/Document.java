package nl.gogognome.gogoaccount.component.document;

import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import org.h2.jdbcx.JdbcDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class Document {

    private Connection connectionToKeepInMemoryDatabaseAlive;

    /** Indicates whether this database has unsaved changes. */
    private boolean changed;

    /** The name of the file from which the database was loaded. */
    private String fileName;

    private Locale locale = Locale.US;

    /** Maps accounts from imported transactions to accounts of gogo account. */
    private final Map<String, String> importedTransactionAccountToAccountMap = new HashMap<>();

    /**
     * Contains the <tt>DatabaseListeners</tt>.
     */
    private final ArrayList<DocumentListener> listeners = new ArrayList<>();

    private final String bookkeepingId = UUID.randomUUID().toString();

    public Document() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:bookkeeping-" + bookkeepingId);
        CompositeDatasourceTransaction.registerDataSource(bookkeepingId, dataSource);
        connectionToKeepInMemoryDatabaseAlive = dataSource.getConnection();
    }

    public String getBookkeepingId() {
        return bookkeepingId;
    }

    /**
     * Adds a database listener.
     * @param l the database listener.
     */
    public void addListener( DocumentListener l ) {
        listeners.add(l);
    }

    /**
     * Removes a database listener.
     * @param l the database listener.
     */
    public void removeListener( DocumentListener l ) {
        listeners.remove(l);
    }

    /** Notifies the listeners. */
    private void notifyListeners()
    {
        for (DocumentListener l : listeners) {
            l.documentChanged(this);
        }
    }

    /**
     * This method is called each time the database changes.
     * This method will make sure that the <tt>DatabaseListener</tt>s get notified
     * at the proper moment only if this database is the current database.
     */
    public void notifyChange() {
        changed = true;
        notifyListeners();
    }

    /**
     * This method is called to indicate that the database is consistent with the
     * file it was last loaded from or saved to.
     */
    public void databaseConsistentWithFile() {
        changed = false;
        // This is the only place where an update takes without calling notifyChange().
        // The reason for this, is that notifyChange() will mark the database as
        // changed, while this method is called to indicate that the database has
        // not been changed since the last load or save action.
        notifyListeners();
    }

    public boolean hasUnsavedChanges()
    {
        return changed;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Adds payments to invoices that are referred to by the journal.
     * To each invoice (referred to by the journal) a new payment is added for the
     * corresponding journal item.
     *
     * <p>This method does not notify changes in the database!
     *
     * @param journalEntry the journal
     * @throws DocumentModificationFailedException if creation of payments fails
     */
    private void createPaymentsForItemsOfJournal(JournalEntry journalEntry) throws DocumentModificationFailedException, ServiceException {
        JournalEntryDetail[] items = journalEntry.getItems();
        InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
        for (JournalEntryDetail item : items) {
            if (item.getInvoiceId() != null) {
                Payment payment = createPaymentForJournalItem(journalEntry, item);
                payment = invoiceService.createPayment(this, payment);
                item.setPaymentId(payment.getId());
            }
        }
    }

    /**
     * Creates a payment for a journal item.
     * @param journalEntry the journal that contains the item
     * @param journalEntryDetail the journal item
     * @return the payment
     */
    private Payment createPaymentForJournalItem(JournalEntry journalEntry, JournalEntryDetail journalEntryDetail) {
        Amount amount;
        if (journalEntryDetail.isDebet()) {
            amount = journalEntryDetail.getAmount();
        } else {
            amount = journalEntryDetail.getAmount().negate();
        }
        Date date = journalEntry.getDate();
        String description = journalEntryDetail.getAccount().getName();
        Payment payment = new Payment(journalEntryDetail.getPaymentId());
        payment.setDescription(description);
        payment.setAmount(amount);
        payment.setDate(date);
        payment.setInvoiceId(journalEntryDetail.getInvoiceId());
        return payment;
    }

    /**
     * Adds a journal to the database.
     *
     * <p>Optionally, this method can update invoices that are referred to by the journal.
     * To each invoice (referred to by the journal) a new payment is added for the
     * corresponding journal item.
     *
     * @param journalEntry the journal to be added
     * @param createPayments <code>true</code> if payments have to be added for invoices referred
     *        to by the journal; <code>false</code> if no payments are not to be created.
     * @throws DocumentModificationFailedException if a problem occurs while adding the journal
     */
    public void addJournal(JournalEntry journalEntry, boolean createPayments) throws DocumentModificationFailedException, ServiceException {
        if (createPayments) {
            createPaymentsForItemsOfJournal(journalEntry);
        }
        journalEntries.add(journalEntry);
        notifyChange();
    }

    /**
     * Removes a journal from the database. Payments booked in the journal are also removed.
     * @param journalEntry the journal to be deleted
     * @throws DocumentModificationFailedException
     */
    public void removeJournal(JournalEntry journalEntry) throws DocumentModificationFailedException {
        if (!journalEntries.contains(journalEntry)) {
            throw new DocumentModificationFailedException("The journal to be removed does not exist.");
        }

        // Remove the journal.
        journalEntries.remove(journalEntry);
    }

    /**
     * Updates a journal. Payments that are modified by the update of the journal
     * are updated in the corresponding invoice.
     * @param oldJournalEntry the journal to be replaced
     * @param newJournalEntry the journal that replaces <code>oldJournal</code>
     * @throws DocumentModificationFailedException if a problem occurs while updating the journal
     */
    public void updateJournal(JournalEntry oldJournalEntry, JournalEntry newJournalEntry) throws DocumentModificationFailedException, ServiceException {
        // Check for payments without paymentId. These payments can exist in old XML files.
        JournalEntryDetail[] items = oldJournalEntry.getItems();
        for (JournalEntryDetail item2 : items) {
            if (item2.getInvoiceId() != null && item2.getPaymentId() == null) {
                throw new DocumentModificationFailedException("The old journal contains a payment without id. It cannot therefore not be updated.");
            }
        }

        int index = journalEntries.indexOf(oldJournalEntry);
        if (index == -1) {
            throw new DocumentModificationFailedException("The old journal does not exist in the database.");
        }

        journalEntries.set(index, newJournalEntry);

        // Update payments. Remove payments from old journal and add payments of the new journal.
        InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
        items = oldJournalEntry.getItems();
        for (JournalEntryDetail item : items) {
            if (item.getPaymentId() != null) {
                invoiceService.removePayment(this, item.getPaymentId());
            }
        }
        items = newJournalEntry.getItems();
        for (JournalEntryDetail item : items) {
            if (item.getInvoiceId() != null) {
                Payment payment = createPaymentForJournalItem(newJournalEntry, item);
                invoiceService.createPayment(this, payment);
            }
        }

        notifyChange();
    }

    /**
     * Gets the journals of the database
     * @return the journals sorted on date
     */
    public List<JournalEntry> getJournalEntries() {
        List<JournalEntry> result = new ArrayList<>(journalEntries);
        Collections.sort(result);
        return result;
    }


    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
        notifyChange();
    }

    /**
     * Gets the journal that creates the specified invoice.
     * @param invoiceId the id of the invoice
     * @return the journal or null if no creating journal exists. The latter
     *         typically happens when the invoice was created in the previous
     *         year.
     */
    public JournalEntry getCreatingJournal(String invoiceId) {
        for (JournalEntry j : journalEntries) {
            if (invoiceId.equals(j.getIdOfCreatedInvoice())) {
                return j;
            }
        }
        return null;
    }

    /**
     * Checks whether an account is used in the database. If it is unused, the account
     * can be removed from the database without destroying its integrity.
     * @param accountId the ID of the account
     * @return <code>true</code> if the account is used; <code>false</code> if the account is unused
     */
    public boolean isAccountUsed(String accountId) {
        for (JournalEntry journalEntry : journalEntries) {
            for (JournalEntryDetail item : journalEntry.getItems()) {
                if (item.getAccount().getId().equals(accountId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets a link between an account of an imported transaction and an account
     * of gogo account.
     * @param importedAccount the account of an imported transaction
     * @param accountId the id of the account in gogo account
     */
    public void setImportedAccount(String importedAccount, String accountId) {
        importedTransactionAccountToAccountMap.put(importedAccount, accountId);
        notifyChange();
    }

    /**
     * Gets the account that corresponds to an account of an imported transaction.
     * @param importedAccount the account of an imported transaction
     * @return the account or null if no corresponding account is found
     */
    public Account getAccountForImportedAccount(String importedAccount) throws ServiceException {
        String accountId = importedTransactionAccountToAccountMap.get(importedAccount);
        if (accountId != null) {
            return new ConfigurationService().getAccount(this, accountId);
        } else {
            return null;
        }
    }

    public Map<String, String> getImportedTransactionAccountToAccountMap() {
        return importedTransactionAccountToAccountMap;
    }

    public String toString(Amount amount) {
        return amount != null ? new AmountFormat(getLocale()).formatAmount(amount): null;
    }

    public Amount toAmount(String string) throws SQLException {
        if (string == null) {
            return null;
        }
        try {
            AmountFormat amountFormat = new AmountFormat(getLocale());
            return amountFormat.parse(string);
        } catch (ParseException e) {
            throw new SQLException("Could not parse amount " + string);
        }
    }

}
