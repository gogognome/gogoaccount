package nl.gogognome.gogoaccount.component.invoice.amountformula;

import nl.gogognome.lib.text.Amount;

import java.util.List;

public interface AmountFormula {

    Amount getAmount(List<String> partyTags);
}
