package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.lib.text.Amount;

/**
 * This class represents a line of an invoice.
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
