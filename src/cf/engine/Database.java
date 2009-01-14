/*
 * $Id: Database.java,v 1.38 2009-01-14 21:32:15 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import nl.gogognome.text.Amount;
import nl.gogognome.util.DateUtil;

/**
 * This class maintains all accounts, journals, debtors and
 * creditors for the bookkeeping.
 *
 * @author Sander Kooijmans
 */
public class Database {

    /** The singleton instance of this class. */
    private static Database instance;
    
    /** Description of the bookkeeping represented by this database. */
    private String description = "empty bookkeeping";
    
    private Account[] assets = new Account[0];
    
    private Account[] liabilities = new Account[0];
    
    private Account[] revenues = new Account[0];
    
    private Account[] expenses = new Account[0];
    
    private ArrayList<Journal> journals = new ArrayList<Journal>();
    
    private ArrayList<Party> parties = new ArrayList<Party>();

    /** Maps ids of accounts to <code>Account</code>s. */
    private HashMap<String, Account> idsToAccountsMap = new HashMap<String, Account>();
    
    /** Maps ids of parties to <code>Party</code> instances. */
    private HashMap<String, Party> idsToPartiesMap = new HashMap<String, Party>();
    
    /** Maps ids of invoices to <code>Invoice</code> instances. */
    private HashMap<String, Invoice> idsToInvoicesMap = new HashMap<String, Invoice>();

    /** Maps ids of invoices to a map of payment ids to Payments. */
    private HashMap<String, HashMap<String, Payment>> idToInvoiceToPaymentMap = new HashMap<String, HashMap<String, Payment>>();

    /** Contains the next payment identifier. */
    private String nextPaymentId = "p1";
    
    /** 
     * Contains the start date of the account period.
     */
    private Date startOfPeriod;
    
	/** The currency of all amounts. */
	private Currency currency = Currency.getInstance("EUR");
    
	/** Indicates whether this database has unsaved changes. */
	private boolean changed;
	
	/** The name of the file from which the database was loaded. */
	private String fileName;
	
