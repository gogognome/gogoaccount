/*
 * $Id: Database.java,v 1.20 2007-02-10 16:28:46 sanderk Exp $
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
import java.util.Vector;

import nl.gogognome.text.Amount;

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
	private static Vector listeners = new Vector();
	
	/**
	 * Adds a database listener.
	 * @param l the database listener.
	 */
	public static void addListener( DatabaseListener l ) 
	{
		listeners.addElement(l);
	}

	/**
	 * Removes a database listener.
	 * @param l the database listener.
	 */
	public static void removeListener( DatabaseListener l ) 
	{
		listeners.removeElement(l);
	}
	
	/** Notifies the listeners. */
	private static void notifyListeners() 
	{
		for (int i=0; i<listeners.size(); i++) 
		{
			DatabaseListener l = (DatabaseListener)listeners.elementAt(i);
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
		for (int i = 0; i < parties.length; i++) 
		{
		    String id = parties[i].getId();
		    newParties.addElement(parties[i]);
		    if (newIdsToPartiesMap.get(id) != null)
		    {
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
    public void addParty(Party party)
    {
        String id = party.getId();
        if (idsToPartiesMap.get(id) != null)
        {
            throw new IllegalArgumentException("A party id "
                    + id + " already exists!");
        }
        parties.addElement(party);
        idsToPartiesMap.put(id, party);
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
    public Balance getBalance(Date date)
    {
        return new Balance(this, date);
    }
    
    /**
     * Gets the operation result for the specified date.
     * @param date the date
     * @return the operation result
     */
    public OperationalResult getOperationalResult(Date date)
    {
        return new OperationalResult(date);
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
            Amount totalDebet = party.getTotalDebet(date);
            Amount totalCredit = party.getTotalCredit(date);
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
            Amount totalDebet = party.getTotalDebet(date);
            Amount totalCredit = party.getTotalCredit(date);
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
    
}
