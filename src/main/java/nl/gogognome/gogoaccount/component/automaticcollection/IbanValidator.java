package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

public class IbanValidator {

    public void validate(String iban) throws ServiceException {
        TextResource textResource = (TextResource) Factory.getInstance(TextResource.class);
        if (iban.length() != 18) {
            throw new ServiceException(textResource.getString("iban.lengthMustBe18", iban));
        }
        if (!Character.isLetter(iban.charAt(0)) || !Character.isLetter(iban.charAt(1))) {
            throw new ServiceException(textResource.getString("iban.mustStartWithCountryCode", iban));
        }
        if (!Character.isDigit(iban.charAt(2)) || !Character.isDigit(iban.charAt(3))) {
            throw new ServiceException(textResource.getString("iban.countryCodeMustBeFollowedByDigits", iban));
        }
        for (int i=4; i<8; i++) {
            if (!Character.isLetter(iban.charAt(i))) {
                throw new ServiceException(textResource.getString("iban.bankCodeMustConsistOfFourLetters", iban));
            }
        }
        for (int i=8; i<18; i++) {
            if (!Character.isDigit(iban.charAt(i))) {
                throw new ServiceException(textResource.getString("iban.accountIdMustConsistOfTenDigits", iban));
            }
        }
    }
}
