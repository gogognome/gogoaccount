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
*/package cf.ui.dialogs;


import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.gogoaccount.gui.beans.InvoiceBean;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.OkCancelDialog;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.JournalItem;
import cf.ui.components.AccountSelectionBean;

/**
 * This class implements the Edit Journal Item dialog.
 *
 * @author Sander Kooijmans
 */
public class EditJournalItemDialog extends OkCancelDialog {
    private AccountSelectionBean sbAccount;

    private JTextField tfAmount;

    private JComboBox cbSide;

    private InvoiceBean invoiceSelector;

    /** The parent frame of this dialog. */
    private Frame parentFrame;

    /** The parent view of this dialog. */
    private View parentView;

    private Database database;

    /**
     * Contains the journal item thas has been entered.
     * Its value will be set when the user presses the Ok button and the input fields
     * are correct. Otherwise, this variable will be <code>null</code>.Object
     */
    private JournalItem enteredJournalItem;

    /**
     * Constructor.
     * @param parent the parent frame of this dialog.
     * @param database the database
     * @param titleId the id of this dialog's title.
     */
    public EditJournalItemDialog(Frame parent, Database database, String titleId) {
        super(parent, titleId);
        this.database = database;
        this.parentFrame = parent;
        initDialog("", null, true, null);
    }

    /**
     * Constructor.
     * @param parent the parent dialog of this dialog.
     * @param database the database
     * @param titleId the id of this dialog's title.
     * @param item the item used to fill in the initial values of the fields.
     */
    public EditJournalItemDialog(Frame parent, Database database, String titleId, JournalItem item) {
        super(parent, titleId);
        this.parentFrame = parent;
        this.database = database;
        AmountFormat af = Factory.getInstance(AmountFormat.class);
        initDialog(af.formatAmountWithoutCurrency(item.getAmount()), item.getAccount(),
                item.isDebet(), database.getInvoice(item.getInvoiceId()));
    }

    /**
     * Constructor.
     * @param parentFrame the parent dialog of this dialog.
     * @param database the database
     * @param titleId the id of this dialog's title.
     * @param item the item used to fill in the initial values of the fields.
     */
    public EditJournalItemDialog(View view, Database database, String titleId, JournalItem item) {
        super(view, titleId);
        this.parentView = view;
        this.database = database;
        AmountFormat af = Factory.getInstance(AmountFormat.class);
        if (item != null) {
            initDialog(af.formatAmountWithoutCurrency(item.getAmount()), item.getAccount(),
                    item.isDebet(), database.getInvoice(item.getInvoiceId()));
        } else {
            initDialog("", null, true, null);
        }
    }

    /**
     * Initializes the dialog. Adds buttons and labels to this dialog.
     * @param amount used to initialize the amount field
     * @param account used to initialize the account combo box
     * @param debet used to initialize the side combo box
     * @param party used to initialize the party combo box
     */
    private void initDialog(String amount, Account account, boolean debet, Invoice invoice) {
        JPanel labelsAndFieldsPanel = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints labelConstraints = new GridBagConstraints();
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		labelsAndFieldsPanel.setLayout(gridBag);

		labelConstraints.gridx = 0;
		labelConstraints.anchor = GridBagConstraints.EAST;
		labelConstraints.insets = new Insets( 0, 0, 0, 10 );
		fieldConstraints.gridx = 1;
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.anchor = GridBagConstraints.WEST;

        WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
        addComponentToGridBag(labelsAndFieldsPanel, wf.createLabel("gen.account"),
                gridBag, labelConstraints);
        sbAccount = new AccountSelectionBean(database, account);
        addComponentToGridBag(labelsAndFieldsPanel, sbAccount,
                gridBag, fieldConstraints);

        addComponentToGridBag(labelsAndFieldsPanel, wf.createLabel("gen.amount"),
                gridBag, labelConstraints);
        tfAmount = wf.createTextField(amount);
        addComponentToGridBag(labelsAndFieldsPanel, tfAmount,
                gridBag, fieldConstraints);

        addComponentToGridBag(labelsAndFieldsPanel, wf.createLabel("gen.side"),
                gridBag, labelConstraints);

        cbSide = wf.createComboBox(new String[] { "gen.debet", "gen.credit" });
        cbSide.setSelectedIndex(debet ? 0 : 1);
        addComponentToGridBag(labelsAndFieldsPanel, cbSide,
                gridBag, fieldConstraints);

        addComponentToGridBag(labelsAndFieldsPanel, wf.createLabel("gen.invoice"),
                gridBag, labelConstraints);

        invoiceSelector = new InvoiceBean(database);
        invoiceSelector.setSelectedInvoice(invoice);
        addComponentToGridBag(labelsAndFieldsPanel, invoiceSelector,
                gridBag, fieldConstraints);

        componentInitialized(labelsAndFieldsPanel);
    }

    /* (non-Javadoc)
     * @see cf.ui.OkCancelDialog#handleOk()
     */
    @Override
	protected void handleOk() {
        Amount amount;
        AmountFormat af = Factory.getInstance(AmountFormat.class);
        try {
            amount = af.parse(tfAmount.getText(), database.getCurrency());
        }
        catch (ParseException e) {
            if (parentFrame != null) {
                MessageDialog.showMessage(parentFrame, "gen.titleError", "ejid.invalidAmount");
            } else {
                assert parentView != null;
                MessageDialog.showErrorMessage(parentView, "ejid.invalidAmount");
            }
            return;
        }

        Account account = sbAccount.getSelectedAccount();
        boolean debet = cbSide.getSelectedIndex() == 0;
        Invoice invoice = invoiceSelector.getSelectedInvoice();

        enteredJournalItem = new JournalItem(amount, account, debet,
            invoice != null ? invoice.getId() : null, invoice != null ? database.createPaymentId() : null);
        hideDialog();
    }

    /**
     * Gets the journal item thas has been entered.
     * Its value will be set when the user presses the Ok button and the input fields
     * are correct. Otherwise, this variable will be <code>null</code>.
     */
    public JournalItem getEnteredJournalItem()
    {
        return enteredJournalItem;
    }

	/**
	 * Adds a component to a container with a grid bag layout manager.
	 *
	 * @param container the container
	 * @param comp the component
	 * @param gridBag the grid bag layout manager, which must be the layout manager
	 *                for <tt>container</tt>
	 * @param c the constraints for the component
	 */
	private static void addComponentToGridBag( Container container, Component comp,
		GridBagLayout gridBag, GridBagConstraints c)
	{
		gridBag.setConstraints( comp, c );
		container.add(comp);
	}

}
