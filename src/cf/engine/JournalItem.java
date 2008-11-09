/*
 * $Id: JournalItem.java,v 1.11 2008-11-09 13:59:13 sanderk Exp $
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

    /**
     * The id of the invoice to which the amount is booked as payment;
     * <code>null</code> if no payment is involved.
     */
    private String invoiceId;
    
    /**
     * The id of the payment within the invoice that corresponds to this item;
     * <code>null</code> if no payment is involved.
     */
    private String paymentId;
    
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
     * @param invoiceId the id of the invoice to which the amount is booked as payment;
     *              <code>null</code> if no invoice is involved.
     * @param paymentId the id of the payment; <code>null</code> if no payment is involved
     */
    public JournalItem(Amount amount, Account account, boolean debet, String invoiceId, String paymentId)  {
        this.amount = amount;
        this.account = account;
        this.debet = debet;
        this.invoiceId = invoiceId;
        this.paymentId = paymentId;
    }
    
    public Account getAccount() {
        return account;
    }

    public Amount getAmount() {
        return amount;
    }

    public String getInvoiceId() {
        return invoiceId;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public boolean isDebet() {
        return debet;
    }
    
    public boolean isCredit() {
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
            boolean payments = ComparatorUtil.equals(this.invoiceId, that.invoiceId)
                && ComparatorUtil.equals(this.paymentId, that.paymentId); 
            return this.account.equals(that.account) && this.amount.equals(that.amount) 
            	&& this.debet == that.debet && payments;
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return amount.hashCode() + account.hashCode();
    }
}
