package nl.gogognome.gogoaccount.components.document;

import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.businessobjects.Payment;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import org.h2.jdbcx.JdbcDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class Document {

	private Connection connectionToKeepInMemoryDatabaseAlive;

	private final ArrayList<Journal> journals = new ArrayList<>();

	/** Maps ids of invoices to a map of payment ids to Payments. */
	private final HashMap<String, HashMap<String, Payment>> idToInvoiceToPaymentMap = new HashMap<>();

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
	 * @param journal the journal
	 * @throws DocumentModificationFailedException if creation of payments fails
	 */
	private void createPaymentsForItemsOfJournal(Journal journal) throws DocumentModificationFailedException, ServiceException {
		JournalItem[] items = journal.getItems();
        InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
		for (JournalItem item : items) {
			Invoice invoice = invoiceService.getInvoice(this, item.getInvoiceId());
			if (invoice != null) {
				Payment payment = createPaymentForJournalItem(journal, item);
				addPayment(invoice.getId(), payment);
			}
		}
	}

	/**
	 * Creates a payment for a journal item.
	 * @param journal the journal that contains the item
	 * @param journalItem the journal item
	 * @return the payment
	 */
	private Payment createPaymentForJournalItem(Journal journal, JournalItem journalItem) {
		Amount amount;
		if (journalItem.isDebet()) {
			amount = journalItem.getAmount();
		} else {
			amount = journalItem.getAmount().negate();
		}
		Date date = journal.getDate();
		String description = journalItem.getAccount().getName();
		return new Payment(journalItem.getPaymentId(), amount, date, description);
	}

	/**
	 * Adds a journal to the database.
	 *
	 * <p>Optionally, this method can update invoices that are referred to by the journal.
	 * To each invoice (referred to by the journal) a new payment is added for the
	 * corresponding journal item.
	 *
	 * @param journal the journal to be added
	 * @param createPayments <code>true</code> if payments have to be added for invoices referred
	 *        to by the journal; <code>false</code> if no payments are not to be created.
	 * @throws DocumentModificationFailedException if a problem occurs while adding the journal
	 */
	public void addJournal(Journal journal, boolean createPayments) throws DocumentModificationFailedException, ServiceException {
		if (createPayments) {
			createPaymentsForItemsOfJournal(journal);
		}
		journals.add(journal);
		notifyChange();
	}

	/**
	 * Adds an invoice and journal to the database. The invoice is created by the journal.
	 * <p>This method will either completely succeed or completely fail.
	 * @param invoice the invoice
	 * @param journal the journal
	 * @throws DocumentModificationFailedException if a problem occurs while adding the invoice or journal.
	 *         In this case the database will not have been changed.
	 */
	public void addInvoicAndJournal(Invoice invoice, Journal journal) throws DocumentModificationFailedException {
		String id = invoice.getId();
		if (!id.equals(journal.getIdOfCreatedInvoice())) {
			throw new DocumentModificationFailedException("The journal does not create the invoice with id " + id);
		}

		journals.add(journal);
	}

	/**
	 * Removes a journal from the database. Payments booked in the journal are also removed.
	 * @param journal the journal to be deleted
	 * @throws DocumentModificationFailedException
	 */
	public void removeJournal(Journal journal) throws DocumentModificationFailedException {
		if (!journals.contains(journal)) {
			throw new DocumentModificationFailedException("The journal to be removed does not exist.");
		}

		// Remove the journal.
		journals.remove(journal);
	}

	/**
	 * Updates a journal. Payments that are modified by the update of the journal
	 * are updated in the corresponding invoice.
	 * @param oldJournal the journal to be replaced
	 * @param newJournal the journal that replaces <code>oldJournal</code>
	 * @throws DocumentModificationFailedException if a problem occurs while updating the journal
	 */
	public void updateJournal(Journal oldJournal, Journal newJournal) throws DocumentModificationFailedException, ServiceException {
		// Check for payments without paymentId. These payments can exist in old XML files.
		JournalItem[] items = oldJournal.getItems();
		for (JournalItem item2 : items) {
			if (item2.getInvoiceId() != null && item2.getPaymentId() == null) {
				throw new DocumentModificationFailedException("The old journal contains a payment without id. It cannot therefore not be updated.");
			}
		}

		int index = journals.indexOf(oldJournal);
		if (index == -1) {
			throw new DocumentModificationFailedException("The old journal does not exist in the database.");
		}

		journals.set(index, newJournal);

		// Update payments. Remove payments from old journal and add payments of the new journal.
		InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
		items = oldJournal.getItems();
		for (JournalItem item1 : items) {
			Invoice invoice = invoiceService.getInvoice(this, item1.getInvoiceId());
			if (invoice != null) {
				removePayment(invoice.getId(), item1.getPaymentId());
			}
		}
		items = newJournal.getItems();
		for (JournalItem item : items) {
			Invoice invoice = invoiceService.getInvoice(this, item.getInvoiceId());
			if (invoice != null) {
				Payment payment = createPaymentForJournalItem(newJournal, item);
				addPayment(invoice.getId(), payment);
			}
		}

		notifyChange();
	}

	/**
	 * Gets the journals of the database
	 * @return the journals sorted on date
	 */
	public List<Journal> getJournals() {
		List<Journal> result = new ArrayList<>(journals);
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
	public Journal getCreatingJournal(String invoiceId) {
		for (Journal j : journals) {
			if (invoiceId.equals(j.getIdOfCreatedInvoice())) {
				return j;
			}
		}
		return null;
	}

	/**
	 * Adds a payment to an invoice.
	 * @param invoiceId the invoice
	 * @param payment the payment
	 * @throws DocumentModificationFailedException if a problem occurs while adding the payment
	 */
	public void addPayment(String invoiceId, Payment payment) throws DocumentModificationFailedException {
		if (payment.getId() == null) {
			throw new DocumentModificationFailedException("The payment has no id");
		}
		HashMap<String, Payment> paymentsForInvoice = idToInvoiceToPaymentMap.get(invoiceId);
		if (paymentsForInvoice == null) {
			paymentsForInvoice = new HashMap<>();
			idToInvoiceToPaymentMap.put(invoiceId, paymentsForInvoice);
		}
		if (paymentsForInvoice.get(payment.getId()) != null) {
			throw new DocumentModificationFailedException("A payment with id " + payment.getId() + " already exists.");
		}
		paymentsForInvoice.put(payment.getId(), payment);
	}

	/**
	 * Removes a payment from an invoice.
	 * @param invoiceId the id of the invoice
	 * @param paymentId the id of the payment
	 * @throws DocumentModificationFailedException if a problem occurs while deleting the payment
	 */
	public void removePayment(String invoiceId, String paymentId) throws DocumentModificationFailedException {
		HashMap<String, Payment> paymentsForInvoice = idToInvoiceToPaymentMap.get(invoiceId);
		if (paymentsForInvoice != null) {
			if (paymentsForInvoice.get(paymentId) == null) {
				throw new DocumentModificationFailedException("No payment with the id " + paymentId + " exists for invoice " + invoiceId);
			}
			paymentsForInvoice.remove(paymentId);
			notifyChange();
		}
	}

	/**
	 * Gets all payments for the specified invoice.
	 * @param invoiceId the ID of the invoice
	 * @return the payments
	 */
	public List<Payment> getPayments(String invoiceId) {
		HashMap<String, Payment> paymentsForInvoice = idToInvoiceToPaymentMap.get(invoiceId);
		ArrayList<Payment> payments = new ArrayList<>(10);
		if (paymentsForInvoice != null) {
			payments.addAll(paymentsForInvoice.values());
			Collections.sort(payments, (o1, o2) -> DateUtil.compareDayOfYear(o1.getDate(), o2.getDate()));
		}
		return payments;
	}

	/**
	 * Checks whether an account is used in the database. If it is unused, the account
	 * can be removed from the database without destroying its integrity.
	 * @param accountId the ID of the account
	 * @return <code>true</code> if the account is used; <code>false</code> if the account is unused
	 */
	public boolean isAccountUsed(String accountId) {
		for (Journal journal : journals) {
			for (JournalItem item : journal.getItems()) {
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
}
