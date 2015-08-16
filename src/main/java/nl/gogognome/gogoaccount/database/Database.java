/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.gogognome.gogoaccount.database;

import static com.google.common.collect.Lists.newArrayList;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.dataaccess.transaction.CompositeTransaction;
import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.businessobjects.AccountType;
import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.InvoiceSearchCriteria;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.businessobjects.Party;
import nl.gogognome.gogoaccount.businessobjects.PartySearchCriteria;
import nl.gogognome.gogoaccount.businessobjects.Payment;
import nl.gogognome.gogoaccount.util.Factory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import com.google.common.base.Joiner;
import org.h2.jdbcx.JdbcDataSource;

public class Database {

	/** Description of the bookkeeping represented by this database. */
	private String description = "";

	private Connection connectionToKeepInMemoryDatabaseAlive;

	private final ArrayList<Journal> journals = new ArrayList<Journal>();

	private ArrayList<Party> parties = new ArrayList<Party>();

	/** Maps ids of parties to <code>Party</code> instances. */
	private HashMap<String, Party> idsToPartiesMap = new HashMap<String, Party>();

	/** Maps ids of invoices to <code>Invoice</code> instances. */
	private HashMap<String, Invoice> idsToInvoicesMap = new HashMap<String, Invoice>();

	/** Maps ids of invoices to a map of payment ids to Payments. */
	private final HashMap<String, HashMap<String, Payment>> idToInvoiceToPaymentMap = new HashMap<String, HashMap<String, Payment>>();

	/** Contains the next payment identifier. */
	private String nextPaymentId = "p1";

	/**
	 * Contains the start date of the account period.
	 */
	private Date startOfPeriod = getFirstDayOfYear(new Date());

	/** The currency of all amounts. */
	private Currency currency = Currency.getInstance("EUR");

	/** Indicates whether this database has unsaved changes. */
	private boolean changed;

	/** The name of the file from which the database was loaded. */
	private String fileName;

	/** Maps accounts from imported transactions to accounts of gogo account. */
	private final Map<String, String> importedTransactionAccountToAccountMap = new HashMap<String, String>();

	/**
	 * Contains the <tt>DatabaseListeners</tt>.
	 */
	private final ArrayList<DatabaseListener> listeners = new ArrayList<DatabaseListener>();

    private final String bookkeepingId = UUID.randomUUID().toString();

    private final AccountDAO accountDAO;

    public Database() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:bookkeeping-" + bookkeepingId);
        CompositeDatasourceTransaction.registerDataSource(bookkeepingId, dataSource);
        connectionToKeepInMemoryDatabaseAlive = dataSource.getConnection();

