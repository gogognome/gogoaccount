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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import nl.gogognome.gogoaccount.gui.beans.InvoiceBean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.JournalItem;
import cf.ui.components.AccountFormatter;

/**
 * This view allows the user to edit a journal item.
 *
 * @author Sander Kooijmans
 */
public class EditJournalItemView extends View {

	private static final long serialVersionUID = 1L;

	private Database database;
    private InvoiceBean invoiceBean;
    private JournalItem itemToBeEdited;

    private ListModel<Account> accountListModel = new ListModel<Account>();
    private StringModel amountModel = new StringModel();
    private ListModel<String> sideListModel = new ListModel<String>();

    private JournalItem enteredJournalItem;

    private AmountFormat amountFormat = Factory.getInstance(AmountFormat.class);

    /**
     * Constructor.
     * @param database the database
     * @param item the item used to fill in the initial values of the fields.
     */
    public EditJournalItemView(Database database, JournalItem item) {
    	this.database = database;
    	this.itemToBeEdited = item;
    }

	@Override
	public String getTitle() {
		String id = itemToBeEdited != null ? "EditJournalItemView.titleAdd"
				: "EditJournalItemView.titleEdit";
		return textResource.getString(id);
	}

	@Override
	public void onInit() {
		initModels();
		addComponents();
	}

	private void initModels() {
		accountListModel.setItems(Arrays.asList(database.getAllAccounts()));

		List<String> sides = Arrays.asList(textResource.getString("gen.debet"),
				textResource.getString("gen.credit"));
		sideListModel.setItems(sides);
		invoiceBean = new InvoiceBean(database);

		if (itemToBeEdited != null) {
			initModelsForItemToBeEdited();
		}
	}

	private void initModelsForItemToBeEdited() {
		accountListModel.setSelectedItem(itemToBeEdited.getAccount(), null);
		amountModel.setString(amountFormat.formatAmountWithoutCurrency(itemToBeEdited.getAmount()));
		sideListModel.setSelectedIndex(itemToBeEdited.isDebet() ? 0 : 1, null);

		Invoice invoice = database.getInvoice(itemToBeEdited.getInvoiceId());
		invoiceBean.setSelectedInvoice(invoice);
	}

	private void addComponents() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel buttonPanel = createButtonPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		add(createInputFieldsPanel(), BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createInputFieldsPanel() {
		InputFieldsColumn column = new InputFieldsColumn();
		addCloseable(column);

		column.addComboBoxField("EditJournalItemView.account", accountListModel,
				new AccountFormatter());
		column.addField("EditJournalItemView.amount", amountModel);
		column.addComboBoxField("EditJournalItemView.side", sideListModel, null);
		column.addVariableSizeField("EditJournalItemView.invoice", invoiceBean);

		return column;
	}

	private JPanel createButtonPanel() {
		ButtonPanel panel = new ButtonPanel(SwingConstants.CENTER);

		JButton okButton = panel.addButton("gen.ok", new OkAction());
		panel.addButton("gen.cancel", new CancelAction());

		setDefaultButton(okButton);

		return panel;
	}

    /**
     * Gets the journal item has has been entered.
     * Its value will be set when the user presses the ok button and the input fields
     * are correct. Otherwise, this variable will be null.
     * @return the entered journal item or null
     */
    public JournalItem getEnteredJournalItem() {
        return enteredJournalItem;
    }

	private void onOk() {
        Amount amount;
        try {
            amount = amountFormat.parse(amountModel.getString(), database.getCurrency());
        } catch (ParseException e) {
        	amount = null;
        }

        Account account = accountListModel.getSingleSelectedItem();
        boolean debet = sideListModel.getSingleSelectedIndex() == 0;
        Invoice invoice = invoiceBean.getSelectedInvoice();

        if (!validateInput(amount, account)) {
        	return;
        }

        enteredJournalItem = new JournalItem(amount, account, debet,
            invoice != null ? invoice.getId() : null,
            invoice != null ? database.createPaymentId() : null);
        requestClose();
	}

	private boolean validateInput(Amount amount, Account account) {
		if (amount == null) {
        	MessageDialog.showWarningMessage(this, "gen.invalidAmount");
        	return false;
		}

		if (account == null) {
        	MessageDialog.showWarningMessage(this, "EditJournalItemView.noAccountSelected");
		}

		return true;
	}

	@Override
	public void onClose() {
	}

	private class OkAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent actionevent) {
			onOk();
		}
	}

	private class CancelAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent actionevent) {
			requestClose();
		}
	}

}
