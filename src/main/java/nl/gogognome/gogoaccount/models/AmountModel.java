package nl.gogognome.gogoaccount.models;

import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

public class AmountModel extends StringModel {

    private final AmountFormat amountFormat;

    public AmountModel(Currency currency) {
        super();
        amountFormat = new AmountFormat(Locale.getDefault(), currency);
    }

    public Amount getAmount() {
        return parseString(getString());
    }

    /**
     * Gets the amount that is typed in to this text field.
     * @return the amount or null if no valid amount was entered
     */
    public Amount parseString(String s) {
        Amount result = null;
        try {
            if (s != null && s.length() > 0) {
                result = new Amount(amountFormat.parse(s));
            }
        } catch (ParseException ignore) {
        }
        return result;
    }

    public boolean isValid() {
        return getValue() == null || parseString(getString()) != null;
    }

    /**
     * Sets the amount that is displayed in the text field.
     * @param amount the amount
     */
    public void setAmount(Amount amount) {
        setValue(amountFormat.formatAmountWithoutCurrency(amount.toBigInteger()));
    }

    /**
     * Sets the amount that is displayed in the text field.
     * @param amount the amount
     * @param source the model change listener that sets the double. It will not get notified. It may be null.
     */
    public void setAmount(Amount amount, ModelChangeListener source) {
        setValue(amountFormat.formatAmountWithoutCurrency(amount.toBigInteger()), source);
    }

}