		accountDAO = new AccountDAO(bookkeepingId);
    }

    public AccountDAO getAccountDAO() {
        return accountDAO;
    }

    /**
	 * Adds a database listener.
	 * @param l the database listener.
	 */
	public void addListener( DatabaseListener l ) {
		listeners.add(l);
	}

	/**
	 * Removes a database listener.
	 * @param l the database listener.
	 */
	public void removeListener( DatabaseListener l ) {
		listeners.remove(l);
	}

	/** Notifies the listeners. */
	private void notifyListeners()
	{
		for (int i=0; i<listeners.size(); i++)
		{
			DatabaseListener l = listeners.get(i);
			l.databaseChanged(this);
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

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
		notifyChange();
	}

	private void checkAccountTypes(Iterable<Account> accounts, boolean debet, boolean balanceAccount)
			throws DatabaseModificationFailedException {
		for (Account a : accounts){
			if (a.getType().isDebet() != debet || a.getType().isBalanceAccount() != balanceAccount) {
				throw new DatabaseModificationFailedException("Account found with type "
						+ a.getType() + " instead of " + Joiner.on(", ").join(AccountType.get(debet, balanceAccount)) + ".");
			}
		}
	}

	public Date getStartOfPeriod()
	{
		return startOfPeriod;
	}

	public Currency getCurrency()
	{
		return currency;
	}

	public void setCurrency(Currency currency)
	{
		this.currency = currency;
		notifyChange();
	}

	/**
	 * Sets the start date of the period of the bookkeeping.
	 * @param startOfPeriod the start date of the period
	 */
	public void setStartOfPeriod(Date startOfPeriod) {
		this.startOfPeriod = startOfPeriod;
		notifyChange();
	}

	/**
	 * Adds payments to invoices that are referred to by the journal.
	 * To each invoice (referred to by the journal) a new payment is added for the
	 * corresponding journal item.
	 *
	 * <p>This method does not notify changes in the database!
	 *
	 * @param journal the journal
	 * @throws DatabaseModificationFailedException if creation of payments fails
	 */
	private void createPaymentsForItemsOfJournal(Journal journal) throws DatabaseModificationFailedException {
		JournalItem[] items = journal.getItems();
		for (int i = 0; i < items.length; i++) {
			Invoice invoice = getInvoice(items[i].getInvoiceId());
			if (invoice != null) {
				Payment payment = createPaymentForJournalItem(journal, items[i]);
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
	 * Creates a unique payment id.
	 * @return a unique payment id
	 */
	public String createPaymentId() {
		String result = nextPaymentId;

		StringBuilder sb = new StringBuilder(nextPaymentId);
		char c = sb.charAt(sb.length() - 1);
		if (c < '0' || c > '9') {
			sb.append('0');
		}
		int index = sb.length() - 1;
		boolean done;
		do {
			c = sb.charAt(index);
			c++;
			if (c <= '9') {
				sb.setCharAt(index, c);
				done = true;
			} else {
				sb.setCharAt(index, '0');
				if (index > 0 && sb.charAt(index-1) >= '0' && sb.charAt(index-1) <= '9') {
					index--;
				} else {
					sb.insert(index, '0');
				}
				done = false;
			}
		} while (!done);

		nextPaymentId = sb.toString();

		return result;
	}

	/**
	 * Sets the highest payment ID. This method will typically be called when a database is loaded
	 * from file.
	 * @param highestPaymentId the highest payment id in use thus far.
	 */
	public void setNextPaymentId(String highestPaymentId) {
		nextPaymentId = highestPaymentId;
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
	 * @throws DatabaseModificationFailedException if a problem occurs while adding the journal
	 */
	public void addJournal(Journal journal, boolean createPayments) throws DatabaseModificationFailedException {
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
	 * @throws DatabaseModificationFailedException if a problem occurs while adding the invoice or journal.
	 *         In this case the database will not have been changed.
	 */
	public void addInvoicAndJournal(Invoice invoice, Journal journal) throws DatabaseModificationFailedException {
		String id = invoice.getId();
		if (!id.equals(journal.getIdOfCreatedInvoice())) {
			throw new DatabaseModificationFailedException("The journal does not create the invoice with id " + id);
		}
		if (idsToInvoicesMap.get(id) != null) {
			throw new DatabaseModificationFailedException("An invoice with ID " + id + " already exists!");
		}
		idsToInvoicesMap.put(id, invoice);

		journals.add(journal);
	}

	/**
	 * Removes a journal from the database. Payments booked in the journal are also removed.
	 * @param journal the journal to be deleted
	 * @throws DatabaseModificationFailedException
	 */
	public void removeJournal(Journal journal) throws DatabaseModificationFailedException {
		if (!journals.contains(journal)) {
			throw new DatabaseModificationFailedException("The journal to be removed does not exist.");
		}

		// Remove the journal.
		journals.remove(journal);
	}

	/**
	 * Removes an invoice.
	 * @param invoiceId the id of the invoice
	 * @throws DatabaseModificationFailedException if no invoice with the specified ID exists.
	 */
	public void removeInvoice(String invoiceId) throws DatabaseModificationFailedException {
		Invoice invoice = idsToInvoicesMap.remove(invoiceId);
		if (invoice == null) {
			throw new DatabaseModificationFailedException("No invoice with ID " + invoiceId + " exists.");
		}
	}

	/**
	 * Updates a journal. Payments that are modified by the update of the journal
	 * are updated in the corresponding invoice.
	 * @param oldJournal the journal to be replaced
	 * @param newJournal the journal that replaces <code>oldJournal</code>
	 * @throws DatabaseModificationFailedException if a problem occurs while updating the journal
	 */
	public void updateJournal(Journal oldJournal, Journal newJournal) throws DatabaseModificationFailedException {
		// Check for payments without paymentId. These payments can exist in old XML files.
		JournalItem[] items = oldJournal.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getInvoiceId() != null && items[i].getPaymentId() == null) {
				throw new DatabaseModificationFailedException("The old journal contains a payment without id. It cannot therefore not be updated.");
			}
		}

		int index = journals.indexOf(oldJournal);
		if (index == -1) {
			throw new DatabaseModificationFailedException("The old journal does not exist in the database.");
		}

		journals.set(index, newJournal);

		// Update payments. Remove payments from old journal and add payments of the new journal.
		items = oldJournal.getItems();
		for (int i = 0; i < items.length; i++) {
			Invoice invoice = getInvoice(items[i].getInvoiceId());
			if (invoice != null) {
				removePayment(invoice.getId(), items[i].getPaymentId());
			}
		}
		items = newJournal.getItems();
		for (int i = 0; i < items.length; i++) {
			Invoice invoice = getInvoice(items[i].getInvoiceId());
			if (invoice != null) {
				Payment payment = createPaymentForJournalItem(newJournal, items[i]);
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
		List<Journal> result = new ArrayList<Journal>(journals);
		Collections.sort(result);
		return result;
	}

	/**
	 * Gets the different types of the parties.
	 * @return the types of the parties. Each type occurs exactly ones. The types are sorted lexicographically.
	 */
	public String[] getPartyTypes() {
		HashSet<String> set = new HashSet<String>();
		for (Iterator<Party> iter = parties.iterator(); iter.hasNext();) {
			Party party = iter.next();
			if (party.getType() != null) {
				set.add(party.getType());
			}
		}
		String[] result = set.toArray(new String[set.size()]);
		Arrays.sort(result);
		return result;
	}

	public Party[] getParties() {
		Party[] result = parties.toArray(new Party[parties.size()]);
		Arrays.sort(result);
		return result;
	}

	/**
	 * Gets parties that match the specified search criteria.
	 * @param searchCriteria the search criteria
	 * @return the matching parties. Never returns <code>null</code>.
	 */
	public Party[] getParties(PartySearchCriteria searchCriteria) {
		ArrayList<Party> matchingParties = new ArrayList<Party>();
		for (Iterator<Party> iter = parties.iterator(); iter.hasNext();) {
			Party party = iter.next();
			if (searchCriteria.matches(party)) {
				matchingParties.add(party);
			}
		}
		Party[] result = new Party[matchingParties.size()];
		matchingParties.toArray(result);
		Arrays.sort(result);
		return result;
	}

	/**
	 * Gets a party by id.
	 * @param id the id of the party
	 * @return the party or <code>null</code> if none is present
	 *         with the specified id.
	 */
	public Party getParty(String id)
	{
		return idsToPartiesMap.get(id);
	}

	/**
	 * Sets the parties in the database. Any parties present in the database
	 * are replaced.
	 * @param parties the parties.
	 * @throws DatabaseModificationFailedException if at least two parties were present
	 *         with the same id.
	 */
	public void setParties(Party[] parties) throws DatabaseModificationFailedException {
		ArrayList<Party> newParties = new ArrayList<Party>();
		HashMap<String, Party> newIdsToPartiesMap = new HashMap<String, Party>();
		for (int i = 0; i < parties.length; i++) {
			String id = parties[i].getId();
			newParties.add(parties[i]);
			if (newIdsToPartiesMap.get(id) != null) {
				throw new DatabaseModificationFailedException("A party id " + id + " already exists!");
			}
			newIdsToPartiesMap.put(id, parties[i]);
		}

		// All parties have a unique id.
		this.parties = newParties;
		idsToPartiesMap = newIdsToPartiesMap;
	}

	/**
	 * Adds a party to the database.
	 * @param party the party to be added
	 * @throws DatabaseModificationFailedException if another party exists with the same id.
	 */
	public void addParty(Party party) throws DatabaseModificationFailedException {
		String id = party.getId();
		if (idsToPartiesMap.get(id) != null) {
			throw new DatabaseModificationFailedException("A party with ID " + id + " already exists!");
		}
		parties.add(party);
		idsToPartiesMap.put(id, party);
		notifyChange();
	}

	/**
	 * Updates a party in the database.
	 * @param oldParty the old party (which must exist in the database)
	 * @param newParty the new party (which may not exist yet in the database)
	 * @throws DatabaseModificationFailedException if the IDs of the old and new party differ
	 */
	public void updateParty(Party oldParty, Party newParty) throws DatabaseModificationFailedException {
		if (idsToPartiesMap.get(oldParty.getId()) == null) {
			throw new DatabaseModificationFailedException("A party with ID " + oldParty.getId() + " does not exist!");
		}
		if (!oldParty.getId().equals(newParty.getId())) {
			throw new DatabaseModificationFailedException("The ID of the party cannot be changed!");
		}

		parties.set(parties.indexOf(oldParty), newParty);
		idsToPartiesMap.put(newParty.getId(), newParty);
		notifyChange();
	}

	/**
	 * Removes a party from the database.
	 * @param party the party to be removed
	 * @throws DatabaseModificationFailedException if the party does not exist
	 */
	public void removeParty(Party party) throws DatabaseModificationFailedException {
		if (idsToPartiesMap.get(party.getId()) == null) {
			throw new DatabaseModificationFailedException("A party with ID " + party.getId() + " does not exist!");
		}
		idsToPartiesMap.remove(party.getId());
		parties.remove(party);
		notifyChange();
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
	 * Adds an invoice to the database. Does not notify changes.
	 * @param invoice the invoice to be added
	 * @throws DatabaseModificationFailedException if another invoice exists with the same id.
	 */
	public void addInvoice(Invoice invoice) throws DatabaseModificationFailedException {
		String id = invoice.getId();
		if (idsToInvoicesMap.get(id) != null) {
			throw new DatabaseModificationFailedException("An invoice with ID " + id + " already exists!");
		}
		idsToInvoicesMap.put(id, invoice);
	}

	/**
	 * Updates an existing invoice.
	 * @param id the id of the existing invoice
	 * @param newInvoice the new invoice
	 * @throws DatabaseModificationFailedException if no invoice with the specified id exists or if
	 *         the id of the new invoice differs from the id
	 */
	public void updateInvoice(String id, Invoice newInvoice) throws DatabaseModificationFailedException {
		if (idsToInvoicesMap.get(id) == null) {
			throw new DatabaseModificationFailedException("An invoice with ID " + id + " does not exist, so it cannot be updated!");
		}
		if (!id.equals(newInvoice.getId())) {
			throw new DatabaseModificationFailedException("The ID of the updated invoice differs from the original ID. Therefore, the invoice cannot be udpated!");
		}

		idsToInvoicesMap.put(id, newInvoice);
		notifyChange();
	}

	/**
	 * Sets the invoices for the database. Any invoices present in the database
	 * are replaced.
	 * @param invoices the invoices
	 * @throws DatabaseModificationFailedException if at least two invoices are added
	 *         with the same id.
	 */
	public void setInvoices(Invoice[] invoices) throws DatabaseModificationFailedException {
		ArrayList<Invoice> newInvoices = new ArrayList<Invoice>();
		HashMap<String, Invoice> newIdsToInvoicesMap = new HashMap<String, Invoice>();
		for (int i = 0; i < invoices.length; i++) {
			String id = invoices[i].getId();
			newInvoices.add(invoices[i]);
			if (newIdsToInvoicesMap.get(id) != null) {
				throw new DatabaseModificationFailedException("Two invoices with the id " + id + " are being added!");
			}
			newIdsToInvoicesMap.put(id, invoices[i]);
		}

		idsToInvoicesMap = newIdsToInvoicesMap;
		notifyChange();
	}

	/**
	 * Gets the invoice with the specified id.
	 * @param id the id of the invoice
	 * @return the invoice or <code>null</code> if the invoice was not found
	 */
	public Invoice getInvoice(String id) {
		return idsToInvoicesMap.get(id);
	}

	/**
	 * Gets a suggestion for an unused invoice id.
	 * @param id a possibly existing invoice id
	 * @return an invoice id that does not exist yet in this database
	 */
	public String suggestNewInvoiceId(String id) {
		String newId = id;
		Set<String> existingInvoiceIds = idsToInvoicesMap.keySet();
		int serialNumber = 1;
		do {
			newId = id + "-" + serialNumber;
			serialNumber++;
		} while (existingInvoiceIds.contains(newId));
		return newId;
	}

	/**
	 * Gets all invoices, sorted on ID.
	 * @return the invoices
	 */
	public Invoice[] getInvoices() {
		Invoice[] result = idsToInvoicesMap.values().toArray(new Invoice[idsToInvoicesMap.size()]);
		Arrays.sort(result);
		return result;
	}

	/**
	 * Gets the invoices that match the search criteria.
	 * @param searchCriteria the search criteria
	 * @return the matching invoices. Will never return <code>null</code>.
	 */
	public Invoice[] getInvoices(InvoiceSearchCriteria searchCriteria) {
		ArrayList<Invoice> matchingInvoices = new ArrayList<Invoice>();
		for (Invoice invoice : idsToInvoicesMap.values()) {
			if (searchCriteria.matches(this, invoice)) {
				matchingInvoices.add(invoice);
			}
		}
		Invoice[] result = new Invoice[matchingInvoices.size()];
		matchingInvoices.toArray(result);
		return result;
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
	 * @throws DatabaseModificationFailedException if a problem occurs while adding the payment
	 */
	public void addPayment(String invoiceId, Payment payment) throws DatabaseModificationFailedException {
		if (payment.getId() == null) {
			throw new DatabaseModificationFailedException("The payment has no id");
		}
		HashMap<String, Payment> paymentsForInvoice = idToInvoiceToPaymentMap.get(invoiceId);
		if (paymentsForInvoice == null) {
			paymentsForInvoice = new HashMap<String, Payment>();
			idToInvoiceToPaymentMap.put(invoiceId, paymentsForInvoice);
		}
		if (paymentsForInvoice.get(payment.getId()) != null) {
			throw new DatabaseModificationFailedException("A payment with id " + payment.getId() + " already exists.");
		}
		paymentsForInvoice.put(payment.getId(), payment);
	}

	/**
	 * Removes a payment from an invoice.
	 * @param invoiceId the id of the invoice
	 * @param paymentId the id of the payment
	 * @throws DatabaseModificationFailedException if a problem occurs while deleting the payment
	 */
	public void removePayment(String invoiceId, String paymentId) throws DatabaseModificationFailedException {
		HashMap<String, Payment> paymentsForInvoice = idToInvoiceToPaymentMap.get(invoiceId);
		if (paymentsForInvoice != null) {
			if (paymentsForInvoice.get(paymentId) == null) {
				throw new DatabaseModificationFailedException("No payment with the id " + paymentId + " exists for invoice " + invoiceId);
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
		ArrayList<Payment> payments = new ArrayList<Payment>(10);
		if (paymentsForInvoice != null) {
			payments.addAll(paymentsForInvoice.values());
			Collections.sort(payments, new Comparator<Payment>() {
				@Override
				public int compare(Payment o1, Payment o2) {
					return DateUtil.compareDayOfYear(o1.getDate(), o2.getDate());
				}
			});
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
	public Account getAccountForImportedAccount(String importedAccount) throws SQLException {
		String accountId = importedTransactionAccountToAccountMap.get(importedAccount);
		if (accountId != null) {
			return accountDAO.get(accountId);
		} else {
			return null;
		}
	}

	public Map<String, String> getImportedTransactionAccountToAccountMap() {
		return importedTransactionAccountToAccountMap;
	}

	private static Date getFirstDayOfYear(Date date) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DATE, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

}