	/** 
	 * Contains the <tt>DatabaseListeners</tt>. 
	 */
	private ArrayList<DatabaseListener> listeners = new ArrayList<DatabaseListener>();
	
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
			l.databaseChanged(instance);
		}
	}
	
	/** 
	 * This method is called each time the database changes.
	 * This method will make sure that the <tt>DatabaseListener</tt>s get notified
	 * at the proper moment only if this database is the current database.
	 */
	public void notifyChange() {
		if (instance == this) {
			changed = true;
			notifyListeners();
		}
	}
	
	/** 
	 * This method is called to indicate that the database is consistent with the
	 * file it was last loaded from or saved to.
	 */
	public void databaseConsistentWithFile() 
	{
		changed = false;
		// This is the only place where an update takes without calling notifyChange().
		// The reason for this, is that notifyChange() will mark the database as
		// changed, while this method is called to indicate that the database has
		// not been changed since the last load or save action. 
		notifyListeners();
	}
	
    /**
     * Gets the singleton instance of this class.
     * @return the singleton instance of this class
     * @deprecated do not use this anymore
     */
    public static synchronized Database getInstance()
    {
        if (instance == null)
        {
            instance = new Database();
        }
        return instance;
    }
    
    /**
     * Sets the singleton instance of this class.
     * @param instance the singleton instance of this class
     * @deprecated do not use this method anymore
     */
    public static synchronized void setInstance(Database instance)
    {
        Database.instance = instance;
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
    
    /**
     * Gets an account based on its id.
     * @param id the id of the account
     * @return the account or <code>null</code> if no account was
     *         found with the specified id.
     */
    public Account getAccount(String id) {
        return idsToAccountsMap.get(id);
    }
    
    public Account[] getAssets() 
    {
        return assets;
    }
    
    /**
     * Sets the assets.
     * @param assets the assets
     * @throws DatabaseModificationFailedException if there is an asset for which holds
     *         <code>!asset.isDebet()</code>.
     */
    void setAssets(Account[] assets) throws DatabaseModificationFailedException
    {
        for (int i=0; i<assets.length; i++)
        {
            if (!assets[i].isDebet())
            {
                throw new DatabaseModificationFailedException("Assets must have debet equal to true!");
            }
        }
        
        Arrays.sort(assets);
        this.assets = assets;
        updateIdsToAccountsMap();
    }
    
    public Account[] getExpenses() 
    {
        return expenses;
    }
    
    /**
     * Sets the expenses.
     * @param expenses the expenses
     * @throws DatabaseModificationFailedException if there is an expense for which holds
     *         <code>!expense.isDebet()</code>
     */
    void setExpenses(Account[] expenses) throws DatabaseModificationFailedException 
    {
        for (int i=0; i<expenses.length; i++)
        {
            if (!expenses[i].isDebet())
            {
                throw new DatabaseModificationFailedException("Expenses must have debet equal to true!");
            }
        }
        
        Arrays.sort(expenses);
        this.expenses = expenses;
        updateIdsToAccountsMap();
    }
    
    public Account[] getLiabilities() 
    {
        return liabilities;
    }
    
    void setLiabilities(Account[] liabilities) throws DatabaseModificationFailedException
    {
        for (int i=0; i<liabilities.length; i++)
        {
            if (liabilities[i].isDebet())
            {
                throw new DatabaseModificationFailedException("Liabilities must have debet equal to false!");
            }
        }
        
        Arrays.sort(liabilities);
        this.liabilities = liabilities;
        updateIdsToAccountsMap();
    }
    
    public Account[] getRevenues() {
        return revenues;
    }
    
    void setRevenues(Account[] revenues) throws DatabaseModificationFailedException {
        for (int i=0; i<revenues.length; i++) {
            if (revenues[i].isDebet()) {
                throw new DatabaseModificationFailedException("Revenues must have debet equal to false!");
            }
        }
        
        Arrays.sort(revenues);
        this.revenues = revenues;
        updateIdsToAccountsMap();
    }

    /**
     * Gets all accounts of this database. The accounts are a concatenation of the
     * assets, liabilities, expenses and revenues.
     * @return all accounts of this database
     */
    public Account[] getAllAccounts() {
        Account[] assets = getAssets();
        Account[] liabilities = getLiabilities();
        Account[] expenses = getExpenses();
        Account[] revenues = getRevenues();
        Account[] accounts = new Account[assets.length + liabilities.length + expenses.length + revenues.length];
        System.arraycopy(assets, 0, accounts, 0, assets.length);
        System.arraycopy(liabilities, 0, accounts, assets.length, liabilities.length);
        System.arraycopy(expenses, 0, accounts, assets.length + liabilities.length, expenses.length);
        System.arraycopy(revenues, 0, accounts, assets.length + liabilities.length + expenses.length, revenues.length);
        return accounts;
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
        invoice.setDatabase(this);

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

        // Check for payments without payment ID.
        for (JournalItem item : journal.getItems()) {
            if (item.getInvoiceId() != null && item.getPaymentId() == null) {
                throw new DatabaseModificationFailedException("The journal has a payment without an id. Therefore, it cannot be removed.");
            }
        }

        // Remove payments.
        for (JournalItem item : journal.getItems()) {
            String invoiceId = item.getInvoiceId();
            String paymentId = item.getPaymentId();
            if (invoiceId != null && paymentId != null) {
                removePayment(invoiceId, paymentId);
            }
        }
        
        // Remove the journal.
        journals.remove(journal);
        
        notifyChange();
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
     * Sets the debtors in the database. Any debtors present in the database
     * are replaced.
     * @param debtors the debtors.
     * @throws DatabaseModificationFailedException if at least two debtors were present 
     *         with the same id.
     */
    void setParties(Party[] parties) throws DatabaseModificationFailedException {
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
    
    /**
     * Updates the map <code>idsToAccountsMap</code> based on all
     * accounts registered at this database.
     */
    private void updateIdsToAccountsMap()
    {
        idsToAccountsMap.clear();
        for (int i=0; i<assets.length; i++)
        {
            idsToAccountsMap.put(assets[i].getId(), assets[i]);
        }
        for (int i=0; i<liabilities.length; i++)
        {
            idsToAccountsMap.put(liabilities[i].getId(), liabilities[i]);
        }
        for (int i=0; i<revenues.length; i++)
        {
            idsToAccountsMap.put(revenues[i].getId(), revenues[i]);
        }
        for (int i=0; i<expenses.length; i++)
        {
            idsToAccountsMap.put(expenses[i].getId(), expenses[i]);
        }
    }
    
    /**
     * Gets the balance for the specified date.
     * @param date the date
     * @return the balance
     */
    public Balance getBalance(Date date) {
        return new Balance(this, date);
    }
    
    /**
     * Gets the operation result for the specified date.
     * @param date the date
     * @return the operation result
     */
    public OperationalResult getOperationalResult(Date date) {
        return new OperationalResult(this, date);
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
     * Checks whether any accounts are present in this database.
     * @return <code>true</code> if any account is present; otherwise <code>false</code>
     * 
     */
    public boolean hasAccounts()
    {
        return assets.length + liabilities.length + expenses.length + revenues.length > 0;
    }
    
    /**
     * Gets the balance for a party at the specified date. The balance consists
     * of the total debet amount minus the total credit amount of the party.
     * 
     * @param party the party
     * @param date the date
     * @return the balance for the party
     */
    public Amount getBalanceForParty(Party party, Date date) {
        Amount result = Amount.getZero(getCurrency());
        for (Invoice invoice : idsToInvoicesMap.values()) {
            if (invoice.getPayingParty().equals(party)) {
                if (DateUtil.compareDayOfYear(invoice.getIssueDate(), date) <= 0) {
                    result = result.add(invoice.getAmountToBePaid());
                }
                List<Payment> payments = invoice.getPayments();
                for (Payment p : payments) {
                    if (DateUtil.compareDayOfYear(p.getDate(), date) <= 0) {
                        result = result.subtract(p.getAmount());
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Gets the debtors at the specified date. Debtors are parties that still have 
     * to pay money to the club.
     * @param date the date
     * @return the debtors
     */
    public Party[] getDebtors(Date date) {
        ArrayList<Party> debtors = new ArrayList<Party>();
        for (int i=0; i<parties.size(); i++) {
            Party party = parties.get(i);
            Amount balance = getBalanceForParty(party, date);
            if (balance.isPositive()) {
                debtors.add(party);
            }
        }
        Party[] result = debtors.toArray(new Party[debtors.size()]);
        return result;
    }
    
    /**
     * Gets the creditors at the specified date. Creditors are parties to which
     * the club owes money.
     * @param date the date
     * @return the creditors
     */
    public Party[] getCreditors(Date date) {
        ArrayList<Party> creditors = new ArrayList<Party>();
        for (int i=0; i<parties.size(); i++) {
            Party party = parties.get(i);
            Amount balance = getBalanceForParty(party, date);
            if (balance.isNegative()) {
                creditors.add(party);
            }
        }
        Party[] result = creditors.toArray(new Party[creditors.size()]);
        return result;
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
        invoice.setDatabase(this);
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
        
        idsToInvoicesMap.get(id).setDatabase(null);
        idsToInvoicesMap.put(id, newInvoice);
        newInvoice.setDatabase(this);
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
        
        // All invoices have a unique id.
        // Remove old invoices.
        for (Invoice invoice : idsToInvoicesMap.values()) {
            invoice.setDatabase(null);
        }
        // Add new invoices
        for (Invoice invoice : invoices) {
            invoice.setDatabase(this);
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
            if (searchCriteria.matches(invoice)) {
                matchingInvoices.add(invoice);
            }
        }
        Invoice[] result = new Invoice[matchingInvoices.size()];
        matchingInvoices.toArray(result);
        return result;
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
        notifyChange();
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
                public int compare(Payment o1, Payment o2) {
                    return DateUtil.compareDayOfYear(o1.getDate(), o2.getDate());
                }
            });
        }
        return payments;
    }
    
}
