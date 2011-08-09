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
package cf.ui.views;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.Currency;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;

/**
 * This view allows the user to edit a description and amount. It is typically used
 * for creating or updating invoices.
 */
public class EditDescriptionAndAmountView extends OkCancelView {

	private static final long serialVersionUID = 1L;

    private String titleId;

    private String initialDescription;
    private Amount initialAmount;
    private Currency currency;

    private JTextField tfDescription;
    private JTextField tfAmount;

    private String editedDescription;
    private Amount editedAmount;

    public EditDescriptionAndAmountView(String titleId, Currency currency) {
        this(titleId, null, null, currency);
    }

    public EditDescriptionAndAmountView(String titleId, String initialDescription, Amount initialAmount,
            Currency currency) {
        this.titleId = titleId;
        this.initialDescription = initialDescription;
        this.initialAmount = initialAmount;
        this.currency = currency;
    }

    @Override
    public String getTitle() {
        return textResource.getString(titleId);
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onInit() {
    	addComponents();
    }

    @Override
	protected JComponent createCenterComponent() {
        JPanel panel = new JPanel(new GridBagLayout());

        int row = 0;
        tfDescription = new JTextField(initialDescription, 30);
        JLabel label = widgetFactory.createLabel("editDescriptionAndAmountView.description", tfDescription);
        panel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        panel.add(tfDescription, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        String amount;
        if (initialAmount != null) {
            amount = Factory.getInstance(AmountFormat.class).formatAmountWithoutCurrency(initialAmount);
        } else {
            amount = "";
        }
        tfAmount = new JTextField(amount);
        label = widgetFactory.createLabel("editDescriptionAndAmountView.amount", tfAmount);
        panel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        panel.add(tfAmount, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        JButton button = widgetFactory.createButton("gen.ok", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent event) {
                onOk();
            }
        });
        buttonPanel.add(button);
        button = widgetFactory.createButton("gen.cancel", closeAction);
        buttonPanel.add(button);

        return panel;
    }

    @Override
	protected void onOk() {
        try {
            editedAmount = Factory.getInstance(AmountFormat.class).parse(tfAmount.getText(), currency);
        } catch (ParseException e) {
            MessageDialog.showWarningMessage(this, "gen.invalidAmount");
            return;
        }
        editedDescription = tfDescription.getText();
        closeAction.actionPerformed(null);
    }

    /**
     * Gets the description as entered by the user.
     * @return the description as entered by the user or <code>null</code> if the user cancelled the view.
     */
    public String getEditedDescription() {
        return editedDescription;
    }

    /**
     * Gets the amount as entered by the user.
     * @return the amount as entered by the user; <code>null</code> if the user cancelled the view or
     *         if the user did not enter an amount.
     */
    public Amount getEditedAmount() {
        return editedAmount;
    }
}
