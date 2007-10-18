/*
 * $Id: Invoice.java,v 1.1 2007-10-18 20:15:42 sanderk Exp $
 *
 * Copyright (C) 2005 Sander Kooijmans
 *
 */

package cf.engine;

import java.util.Date;

import nl.gogognome.text.Amount;

/**
 * This class represents an invoice. 
 */
public class Invoice {

    /** The identifier of the invoice. */
    private String id;
    
    /** The party that has to pay the invoice. */
    private Party payingParty;
    
    /** 
     * The party to whom this invoice is concerned. If this party is a minor, then typically
     * the paying party will be the parent of this party. 
     */
    private Party concerningParty;

    /** The amount to be paid. */
    private Amount amountToBePaid;

    /** The date this invoice was issued. */
    private Date issueDate;
    
    /** 
     * Contains details about this invoice. None of the elements may be <code>null</code>.
     * Descriptions may be associated to amounts. 
     */
    private String[] descriptions;
    
    /** 
     * Contains amounts that belongs to the descriptions. 
     * If an element is <code>null</code>, then the description has no corresponding amount.
     * 
     * <p>These amounts can be used to explain the total amount to be paid.
     *  
     * <p>Invariant: <code>amounts.length == descriptions.length</code> 
     */
    private Amount[] amounts;

    /** Contains payments of this invoice. */
    private Payment[] payments;
    
    /** This class specifies a payment. */
    public static class Payment {
        public Amount amount;
        public Date date;
        public String description;
    }
    
    /**
     * Consturctor.
     * @param id
     * @param payingParty
     * @param concerningParty
     * @param amountToBePaid
     * @param issueDate
     * @param descriptions
     * @param amounts
     */
    public Invoice(String id, Party payingParty, Party concerningParty, Amount amountToBePaid,
            Date issueDate, String[] descriptions, Amount[] amounts) {
        this(id, payingParty, concerningParty, amountToBePaid, issueDate, descriptions, amounts, 
            new Payment[0]);
    }

    /**
     * Consturctor.
     * @param id
     * @param payingParty
     * @param concerningParty
     * @param amountToBePaid
     * @param issueDate
     * @param descriptions
     * @param amounts
     */
    public Invoice(String id, Party payingParty, Party concerningParty, Amount amountToBePaid,
            Date issueDate, String[] descriptions, Amount[] amounts, Payment[] payments) {
        this.id = id;
        this.payingParty = payingParty;
        this.concerningParty = concerningParty;
        this.amountToBePaid = amountToBePaid;
        this.issueDate = issueDate;
        this.descriptions = descriptions;
        this.amounts = amounts;
        this.payments = payments;
    }

    /**
     * Gets the amount that has to be paid for this invoice minus the payments that have been made.
     * @return the remainig amount that has to be paid
     */
    public Amount getRemainingAmountToBePaid() {
        Amount result = amountToBePaid;
        for (int i=0; i<payments.length; i++) {
            result = result.subtract(payments[i].amount);
        }
        return result;
    }

    /**
     * Checks whether this invoice has been paid.
     * @return <code>true</code> if the remaining amount to be paid is zero (no more and no less);
     *         <code>false</code> otherwise
     */
    public boolean hasBeenPaid() {
        return getRemainingAmountToBePaid().isZero();
    }
    
    public Amount getAmountToBePaid() {
        return amountToBePaid;
    }
    
    public Amount[] getAmounts() {
        return amounts;
    }

    public Party getConcerningParty() {
        return concerningParty;
    }

    public String[] getDescriptions() {
        return descriptions;
    }

    public String getId() {
        return id;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public Party getPayingParty() {
        return payingParty;
    }

    public Payment[] getPayments() {
        return payments;
    }
    
    public Invoice addPayment(Payment payment) {
        Payment[] newPayments = new Payment[payments.length + 1];
        System.arraycopy(payments, 0, newPayments, 0, payments.length);
        newPayments[payments.length] = payment;
        return new Invoice(id, payingParty, concerningParty, amountToBePaid, issueDate, 
            descriptions, amounts, newPayments); 
    }
}
