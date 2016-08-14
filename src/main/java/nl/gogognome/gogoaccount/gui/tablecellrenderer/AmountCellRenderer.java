package nl.gogognome.gogoaccount.gui.tablecellrenderer;

import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

public class AmountCellRenderer extends RightAlignedRenderer {

    private final AmountFormat amountFormat;

    public AmountCellRenderer(AmountFormat amountFormat) {
        this.amountFormat = amountFormat;
    }

    public void setValue(Object value) {
        if (value instanceof Amount) {
            Amount amount = (Amount) value;
            super.setValue(amountFormat.formatAmountWithoutCurrency(amount.toBigInteger()));
        } else {
            super.setValue(value);
        }
    }

}
