package nl.gogognome.gogoaccount.util;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import org.junit.jupiter.api.*;
import nl.gogognome.lib.text.*;

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
        assertNull(AmountInDatabase.format(null));
        assertEquals("1234", AmountInDatabase.format(new Amount("1234")));
        assertEquals("-1234", AmountInDatabase.format(new Amount("-1234")));
    }
}