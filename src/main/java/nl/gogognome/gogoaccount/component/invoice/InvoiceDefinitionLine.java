package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.lib.text.Amount;

public class InvoiceDefinitionLine {

    private final Amount amount;
    private final String description;
    private final Account account;

    public InvoiceDefinitionLine(Amount amount, String description, Account account) {
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
