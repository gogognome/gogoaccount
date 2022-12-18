package nl.gogognome.gogoaccount.component.invoice.amountformula;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import nl.gogognome.lib.text.*;

public class ConstantAmountTest {

    private final Amount someAmount = new Amount("123");
    private final ConstantAmount constantAmount = new ConstantAmount(someAmount);

    @Test
    public void getAmountShouldIgnorePartyTags() throws Exception {
        assertEquals(someAmount, constantAmount.getAmount(null));
        assertEquals(someAmount, constantAmount.getAmount(emptyList()));
        assertEquals(someAmount, constantAmount.getAmount(singletonList("A")));
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
        assertNotEquals(null, constantAmount);
        assertNotEquals(constantAmount, new Object());
        assertNotEquals(constantAmount, new ConstantAmount(new Amount("6549")));
    }

    @Test
    public void testHashCode() throws Exception {
        assertEquals(constantAmount.hashCode(), constantAmount.hashCode());
        assertEquals(constantAmount.hashCode(), new ConstantAmount(new Amount(someAmount.toString())).hashCode());
        assertNotEquals(constantAmount.hashCode(), new ConstantAmount(new Amount("6549")).hashCode());
    }
}