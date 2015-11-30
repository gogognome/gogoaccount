package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IbanValidatorTest {

    private final IbanValidator ibanValidator = new IbanValidator();

    @Before
    public void initTextResource() {
        Locale.setDefault(Locale.US);
        TextResource tr = new TextResource(Locale.US);
        tr.loadResourceBundle("stringresources");
        Factory.bindSingleton(TextResource.class, tr);
    }

    @Test
    public void testIbanValidator() throws ServiceException {
        assertThrowsException("IBAN 0123456789 is invalid. The length must be 18.", "0123456789");
        assertThrowsException("IBAN 123456789012345678 is invalid. It does not start with a country code.", "123456789012345678");
        assertThrowsException("IBAN X23456789012345678 is invalid. It does not start with a country code.", "X23456789012345678");
        assertThrowsException("IBAN 1X3456789012345678 is invalid. It does not start with a country code.", "1X3456789012345678");
        assertThrowsException("IBAN NLXX56789012345678 is invalid. The country code must be followed by two digits.", "NLXX56789012345678");
        assertThrowsException("IBAN NL2X56789012345678 is invalid. The country code must be followed by two digits.", "NL2X56789012345678");
        assertThrowsException("IBAN NLX256789012345678 is invalid. The country code must be followed by two digits.", "NLX256789012345678");
        assertThrowsException("IBAN 123456789012345678 is invalid. It does not start with a country code.", "123456789012345678");
        assertThrowsException("IBAN NL34R6789012345678 is invalid. The bank code must consist of four letters.", "NL34R6789012345678");
        assertThrowsException("IBAN NL34RA789012345678 is invalid. The bank code must consist of four letters.", "NL34RA789012345678");
        assertThrowsException("IBAN NL34RAB89012345678 is invalid. The bank code must consist of four letters.", "NL34RAB89012345678");
        assertThrowsException("IBAN NL345ABO9012345678 is invalid. The bank code must consist of four letters.", "NL345ABO9012345678");

        assertValid("NL12RABO0123456789");
        assertValid("nl12rabo0123456789");
    }

    private void assertThrowsException(String expectedException, String iban) {
        try {
            ibanValidator.validate(iban);
            fail("Expected exception not thrown");
        } catch (ServiceException e) {
            assertEquals(expectedException, e.getMessage());
        }
    }

    private void assertValid(String iban) throws ServiceException {
        ibanValidator.validate(iban);
        assertTrue("IBAN is valid", true);
    }
}
