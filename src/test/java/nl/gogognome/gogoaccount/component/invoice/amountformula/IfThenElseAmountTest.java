package nl.gogognome.gogoaccount.component.invoice.amountformula;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.mockito.VarArgsMatcher;
import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.criteria.StringLiteral;
import nl.gogognome.textsearch.string.CriterionMatcher;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class IfThenElseAmountTest {

    private Criterion criterion = mock(Criterion.class);
    private CriterionMatcher criterionMatcher = mock(CriterionMatcher.class);
    private AmountFormula thenFormula = mock(AmountFormula.class);
    private AmountFormula elseFormula = mock(AmountFormula.class);
    private IfThenElseAmount ifThenElse = new IfThenElseAmount(criterionMatcher, criterion, thenFormula, elseFormula);
    private IfThenElseAmount ifThen = new IfThenElseAmount(criterionMatcher, criterion, thenFormula, null);

    private Amount someAmount = new Amount("123");

    @Test
    public void getAmountReturningThenAmount() throws Exception {
        when(criterionMatcher.matches(any(), VarArgsMatcher.varArg(l -> true))).thenReturn(true);
        when(thenFormula.getAmount(anyListOf(String.class))).thenReturn(someAmount);

        List<String> tags = asList("tag1", "tag2");
        Amount amount = ifThenElse.getAmount(tags);

        assertEquals(someAmount, amount);
        verify(thenFormula).getAmount(tags);
        verify(elseFormula, never()).getAmount(tags);
    }

    @Test
    public void getAmountReturningElseAmount() throws Exception {
        when(criterionMatcher.matches(any(), VarArgsMatcher.varArg(l -> true))).thenReturn(false);
        when(elseFormula.getAmount(anyListOf(String.class))).thenReturn(someAmount);

        List<String> tags = asList("tag1", "tag2");
        Amount amount = ifThenElse.getAmount(tags);

        assertEquals(someAmount, amount);
        verify(thenFormula, never()).getAmount(tags);
        verify(elseFormula).getAmount(tags);
    }


    @Test
    public void getAmountWithoutElseReturningNull() throws Exception {
        when(criterionMatcher.matches(any(), VarArgsMatcher.varArg(l -> true))).thenReturn(false);

        List<String> tags = asList("tag1", "tag2");
        Amount amount = ifThen.getAmount(tags);

        assertNull(amount);
        verify(thenFormula, never()).getAmount(tags);
        verify(elseFormula, never()).getAmount(tags);
    }

    @Test
    public void toStringWithElse() throws Exception {
        when (criterion.toString()).thenReturn("<criterion>");
        when(thenFormula.toString()).thenReturn("<then>");
        when(elseFormula.toString()).thenReturn("<else>");

        String string = ifThenElse.toString();

        assertEquals("if (<criterion>)<then> else <else>", string);
    }

    @Test
    public void toStringWithoutElse() throws Exception {
        when (criterion.toString()).thenReturn("<criterion>");
        when(thenFormula.toString()).thenReturn("<then>");

        String string = ifThen.toString();

        assertEquals("if (<criterion>)<then>", string);
    }

    @Test
    public void getAmountMatchesWholeTagsOnly() {
        IfThenElseAmount if_c_Amount = new IfThenElseAmount(new StringLiteral("c"), new ConstantAmount(someAmount), null);

        assertEquals(someAmount, if_c_Amount.getAmount(asList("c")));
        assertEquals(someAmount, if_c_Amount.getAmount(asList("C")));
        assertEquals(someAmount, if_c_Amount.getAmount(asList("a", "b", "c")));
        assertNull(if_c_Amount.getAmount(asList("abc")));
    }

    @Test
    public void testHashCodeAndEquals() {
        assertHashAndEqual(true, ifThenElse, ifThen, ifThenElse, ifThen);
        assertHashAndEqual(true, ifThenElse, null, ifThenElse, null);
        assertHashAndEqual(true, new ConstantAmount(someAmount), null, new ConstantAmount(someAmount), null);
        assertHashAndEqual(false, ifThenElse, null, ifThenElse, ifThen);
        assertHashAndEqual(false, ifThenElse, ifThen, ifThenElse, null);
        assertHashAndEqual(false, ifThenElse, ifThen, ifThenElse, new ConstantAmount(someAmount));

        assertFalse(ifThen.equals(new Object()));
        assertFalse(ifThen.equals(null));
    }

    private void assertHashAndEqual(boolean expectedEqual, AmountFormula thenFormule1, AmountFormula elseFormula1, AmountFormula thenFormule2, AmountFormula elseFormula2) {
        IfThenElseAmount ifThenElseAmount1 = new IfThenElseAmount(criterionMatcher, criterion, thenFormule1, elseFormula1);
        IfThenElseAmount ifThenElseAmount2 = new IfThenElseAmount(criterionMatcher, criterion, thenFormule2, elseFormula2);
        assertEquals("Formula " + ifThenElseAmount1 + " should " + (expectedEqual ? "" : "not ") + "  be equal to " + ifThenElseAmount2,
                expectedEqual, ifThenElseAmount1.equals(ifThenElseAmount2));
        assertEquals("Hashcode for formula " + ifThenElseAmount1 + " should " + (expectedEqual ? "" : "not ") + "  be equal to hashcode of " + ifThenElseAmount2,
                expectedEqual, ifThenElseAmount1.hashCode() == ifThenElseAmount2.hashCode());
    }
}