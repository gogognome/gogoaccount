/*
 * $Id: JournalItem.java,v 1.9 2008-01-11 18:56:56 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import nl.gogognome.text.Amount;
import nl.gogognome.util.ComparatorUtil;

/**
 * This class represents a single item of a journal. 
 *
 * @author Sander Kooijmans
 */
public class JournalItem 
{
    private Amount amount;
    
    private Account account;
    
    private String invoiceId;
    
    /** 
     * If <code>invoiceId != null</code>, then this field indicates whether
     * this item represents invoice creation (<code>true</code>) or a payment
     * (<code>false</code>).
     */
    private boolean invoiceCreation;
    
    /** 
     * Indicates whether the amount in this items is booked on the
     * debet side (true) or credit side (false).
     */
    private boolean debet;
    
    /**
     * Constructs a journal item.
     * @param amount the amount
     * @param account the account on which the amount is booked 
     * @param debet true if the amount is booked on the debet side; false if
     *              the amount is booked on the credit side 
     * @param invoiceId the id of the invoice to which the amount is booked;
     *              <code>null</code> if no invoice is involved.
     * @param invoiceCreation if <code>invoiceId != null</code>, then this parameter 
     *        indicates whether this item represents invoice creation (<code>true</code>)
     *        or a payment (<code>false</code>).
     */
    public JournalItem(Amount amount, Account account, boolean debet, String invoiceId, 
            boolean invoiceCreation)  {
        this.amount = amount;
        this.account = account;
        this.debet = debet;
        this.invoiceId = invoiceId;
        this.invoiceCreation = invoiceCreation;
    }
    
    public Account getAccount() 
    {
        return account;
    }

    public Amount getAmount() 
    {
        return amount;
    }

    public String getInvoiceId() {
        return invoiceId;
    }
    
    public boolean hasInvoiceCreation() {
        return invoiceCreation;
    }
    
    public boolean isDebet()
    {
        return debet;
    }
    
    public boolean isCredit()
    {
        return !debet;
    }
    
    /**
     * Checks whether this instance is equal to another instance.
     * @param o the other instance
     * @return <code>true</code> if this instance is equal to <code>o</code>;
     *          <code>false</code> otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof JournalItem) {
            JournalItem that = (JournalItem) o;
            boolean equalParties = ComparatorUtil.equals(this.invoiceId, that.invoiceId); 
            return this.account.equals(that.account) && this.amount.equals(that.amount) 
            	&& this.debet == that.debet && equalParties;
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return amount.hashCode() + account.hashCode();
    }
}
