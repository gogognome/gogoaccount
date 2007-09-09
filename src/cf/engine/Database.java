/*
 * $Id: Database.java,v 1.25 2007-09-09 19:39:40 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
    
    private Vector journals = new Vector();
    
    private Vector parties = new Vector();
    
    /** Maps ids of accounts to <code>Account</code>s. */
    private HashMap idsToAccountsMap = new HashMap();
    
    /** Maps ids of parties to <code>Party</code> instances. */
    private HashMap idsToPartiesMap = new HashMap();
    
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
	private ArrayList listeners = new ArrayList();
	
	/**
	 * Adds a database listener.
	 * @param l the database listener.
	 */
	public void addListener( DatabaseListener l ) 
	{
		listeners.add(l);
	}

	/**
	 * Removes a database listener.
	 * @param l the database listener.
	 */
	public void removeListener( DatabaseListener l ) 
	{
		listeners.remove(l);
	}
	
	/** Notifies the listeners. */
	private void notifyListeners() 
	{
		for (int i=0; i<listeners.size(); i++) 
		{
			DatabaseListener l = (DatabaseListener)listeners.get(i);
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
    public Account getAccount(String id)
    {
        return (Account)idsToAccountsMap.get(id);
    }
    
    public Account[] getAssets() 
    {
        return assets;
    }
    
    public void setAssets(Account[] assets) 
    {
        for (int i=0; i<assets.length; i++)
        {
            if (!assets[i].isDebet())
            {
                throw new IllegalArgumentException("Assets must have debet equal to true!");
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
    
    public void setExpenses(Account[] expenses) 
    {
        for (int i=0; i<expenses.length; i++)
        {
            if (!expenses[i].isDebet())
            {
                throw new IllegalArgumentException("Expenses must have debet equal to true!");
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
    
    public void setLiabilities(Account[] liabilities) 
    {
        for (int i=0; i<liabilities.length; i++)
        {
            if (liabilities[i].isDebet())
            {
                throw new IllegalArgumentException("Liabilities must have debet equal to false!");
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
    
    public void setRevenues(Account[] revenues) 
    {
        for (int i=0; i<revenues.length; i++)
        {
            if (revenues[i].isDebet())
            {
                throw new IllegalArgumentException("Revenues must have debet equal to false!");
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
    public void setJournals(Journal[] journals)
    {
        this.journals.removeAllElements();
        for (int i=0; i<journals.length; i++)
        {
            this.journals.addElement(journals[i]);
        }
        notifyChange();
    }
    
    public void addJournal(Journal journal)
    {
        journals.addElement(journal);
        notifyChange();
    }
    
    public Journal[] getJournals()
    {
        Journal[] result = new Journal[journals.size()];
        journals.copyInto(result);
        Arrays.sort(result);
        return result;
    }
    
    public Party[] getParties()
    {
        Party[] result = new Party[parties.size()];
        parties.copyInto(result);
        Arrays.sort(result);
        return result;
    }

    public Party[] getParties(PartySearchCriteria searchCriteria) {
        ArrayList matchingParties = new ArrayList();
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
        return (Party)idsToPartiesMap.get(id);
    }
    
    /**
     * Sets the debtors in the database. Any debtors present in the database
     * are replaced.
     * @param debtors the debtors.
     * @throws IllegalArgumentException if at least two debtors were present 
     *         with the same id.
     */
    public void setParties(Party[] parties)
    {
        Vector newParties = new Vector();
        HashMap newIdsToPartiesMap = new HashMap();
		for (int i = 0; i < parties.length; i++) {
		    String id = parties[i].getId();
		    newParties.addElement(parties[i]);
		    if (newIdsToPartiesMap.get(id) != null) {
	            throw new IllegalArgumentException("A party id "
	                    + id + " already exists!");
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
     * @throws IllegalArgumentException if another party exists with the same id. 
     */
    public void addParty(Party party) {
        String id = party.getId();
        if (idsToPartiesMap.get(id) != null) {
            throw new IllegalArgumentException("A party with ID " + id + " already exists!");
        }
        parties.addElement(party);
        idsToPartiesMap.put(id, party);
        notifyChange();
    }

    /**
     * Updates a party in the database.
     * @param oldParty the old party (which must exist in the database)
     * @param newParty the new party (which may not exist yet in the database)
     * @throws IllegalArgumentException if another party exists with the same id as <code>newParty</code> 
     *         and if <code>oldParty</code>'s ID is different from <code>newParty</code>'s ID or if
     *         <code>oldParty</code> does not exist in the database 
     */
    public void updateParty(Party oldParty, Party newParty) {
        if (idsToPartiesMap.get(oldParty.getId()) == null) {
            throw new IllegalArgumentException("A party with ID " + oldParty.getId() + " does not exist!");
        }
        if (!oldParty.getId().equals(newParty.getId())) {
            if (idsToPartiesMap.get(newParty.getId()) != null) {
                throw new IllegalArgumentException("A party with ID " + newParty.getId() + " already exists!");
            }
        }
        parties.set(parties.indexOf(oldParty), newParty);
        idsToPartiesMap.put(newParty.getId(), newParty);
        notifyChange();
    }
    
    /**
     * Removes a party from the database.
     * @param party the party to be removed
     */
    public void removeParty(Party party) {
        if (idsToPartiesMap.get(party.getId()) == null) {
            throw new IllegalArgumentException("A party with ID " + party.getId() + " does not exist!");
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
     * Gets the total of debet amounts of a party at the specified date.
     * @param party the party
     * @param date the date 
     * @return the total of debet amounts of the party
     */
    public Amount getTotalDebetForParty(Party party, Date date) {
        Amount result = Amount.getZero(getCurrency());
        for (Iterator iter = journals.iterator(); iter.hasNext();) {
            Journal journal = (Journal) iter.next();
            if (DateUtil.compareDayOfYear(journal.getDate(), date) <= 0) {
	            JournalItem[] items = journal.getItems();
	            for (int j = 0; j < items.length; j++) {
	                if (party.equals(items[j].getParty())) {
	                    if (items[j].isDebet()) {
	                        result = result.add(items[j].getAmount());
	                    }
	                }
	            }
            }
        }
        return result;
    }
    
    /**
     * Gets the total of credit amounts of a party at the specified date.
     * @param party the party
     * @param date the date 
     * @return the total of credit amounts of the party
     */
    public Amount getTotalCreditForParty(Party party, Date date) {
        Amount result = Amount.getZero(getCurrency());
        for (Iterator iter = journals.iterator(); iter.hasNext();) {
            Journal journal = (Journal) iter.next();
            if (DateUtil.compareDayOfYear(journal.getDate(), date) <=0) {
	            JournalItem[] items = journal.getItems();
	            for (int j = 0; j < items.length; j++) {
	                if (party.equals(items[j].getParty())) {
	                    if (items[j].isCredit()) {
	                        result = result.add(items[j].getAmount());
	                    }
	                }
	            }
            }
        }
        return result;
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
        return getTotalDebetForParty(party, date).subtract(getTotalCreditForParty(party, date));
    }
    
    /**
     * Gets the debtors at the specified date. Debtors are parties that still have 
     * to pay money to the club.
     * @param date the date
     * @return the debtors
     */
    public Party[] getDebtors(Date date) {
        Vector debtors = new Vector();
        for (int i=0; i<parties.size(); i++)
        {
            Party party = (Party)parties.elementAt(i);
            Amount totalDebet = getTotalDebetForParty(party, date);
            Amount totalCredit = getTotalCreditForParty(party, date);
            Amount balance = totalDebet.subtract(totalCredit);
            if (balance.isPositive())
            {
                debtors.addElement(party);
            }
        }
        Party[] result = new Party[debtors.size()];
        debtors.copyInto(result);
        return result;
    }
    
    /**
     * Gets the creditors at the specified date. Creditors are parties to which
     * the club owes money.
     * @param date the date
     * @return the creditors
     */
    public Party[] getCreditors(Date date) {
        Vector creditors = new Vector();
        for (int i=0; i<parties.size(); i++)
        {
            Party party = (Party)parties.elementAt(i);
            Amount totalDebet = getTotalDebetForParty(party, date);
            Amount totalCredit = getTotalCreditForParty(party, date);
            Amount balance = totalDebet.subtract(totalCredit);
            if (balance.isNegative())
            {
                creditors.addElement(party);
            }
        }
        Party[] result = new Party[creditors.size()];
        creditors.copyInto(result);
        return result;
    }
    
    /**
     * All journals before or at the specified date are replaced by
     * a single journal. This method will not change the balance at the
     * specified date or of later dates.
     * 
     * <p>Journals with a date before the specified date will only be remained
     * if they contain references to amounts that are still to be paid to creditors or
     * received from debtors.
     * 
     * <p>This method will remove journals. It should be used carefully. Typically,
     * it should not be used on a database that contains unsaved changes.
     * 
     * @param date the date
     */
    public void cleanUpJournalsBefore(Date date) {
        ArrayList journalsToBeDeleted = new ArrayList(Arrays.asList(getJournals()));
        
        // maps accounts to the amount of the corresponding accounts.
        HashMap accountsToAmountMap = new HashMap();
        
        // maps parties to the amount to be received from the corresponding party.
        HashMap partiesToAmountMap = new HashMap();
        
        // maps parties to a LinkedList of journals referring to the party
        HashMap partiesToJournalsMap = new HashMap();
        
        int index = 0;
        while (index < journalsToBeDeleted.size()) {
            Journal journal = (Journal)journalsToBeDeleted.get(index);
            if (DateUtil.compareDayOfYear(journal.getDate(), date) > 0) {
                // The journal has a date after the specified date and should
                // therefore not be replaced.
                journalsToBeDeleted.remove(index);
            } else {
                JournalItem[] items = journal.getItems();

                Party foundParty = null;
                Amount partyAmount = null;
                for (int i = 0; i < items.length; i++) {
                    JournalItem item = items[i];
                    
                    Amount amount = getAmountFromMap(accountsToAmountMap, item.getAccount());
                    if (item.isDebet()) {
                        amount = amount.add(item.getAmount());
                    } else {
                        amount = amount.subtract(item.getAmount());
                    }
                    accountsToAmountMap.put(item.getAccount(), amount);
                    
                    Party party = item.getParty();
                    if (party != null) {
                        if (foundParty != null) {
                            throw new IllegalStateException("Found a journal with more than 1 item with a party!");
                        }
                        
                        foundParty = party;
                        partyAmount = getAmountFromMap(partiesToAmountMap, party);
                        if (item.isDebet()) {
                            partyAmount = partyAmount.add(item.getAmount());
                        } else {
                            partyAmount = partyAmount.subtract(item.getAmount());
                        }
                        partiesToAmountMap.put(party, partyAmount);
                        
                        List list = (List)partiesToJournalsMap.get(party);
                        if (list == null) {
                            list = new LinkedList();
                            partiesToJournalsMap.put(party, list);
                        }
                        list.add(journal);
                    }
                }
                
                index++;

                if (foundParty != null && partyAmount != null && partyAmount.isZero()) {
                    // The party that was found has an amount of zero. The journals
                    // that refer to the party can be removed.
                    List list = (List)partiesToJournalsMap.get(foundParty);
                    list.clear();
                }
            }
        }

        // For each of the parties whose balance is not zero, make sure that the
        // journals referring to that party are not removed.
        for (Iterator iter = partiesToJournalsMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            Party party = (Party)entry.getKey();
            List list = (List)entry.getValue();
            for (Iterator iterator = list.iterator(); iterator.hasNext();) {
                Journal journal = (Journal) iterator.next();
                journalsToBeDeleted.remove(journal);
                
                // Undo the update of the amounts in accountsToAmountMap.
                JournalItem[] items = journal.getItems();
                for (int i = 0; i < items.length; i++) {
                    JournalItem item = items[i];
                    
                    Amount amount = getAmountFromMap(accountsToAmountMap, item.getAccount());
	                if (!item.isDebet()) {
	                    amount = amount.add(item.getAmount());
	                } else {
	                    amount = amount.subtract(item.getAmount());
	                }
	                accountsToAmountMap.put(item.getAccount(), amount);
	                
	                if (item.getParty() != null && !party.equals(item.getParty())) {
	                    throw new InternalError("Journal contains wrong party!");
	                }
                }
            }
        }
        
        
        // Remove the journals.
        for (Iterator iter = journalsToBeDeleted.iterator(); iter.hasNext();) {
            Journal journal = (Journal) iter.next();
            journals.remove(journal);
        }
        
        // Add a journal that matches the amounts of the removed journals.
        Set accountToAmountEntries = accountsToAmountMap.entrySet();
        JournalItem[] items = new JournalItem[accountToAmountEntries.size()];
        index = 0;
        for (Iterator iter = accountToAmountEntries.iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            Account account = (Account) entry.getKey();
            Amount amount = (Amount) entry.getValue();
            if (!amount.isNegative()) {
                items[index] = new JournalItem(amount, account, true, null);
            } else {
                items[index] = new JournalItem(amount.negate(), account, false, null);
            }
            index++;
        }
        
        journals.addElement(new Journal("reset", "reset", date, items));
        
        notifyChange();
    }
    
    /**
     * Gets an amount from a <code>Map</code>.
     * 
     * @param map the map
     * @param key the key for which the corresponding <code>Amount</code> is
     *         to be returned
     * @return the <code>Amount</code> that corresponds to the specified key or
     *         an amount representing zero if no <code>Amount</code> was associated
     *         with the key.
     */
    private Amount getAmountFromMap(Map map, Object key) {
        Amount result = (Amount)map.get(key);
        if (result == null) {
            result = Amount.getZero(getCurrency()); 
        }
        return result;
    }
}
