package nl.gogognome.gogoaccount.component.invoice.amountformula;

import nl.gogognome.lib.text.Amount;
import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class ConstantAmountTest {

    private Amount someAmount = new Amount("123");
    private ConstantAmount constantAmount = new ConstantAmount(someAmount);

    @Test
    public void getAmountShouldIgnorePartyTags() throws Exception {
        assertEquals(someAmount, constantAmount.getAmount(null));
        assertEquals(someAmount, constantAmount.getAmount(emptyList()));
        assertEquals(someAmount, constantAmount.getAmount(asList("A")));
        assertEquals(someAmount, constantAmount.getAmount(asList("A", "B", "C")));
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("123", constantAmount.toString());
    }

    @Test
    public void testEquals() throws Exception {
        assertEquals(constantAmount, constantAmount);
        assertEquals(constantAmount, new ConstantAmount(new Amount(someAmount.toString())));
        assertFalse(constantAmount.equals(null));
        assertFalse(constantAmount.equals(new Object()));
        assertFalse(constantAmount.equals(new ConstantAmount(new Amount("6549"))));
    }

    @Test
    public void testHashCode() throws Exception {
        assertEquals(constantAmount.hashCode(), constantAmount.hashCode());
        assertEquals(constantAmount.hashCode(), new ConstantAmount(new Amount(someAmount.toString())).hashCode());
        assertFalse(constantAmount.hashCode() == new ConstantAmount(new Amount("6549")).hashCode());
    }
}