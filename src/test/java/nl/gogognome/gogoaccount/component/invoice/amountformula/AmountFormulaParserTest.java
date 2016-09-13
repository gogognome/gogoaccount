package nl.gogognome.gogoaccount.component.invoice.amountformula;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.textsearch.criteria.And;
import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.criteria.StringLiteral;
import org.junit.Test;

import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

import static org.junit.Assert.*;

public class AmountFormulaParserTest {

    private AmountFormat amountFormat = new AmountFormat(Locale.US, Currency.getInstance("EUR"));
    private AmountFormulaParser parser = new AmountFormulaParser(amountFormat);

    @Test
    public void testParser() throws ParseException {
        parseShouldFail("", "Text must not be empty");
        parseShouldFail("   ", "Text must not be empty");
        parseShouldFail("10 20", "Expected end of string but found : 20");

        parseShouldResultIn("10", constantAmount("10"));
        parseShouldResultIn("if (foo) 20", _if(new StringLiteral("foo"), constantAmount("20")));
        parseShouldResultIn("if (foo) 20 else 10", _if(new StringLiteral("foo"), constantAmount("20"), constantAmount("10")));
        parseShouldResultIn("if (foo and bar) 20", _if(new And(new StringLiteral("foo"), new StringLiteral("bar")), constantAmount("20")));
        parseShouldResultIn("if (((foo))) 20", _if(new StringLiteral("foo"), constantAmount("20")));
        parseShouldResultIn("if (foo) (if (bar) 20 else 30)", _if(new StringLiteral("foo"), _if(new StringLiteral("bar"), constantAmount("20"), constantAmount("30"))));
        parseShouldResultIn("if (foo) (if (bar) 20) else 30", _if(new StringLiteral("foo"), _if(new StringLiteral("bar"), constantAmount("20")), constantAmount("30")));

        parseShouldFail("if (foo)) 20", "Expected amount but found )"); // one closing bracket too many
        parseShouldFail("if ((foo) 20", "Expected amount but found end of string"); // one closing bracket too few
        parseShouldFail("if (foo) (if (bar) 20 else 30", "Expected ')'"); // one closing bracket too few
        parseShouldFail("foo", "Expected amount but found foo");
        parseShouldFail("if foo 20", "Expected '('");
    }

    private ConstantAmount constantAmount(String amount) throws ParseException {
        return new ConstantAmount(new Amount(amountFormat.parse(amount)));
    }

    private AmountFormula _if(Criterion criterion, AmountFormula thenFormula) {
        return new IfThenElseAmount(criterion, thenFormula, null);
    }

    private AmountFormula _if(Criterion criterion, AmountFormula thenFormula, AmountFormula elseFormula) {
        return new IfThenElseAmount(criterion, thenFormula, elseFormula);
    }

    private void parseShouldFail(String text, String expectedErrorMessage) {
        try {
            AmountFormula formula = parser.parse(text);
            fail("Expected exception not thrown!");
        } catch (ParseException e) {
            assertEquals(expectedErrorMessage, e.getMessage());
        }
    }

    private void parseShouldResultIn(String text, AmountFormula expectedFormula) {
        try {
            AmountFormula formula = parser.parse(text);
            assertEquals(expectedFormula, formula);
        } catch (ParseException e) {
            fail("Could not parse " + text + ": " + e.getMessage());
        }
    }
}