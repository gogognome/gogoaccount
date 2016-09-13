package nl.gogognome.gogoaccount.component.invoice.amountformula;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.string.CriterionMatcher;
import nl.gogognome.textsearch.string.StringSearchFactory;

import java.util.List;

public class IfThenElseAmount implements AmountFormula {

    private final CriterionMatcher matcher = new StringSearchFactory().caseInsensitiveCriterionMatcher();
    private final Criterion criterion;
    private final AmountFormula thenAmountFormula;
    private final AmountFormula elseAmountFormula;

    public IfThenElseAmount(Criterion criterion, AmountFormula thenAmountFormula, AmountFormula elseAmountFormula) {
        this.criterion = criterion;
        this.thenAmountFormula = thenAmountFormula;
        this.elseAmountFormula = elseAmountFormula;
    }

    @Override
    public Amount getAmount(List<String> partyTags) {
        if (matcher.matches(criterion, partyTags.toArray(new String[partyTags.size()]))) {
            return thenAmountFormula.getAmount(partyTags);
        } else {
            if (elseAmountFormula != null) {
                return elseAmountFormula.getAmount(partyTags);
            } else {
                return null;
            }
        }
    }

    @Override
    public String toString() {
        return "if (" + criterion.toString() + ")" + thenAmountFormula +
                ((elseAmountFormula != null) ? " else " + elseAmountFormula : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IfThenElseAmount) {
            IfThenElseAmount that = (IfThenElseAmount) obj;
            return this.thenAmountFormula.equals(that.thenAmountFormula) &&
                    ((this.elseAmountFormula == null && that.elseAmountFormula == null)
                    || (this.elseAmountFormula != null && this.elseAmountFormula.equals(that.elseAmountFormula)));
        }
        return false;
    }
}
