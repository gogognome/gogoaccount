/*
 * $Id: Journal.java,v 1.11 2008-01-10 19:18:07 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;

/**
 * This class represents a journal. 
 *
 * @author Sander Kooijmans
 */
public class Journal implements Comparable {

    private String id;
    
    private String description;
    
    private JournalItem[] items;
    
    private Date date;

    /**
     * Creates a journal.
     * @param id the id of the jounal
     * @param description the description of the journal
     * @param date the date of the journal
     * @param items the items of the journal. The sums of the debet 
     *        and credit amounts must be equal!
     * @throws IllegalArgumentException if the sum of debet and credit amounts differ
     *          or if more than one item with a party has been specified.
     */
    public Journal(String id, String description, Date date, JournalItem[] items) {
        this.id = id;
        this.description = description;
        this.date = date;
        this.items = items;
        
        Currency currency = Database.getInstance().getCurrency();
        Amount totalDebet = Amount.getZero(currency);
        Amount totalCredit = totalDebet;
        for (int i=0; i<items.length; i++) {
            if (items[i].isDebet()) {
                totalDebet = totalDebet.add(items[i].getAmount());
            } else {
                totalCredit = totalCredit.add(items[i].getAmount());
            }
        }
        
        if (totalDebet.compareTo(totalCredit) != 0) {
            AmountFormat af = new AmountFormat(Locale.getDefault());
            throw new IllegalArgumentException(
                    "The sum of debet and credit amounts differ for journal " + id 
                    + "! debet: " + af.formatAmount(totalDebet) + 
                    "; credit: " + af.formatAmount(totalCredit));
        }
    }
    
    /**
     * Gets the date of this journal.
     * @return the date of this journal.
     */
    public Date getDate() 
    {
        return date;
    }
    
    /**
     * Gets the description of this journal
     * @return the description of this journal.
     */
    public String getDescription() 
    {
        return description;
    }
    
    /**
     * Gets the id of this journal.
     * @return the id of this journal.
     */
    public String getId() 
    {
        return id;
    }
    
    /**
     * Gets the items of this journal.
     * @return the items of this journal.
     */
    public JournalItem[] getItems() 
    {
        return items;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        Journal that = (Journal)o;
        return this.date.compareTo(that.date);
    }
    
    /**
     * Checks whether this instance is equal to another instance.
     * @param o the other instance
     * @return <code>true</code> if this instance is equal to <code>o</code>;
     *          <code>false</code> otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof Journal) {
            Journal that = (Journal) o;
            return this.id.equals(that.id) && this.date.equals(that.date) &&
            	this.description.equals(that.description) &&
            	Arrays.equals(this.items, that.items);
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return id.hashCode() + date.hashCode() + description.hashCode();
    }
    
}
