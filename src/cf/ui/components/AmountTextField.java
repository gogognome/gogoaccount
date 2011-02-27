/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
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
