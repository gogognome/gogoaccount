package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.lib.text.Amount;

public class InvoiceLineDefinition {

    private Amount amount;
    private String description;
    private Account account;

    public InvoiceLineDefinition(Amount amount, String description, Account account) {
        super();
        this.amount = amount;
        this.description = description;
        this.account = account;
    }

    public Amount getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Account getAccount() {
        return account;
    }

}
