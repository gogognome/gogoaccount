/*
 * $Id: Database.java,v 1.29 2008-01-10 19:18:07 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import nl.gogognome.text.Amount;
import nl.gogognome.util.DateUtil;
import cf.engine.Invoice.Payment;

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

    private ArrayList<Invoice> invoices = new ArrayList<Invoice>();
    
    /** Maps ids of accounts to <code>Account</code>s. */
    private HashMap<String, Account> idsToAccountsMap = new HashMap<String, Account>();
    
    /** Maps ids of parties to <code>Party</code> instances. */
    private HashMap<String, Party> idsToPartiesMap = new HashMap<String, Party>();
    
    /** Maps ids of invoices to <code>Invoice</code> instances. */
    private HashMap<String, Invoice> idsToInvoicesMap = new HashMap<String, Invoice>();
    
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
	private void notifyChange() 
	{
		if (instance == this) 
		{
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
    public void setAssets(Account[] assets) throws DatabaseModificationFailedException
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
        notifyChange();
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
    public void setExpenses(Account[] expenses) throws DatabaseModificationFailedException 
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
        notifyChange();
    }
    
    public Account[] getLiabilities() 
    {
        return liabilities;
    }
    
    public void setLiabilities(Account[] liabilities) throws DatabaseModificationFailedException
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
        notifyChange();
    }
    
    public Account[] getRevenues() 
    {
        return revenues;
    }
    
    public void setRevenues(Account[] revenues) throws DatabaseModificationFailedException 
    {
        for (int i=0; i<revenues.length; i++)
        {
            if (revenues[i].isDebet())
            {
                throw new DatabaseModificationFailedException("Revenues must have debet equal to false!");
            }
        }
        
        Arrays.sort(revenues);
        this.revenues = revenues;
        updateIdsToAccountsMap();
        notifyChange();
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
    
    public void setStartOfPeriod(Date startOfPeriod) {
        this.startOfPeriod = startOfPeriod;
        notifyChange();
    }
    
    /**
     * Sets the journals in the database.
     * @param journals the new journals of the database.
     */
    public void setJournals(Journal[] journals) {
        this.journals.clear();
        for (int i=0; i<journals.length; i++) {
            this.journals.add(journals[i]);
        }
        notifyChange();
    }
    
    public void addJournal(Journal journal)
    {
        journals.add(journal);
        notifyChange();
    }
    
    public Journal[] getJournals() {
        Journal[] result = journals.toArray(new Journal[journals.size()]);
        Arrays.sort(result);
        return result;
    }
    
    /**
     * Gets the different types of the parties.
     * @return the types of the parties. Each type occurs exactly ones. The types are sorted lexicographically.
     */
    public String[] getPartyTypes() {
        HashSet<String> set = new HashSet<String>();
        for (Iterator iter = parties.iterator(); iter.hasNext();) {
            Party party = (Party) iter.next();
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

    public Party[] getParties(PartySearchCriteria searchCriteria) {
        ArrayList<Party> matchingParties = new ArrayList<Party>();
        for (Iterator iter = parties.iterator(); iter.hasNext();) {
            Party party = (Party) iter.next();
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
		notifyChange();
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
     * @throws DatabaseModificationFailedException if another party exists with the same id as <code>newParty</code> 
     *         and if <code>oldParty</code>'s ID is different from <code>newParty</code>'s ID or if
     *         <code>oldParty</code> does not exist in the database 
     */
    public void updateParty(Party oldParty, Party newParty) throws DatabaseModificationFailedException {
        if (idsToPartiesMap.get(oldParty.getId()) == null) {
            throw new DatabaseModificationFailedException("A party with ID " + oldParty.getId() + " does not exist!");
        }
        if (!oldParty.getId().equals(newParty.getId())) {
            if (idsToPartiesMap.get(newParty.getId()) != null) {
                throw new DatabaseModificationFailedException("A party with ID " + newParty.getId() + " already exists!");
            }
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
        for (Iterator<Invoice> iter = invoices.iterator(); iter.hasNext();) {
            Invoice invoice = iter.next();
            if (invoice.getPayingParty().equals(party)) {
                if (DateUtil.compareDayOfYear(invoice.getIssueDate(), date) <= 0) {
                    result = result.add(invoice.getAmountToBePaid());
                    
                    Payment[] payments = invoice.getPayments();
                    for (int j = 0; j < payments.length; j++) {
                        if (DateUtil.compareDayOfYear(payments[j].date, date) <= 0) {
                            result = result.add(payments[j].amount);
                        }
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
     * Adds an invoice to the database.
     * @param invoice the invoice to be added
     * @throws DatabaseModificationFailedException if another invoice exists with the same id. 
     */
    public void addInvoice(Invoice invoice) throws DatabaseModificationFailedException {
        String id = invoice.getId();
        if (idsToInvoicesMap.get(id) != null) {
            throw new DatabaseModificationFailedException("An invoice with ID " + id + " already exists!");
        }
        invoices.add(invoice);
        idsToInvoicesMap.put(id, invoice);
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
        this.invoices = newInvoices;
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
     * Gets all invoices, sorted on ID.
     * @return the invoices
     */
    public Invoice[] getInvoices() {
        Invoice[] result = invoices.toArray(new Invoice[invoices.size()]);
        Arrays.sort(result);
        return result;
    }
    
    public Invoice[] getInvoices(InvoiceSearchCriteria searchCriteria) {
        ArrayList<Invoice> matchingInvoices = new ArrayList<Invoice>();
        for (Iterator<Invoice> iter = invoices.iterator(); iter.hasNext();) {
            Invoice invoice = iter.next();
            if (searchCriteria.matches(invoice)) {
                matchingInvoices.add(invoice);
            }
        }
        Invoice[] result = new Invoice[matchingInvoices.size()];
        matchingInvoices.toArray(result);
        return result;
    }
    
}
