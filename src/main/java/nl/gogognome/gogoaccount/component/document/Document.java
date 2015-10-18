package nl.gogognome.gogoaccount.component.document;

import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.PartyService;
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
