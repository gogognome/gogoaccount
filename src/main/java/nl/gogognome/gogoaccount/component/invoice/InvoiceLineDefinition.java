package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.lib.text.Amount;

public class InvoiceLineDefinition {

    private Amount amount;
    private Account account;

    public InvoiceLineDefinition(Amount amount, Account account) {
        super();
        this.amount = amount;
        this.account = account;
    }

    public Amount getAmount() {
        return amount;
    }

    public Account getAccount() {
        return account;
    }

}
