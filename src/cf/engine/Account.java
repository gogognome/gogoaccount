/*
 * $Id: Account.java,v 1.11 2007-03-04 20:43:54 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.Calendar;
import java.util.Date;

import nl.gogognome.text.Amount;
import nl.gogognome.util.DateUtil;

/**
 * This class represents an account. 
 *
 * @author Sander Kooijmans
 */
public class Account implements Comparable
{

    /** The id of this account. The id's of all accounts must be unique. */
    private String id;
    
    /** The name of this account. */
    private String name;
    
    /** 
     * Indicates whether this account is on the debet side of a balance or
     * of the results.
     * <code>true</code> indicates this account is an asset or a expense;
     * <code>false</code> indicates this account is a liability or a revenue.
     */
    private boolean debet;
    
    /** The database to which this account belongs. */
    private Database database;
    
    /**
     * Constructs an acount.
     * @param id the id of this account
     * @param name the name of this account
     * @param debet Indicates whether this account is on the debet side of a balance or
     *              of the results.
     * <code>true</code> indicates this account is an asset or a expense;
     * <code>false</code> indicates this account is a liability or a revenue.
     * @param database the database to which this account belongs. It is used
     *         to calculate the balance of this account at a specific date.
     */
    public Account(String id, String name, boolean debet, Database database) {
        this.id = id;
        this.name = name;
        this.debet = debet;
        this.database = database;
    }
    
    /**
     * Gets the id of this account.
     * @return the id of this account
     */
    public String getId()
    {
        return id;
    }
    
    /**
     * Gets the name of this account.
     * @return the name of this account
     */
    public String getName()
    {
        return name;
    }
    
    /** 
     * Checks whether this account is on the debet side of a balance or
     * of the results.
     * <code>true</code> indicates this account is an asset or a expense;
     * <code>false</code> indicates this account is a liability or a revenue.
     */
    public boolean isDebet()
    {
        return debet;
    }
    
    /**
     * Gets the balance of this account at the specified date.
     * @param date the date
     * @return the balance of this account at the specified date
     */
    public Amount getBalance(Date date)
    {
        Journal[] journals = database.getJournals();
        Amount result = Amount.getZero(database.getCurrency());
        for (int i = 0; i < journals.length; i++) 
        {
            if (DateUtil.compareDayOfYear(journals[i].getDate(), date) <= 0)
            {
	            JournalItem[] items = journals[i].getItems();
	            for (int j = 0; j < items.length; j++) 
	            {
	                if (items[j].getAccount().equals(this))
	                {
	                    if (debet == items[j].isDebet())
	                    {
	                        result = result.add(items[j].getAmount());
	                    }
	                    else
	                    {
	                        result = result.subtract(items[j].getAmount());
	                    }
	                }
	            }
            }
        }
        return result;
    }

    /**
     * Gets the balance of this account at start of the bookkeeping.
     * @return the balance of this account at start of the bookkeeping
     */
    public Amount getStartBalance() {
        Date date = database.getStartOfPeriod();
        
        // Subtract one day of the period start date, because otherwise the changes
        // made on that day will be taken into account too.
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        date = cal.getTime();
        
        return getBalance(date);
    }
    
    /**
     * Gets the hash code for this instance.
     * @return the hash code
     */
    public int hashCode()
    {
        return id.hashCode();
    }
    
    public boolean equals(Object o)
    {
        if (o instanceof Account)
        {
            return this.id == ((Account)o).getId();
        }
        else
        {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        Account that = (Account)o;
        return this.id.compareTo(that.id);
    }
    
}
