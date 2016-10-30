package nl.gogognome.gogoaccount.component.invoice.amountformula;

import nl.gogognome.lib.text.Amount;
import org.h2.schema.Constant;

import java.util.List;

public class ConstantAmount implements AmountFormula {
    private final Amount amount;

    public ConstantAmount(Amount amount) {
        this.amount = amount;
    }

    @Override
    public Amount getAmount(List<String> partyTags) {
        return amount;
    }

    @Override
    public String toString() {
        return amount.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConstantAmount) {
            ConstantAmount that = (ConstantAmount) obj;
            return this.amount.equals(that.amount);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }
}
