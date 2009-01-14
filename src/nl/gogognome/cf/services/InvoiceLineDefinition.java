/*
 * $Id: InvoiceLineDefinition.java,v 1.1 2009-01-14 21:32:15 sanderk Exp $
 */

package nl.gogognome.cf.services;

import cf.engine.Account;
import nl.gogognome.text.Amount;

/**
 * This class represents a line of an invoice.
 * 
 * @author Sander Kooijmans
 */
public class InvoiceLineDefinition {

    private boolean amountToBePaid;
    
    private Amount debet;
    
    private Amount credit;
    
    private Account account;

    public InvoiceLineDefinition(Amount debet, Amount credit, Account account,
        boolean amountToBePaid) {
        super();
        this.debet = debet;
        this.credit = credit;
        this.account = account;
        this.amountToBePaid = amountToBePaid;
    }

    public boolean isAmountToBePaid() {
        return amountToBePaid;
    }

    public Amount getDebet() {
        return debet;
    }

    public Amount getCredit() {
        return credit;
    }

    public Account getAccount() {
        return account;
    }

}
