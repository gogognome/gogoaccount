package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.invoice.amountformula.AmountFormula;
import nl.gogognome.gogoaccount.component.invoice.amountformula.ConstantAmount;
import nl.gogognome.lib.text.Amount;

public class InvoiceLineDefinition {

    private final AmountFormula amountFormula;
    private final String description;
    private final Account account;

    public InvoiceLineDefinition(Amount amount, String description, Account account) {
        this(new ConstantAmount(amount), description, account);
    }

    public InvoiceLineDefinition(AmountFormula amountFormula, String description, Account account) {
        this.amountFormula = amountFormula;
        this.description = description;
        this.account = account;
    }

    public AmountFormula getAmountFormula() {
        return amountFormula;
    }

    public String getDescription() {
        return description;
    }

    public Account getAccount() {
        return account;
    }
}
