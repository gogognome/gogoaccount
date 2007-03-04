/*
 * $Id: AmountTextField.java,v 1.2 2006-12-23 19:06:51 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import java.text.ParseException;
import java.util.Currency;

import javax.swing.JTextField;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;

/**
 * This class implements a text field for <code>Amount</code>s.
 *
 * @author Sander Kooijmans
 */
public class AmountTextField extends JTextField {

    private AmountFormat amountFormat = TextResource.getInstance().getAmountFormat();

    private Currency currency;
    
    public AmountTextField(Currency currency) {
        this.currency = currency;
        setHorizontalAlignment(JTextField.RIGHT);
    }
    
    /**
     * Sets the amount that is displayed in the text field.
     * @param amount the amount
     */
    public void setAmount(Amount amount) {
        setText(amountFormat.formatAmountWithoutCurrency(amount));
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
                result = amountFormat.parse(getText(), currency);
            }
        } catch (ParseException ignore) {
        }
        return result;
    }
}
