package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.beans.InvoiceBean;
import nl.gogognome.gogoaccount.gui.components.AccountFormatter;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import javax.swing.*;
import java.io.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EditJournalEntryDetailView extends OkCancelView {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ConfigurationService configurationService;
    private final InvoiceService invoiceService;
    private final PartyService partyService;
    private final ViewFactory viewFactory;
    private final MessageDialog messageDialog;
    private final HandleException handleException;

    private final Document document;
    private InvoiceBean invoiceBean;
    private JournalEntryDetail itemToBeEdited;

    private final ListModel<Account> accountListModel = new ListModel<>();
    private final StringModel amountModel = new StringModel();
    private final ListModel<String> sideListModel = new ListModel<>();

    private JournalEntryDetail enteredJournalEntryDetail;

    private AmountFormat amountFormat;

    public EditJournalEntryDetailView(Document document, ConfigurationService configurationService, InvoiceService invoiceService, PartyService partyService, ViewFactory viewFactory) {
        this.document = document;
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.partyService = partyService;
        this.viewFactory = viewFactory;
        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);
    }

    public void setItemToBeEdited(JournalEntryDetail itemToBeEdited) {
        this.itemToBeEdited = itemToBeEdited;
    }

    @Override
    public String getTitle() {
        String id = itemToBeEdited == null ? "EditJournalItemView.titleAdd" : "EditJournalItemView.titleEdit";
        return textResource.getString(id);
    }

    @Override
    public void onInit() {
        handleException.of(() -> {
            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            amountFormat = new AmountFormat(Locale.getDefault(), bookkeeping.getCurrency());

            initModels();
            addComponents();
        });
    }

    private void initModels() {
        handleException.of(() -> {
            accountListModel.setItems(configurationService.findAllAccounts(document));

            List<String> sides = Arrays.asList(textResource.getString("gen.debit"),
                    textResource.getString("gen.credit"));
            sideListModel.setItems(sides);
            sideListModel.setSelectedIndex(0, null);

            invoiceBean = new InvoiceBean(document, partyService, viewFactory, handleException);

            if (itemToBeEdited != null) {
                initModelsForItemToBeEdited();
            } else {
                accountListModel.setSelectedIndex(0, null);
                amountModel.setString(null);
                sideListModel.setSelectedIndex(0, null);
            }
        });
    }

    private void initModelsForItemToBeEdited() throws ServiceException {
        accountListModel.setSelectedItem(configurationService.getAccount(document, itemToBeEdited.getAccountId()), null);
        amountModel.setString(amountFormat.formatAmountWithoutCurrency(itemToBeEdited.getAmount().toBigInteger()));
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
     * Gets the journal item has been entered.
     * Its value will be set when the user presses the ok button and the input fields
     * are correct. Otherwise, this variable will be null.
     * @return the entered journal item or null
     */
    public JournalEntryDetail getEnteredJournalEntryDetail() {
        return enteredJournalEntryDetail;
    }

    @Override
    protected void onOk() {
        handleException.of(() -> {
            Amount amount;
            try {
                amount = new Amount(amountFormat.parse(amountModel.getString()));
            } catch (ParseException e) {
                amount = null;
            }

            Account account = accountListModel.getSelectedItem();
            boolean debet = sideListModel.getSelectedIndex() == 0;
            Invoice invoice = invoiceBean.getSelectedInvoice();

            if (!validateInput(amount, account)) {
                return;
            }

            enteredJournalEntryDetail = new JournalEntryDetail();
            enteredJournalEntryDetail.setAmount(amount);
            enteredJournalEntryDetail.setAccountId(account.getId());
            enteredJournalEntryDetail.setDebet(debet);
            enteredJournalEntryDetail.setInvoiceId(invoice != null ? invoice.getId() : null);
            requestClose();
        });
    }

    private boolean validateInput(Amount amount, Account account) {
        if (amount == null) {
            messageDialog.showWarningMessage("gen.invalidAmount");
            return false;
        }

        if (account == null) {
            messageDialog.showWarningMessage("EditJournalItemView.noAccountSelected");
        }

        return true;
    }

    @Override
    public void onClose() {
    }

}
