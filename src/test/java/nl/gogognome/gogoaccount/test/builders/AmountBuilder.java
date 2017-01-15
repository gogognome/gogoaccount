package nl.gogognome.gogoaccount.test.builders;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

public class AmountBuilder {

    private final static AmountFormat amountFormat = new AmountFormat(Locale.US, Currency.getInstance("EUR"));

    public static Amount build(int value) {
        try {
            return new Amount(amountFormat.parse(Integer.toString(value)));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
