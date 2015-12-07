package nl.gogognome.gogoaccount.util;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import java.math.BigInteger;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

public class AmountInDatabase {

    public static Amount parse(String value) throws SQLException {
        if (value == null) {
            return null;
        }
        boolean oldStyle;
        Currency currency;
        if (value.startsWith("-/- ")) {
            oldStyle = !Character.isDigit(value.charAt(4));
            currency = oldStyle ? parseCurrency(value, 4) : null;
        } else if (value.startsWith("-")) {
            oldStyle = false;
            currency = null;
        } else {
            oldStyle = !Character.isDigit(value.charAt(0));
            currency = oldStyle ? parseCurrency(value, 0) : null;
        }
        if (oldStyle) {
            try {
                return new Amount(new AmountFormat(Locale.US, currency).parse(value, currency));
            } catch (ParseException e) {
                throw new SQLException("Syntax error in amount '" + value + "'");
            }
        } else {
            return new Amount(new BigInteger(value));
        }
    }

    private static Currency parseCurrency(String value, int startIndex) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (int i=startIndex; i<value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                break;
            }
            sb.append(value.charAt(i));
        }
        try {
            return Currency.getInstance(sb.toString());
        } catch (IllegalArgumentException e) {
            throw new SQLException("Illegal currency code '" + sb.toString() + "' found in amount " + value);
        }
    }

    public static String format(Amount amount) {
        return amount != null ? amount.toBigInteger().toString() : null;
    }
}
