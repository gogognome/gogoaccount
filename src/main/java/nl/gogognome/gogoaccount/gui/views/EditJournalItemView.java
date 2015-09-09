package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.gui.beans.InvoiceBean;
import nl.gogognome.gogoaccount.gui.components.AccountFormatter;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;

import javax.swing.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

/**
 * This view allows the user to edit a journal item.
 */
public class EditJournalItemView extends OkCancelView {

	private static final long serialVersionUID = 1L;

    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);

	private Document document;
    private InvoiceBean invoiceBean;
    private JournalItem itemToBeEdited;

    private ListModel<Account> accountListModel = new ListModel<>();
    private StringModel amountModel = new StringModel();
    private ListModel<String> sideListModel = new ListModel<>();

    private JournalItem enteredJournalItem;

    private AmountFormat amountFormat = Factory.getInstance(AmountFormat.class);

    /**
     * Constructor.
     * @param document the database
     * @param item the item used to fill in the initial values of the fields.
     */
    public EditJournalItemView(Document document, JournalItem item) {
    	this.document = document;
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
		try {
            accountListModel.setItems(ObjectFactory.create(ConfigurationService.class).findAllAccounts(document));

            List<String> sides = Arrays.asList(textResource.getString("gen.debet"),
                    textResource.getString("gen.credit"));
            sideListModel.setItems(sides);
            sideListModel.setSelectedIndex(0, null);

            invoiceBean = new InvoiceBean(document);

            if (itemToBeEdited != null) {
                initModelsForItemToBeEdited();
            }
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            close();
        }
	}

	private void initModelsForItemToBeEdited() throws ServiceException {
		accountListModel.setSelectedItem(itemToBeEdited.getAccount(), null);
		amountModel.setString(amountFormat.formatAmountWithoutCurrency(itemToBeEdited.getAmount()));
		sideListModel.setSelectedIndex(itemToBeEdited.isDebet() ? 0 : 1, null);

		Invoice invoice = itemToBeEdited.getInvoiceId() != null ? invoiceService.getInvoice(document, itemToBeEdited.getInvoiceId()) : null;
		invoiceBean.setSelectedInvoice(invoice);
	}

	@Override
	protected JComponent createCenterComponent() {
		InputFieldsColumn column = new InputFieldsColumn();
		addCloseable(column);

		column.addComboBoxField("EditJournalItemView.account", accountListModel,
				new AccountFormatter());
		column.addField("EditJournalItemView.amount", amountModel);
		column.addComboBoxField("EditJournalItemView.side", sideListModel, null);
		column.addVariableSizeField("EditJournalItemView.invoice", invoiceBean);

		return column;
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

	@Override
	protected void onOk() {
        try {
            Bookkeeping bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
            Amount amount;
            try {
                amount = amountFormat.parse(amountModel.getString(), bookkeeping.getCurrency());
            } catch (ParseException e) {
                amount = null;
            }

            Account account = accountListModel.getSelectedItem();
            boolean debet = sideListModel.getSelectedIndex() == 0;
            Invoice invoice = invoiceBean.getSelectedInvoice();

            if (!validateInput(amount, account)) {
                return;
            }

            enteredJournalItem = new JournalItem(amount, account, debet,
                    invoice != null ? invoice.getId() : null,
                    invoice != null ? "TODO: Fix this" : null);
            requestClose();
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
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

}
