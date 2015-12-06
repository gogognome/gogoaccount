package nl.gogognome.gogoaccount.gui.components;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import javax.swing.*;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

/**
 * This class implements a text field for <code>Amount</code>s.
 */
public class AmountTextField extends JTextField {

    private final AmountFormat amountFormat;

    public AmountTextField(Currency currency) {
        amountFormat = new AmountFormat(Locale.getDefault(), currency);
        setHorizontalAlignment(JTextField.RIGHT);
    }

    /**
     * Sets the amount that is displayed in the text field.
     * @param amount the amount
     */
    public void setAmount(Amount amount) {
        setText(amountFormat.formatAmountWithoutCurrency(amount.toBigInteger()));
    }

    /**
     * Gets the amount that is typed in to this text field.
     * @return the amount or <code>null</code> if no valid amount was entered
     */
    public Amount getAmount() {
        Amount result = null;
        try {
            String text = getText();
            if (text.length() > 0) {
                result = new Amount(amountFormat.parse(getText()));
            }
        } catch (ParseException ignore) {
        }
        return result;
    }
}
