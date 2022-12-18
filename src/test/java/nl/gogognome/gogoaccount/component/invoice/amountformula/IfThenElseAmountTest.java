package nl.gogognome.gogoaccount.component.invoice.amountformula;

import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import nl.gogognome.lib.text.*;
import nl.gogognome.textsearch.criteria.*;
import nl.gogognome.textsearch.string.*;

public class IfThenElseAmountTest {

    private final Criterion criterion = mock(Criterion.class);
    private final CriterionMatcher criterionMatcher = mock(CriterionMatcher.class);
    private final AmountFormula thenFormula = mock(AmountFormula.class);
    private final AmountFormula elseFormula = mock(AmountFormula.class);
    private final IfThenElseAmount ifThenElse = new IfThenElseAmount(criterionMatcher, criterion, thenFormula, elseFormula);
    private final IfThenElseAmount ifThen = new IfThenElseAmount(criterionMatcher, criterion, thenFormula, null);

    private final Amount someAmount = new Amount("123");

    @Test
    public void getAmountReturningThenAmount() throws Exception {
        when(criterionMatcher.matches(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);
        when(thenFormula.getAmount(ArgumentMatchers.<String>anyList())).thenReturn(someAmount);

        List<String> tags = asList("tag1", "tag2");
        Amount amount = ifThenElse.getAmount(tags);

        assertEquals(someAmount, amount);
        verify(thenFormula).getAmount(tags);
        verify(elseFormula, never()).getAmount(tags);
    }

    @Test
    public void getAmountReturningElseAmount() throws Exception {
        when(criterionMatcher.matches(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(false);
        when(elseFormula.getAmount(ArgumentMatchers.<String>anyList())).thenReturn(someAmount);

        List<String> tags = asList("tag1", "tag2");
        Amount amount = ifThenElse.getAmount(tags);

        assertEquals(someAmount, amount);
        verify(thenFormula, never()).getAmount(tags);
        verify(elseFormula).getAmount(tags);
    }


    @Test
    public void getAmountWithoutElseReturningNull() throws Exception {
        when(criterionMatcher.matches(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(false);

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

        assertEquals(someAmount, if_c_Amount.getAmount(Collections.singletonList("c")));
        assertEquals(someAmount, if_c_Amount.getAmount(Collections.singletonList("C")));
        assertEquals(someAmount, if_c_Amount.getAmount(asList("a", "b", "c")));
        assertNull(if_c_Amount.getAmount(Collections.singletonList("abc")));
    }

    @Test
    public void testHashCodeAndEquals() {
        assertHashAndEqual(true, ifThenElse, ifThen, ifThenElse, ifThen);
        assertHashAndEqual(true, ifThenElse, null, ifThenElse, null);
        assertHashAndEqual(true, new ConstantAmount(someAmount), null, new ConstantAmount(someAmount), null);
        assertHashAndEqual(false, ifThenElse, null, ifThenElse, ifThen);
        assertHashAndEqual(false, ifThenElse, ifThen, ifThenElse, null);
        assertHashAndEqual(false, ifThenElse, ifThen, ifThenElse, new ConstantAmount(someAmount));

        assertNotEquals(ifThen, new Object());
        assertNotEquals(null, ifThen);
    }

    private void assertHashAndEqual(boolean expectedEqual, AmountFormula thenFormule1, AmountFormula elseFormula1, AmountFormula thenFormule2, AmountFormula elseFormula2) {
        IfThenElseAmount ifThenElseAmount1 = new IfThenElseAmount(criterionMatcher, criterion, thenFormule1, elseFormula1);
        IfThenElseAmount ifThenElseAmount2 = new IfThenElseAmount(criterionMatcher, criterion, thenFormule2, elseFormula2);
        assertEquals(expectedEqual, ifThenElseAmount1.equals(ifThenElseAmount2), "Formula " + ifThenElseAmount1 + " should " + (expectedEqual ? "" : "not ") + "  be equal to " + ifThenElseAmount2);
        assertEquals(expectedEqual, ifThenElseAmount1.hashCode() == ifThenElseAmount2.hashCode(), "Hashcode for formula " + ifThenElseAmount1 + " should " + (expectedEqual ? "" : "not ") + "  be equal to hashcode of " + ifThenElseAmount2);
    }
}