package nl.gogognome.gogoaccount.util;

import nl.gogognome.dataaccess.DataAccessException;
import nl.gogognome.lib.text.Amount;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.text.DateFormat;

import static org.junit.Assert.*;

public class AmountInDatabaseTest {

    @Test
    public void oldSyntax() throws SQLException {
        assertNull(AmountInDatabase.parse(null));
        assertEquals(new Amount("1234"), AmountInDatabase.parse("EUR 12.34"));
        assertEquals(new Amount("-1234"), AmountInDatabase.parse("-/- EUR 12.34"));

        try {
            AmountInDatabase.parse("afsaa 23");
            fail("Expected exception not thrown");
        } catch (SQLException e) {
            assertEquals("Illegal currency code 'afsaa' found in amount afsaa 23", e.getMessage());
        }
    }

    @Test
    public void newSyntax() throws SQLException {
        assertNull(AmountInDatabase.parse(null));
        assertEquals(new Amount("1234"), AmountInDatabase.parse("1234"));
        assertEquals(new Amount("-1234"), AmountInDatabase.parse("-1234"));
    }

    @Test
    public void formatAmount() {
        assertEquals(null, AmountInDatabase.format(null));
        assertEquals("1234", AmountInDatabase.format(new Amount("1234")));
        assertEquals("-1234", AmountInDatabase.format(new Amount("-1234")));
    }
}