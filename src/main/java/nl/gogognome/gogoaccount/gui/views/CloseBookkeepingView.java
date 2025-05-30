package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.gui.components.AccountFormatter;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;


/**
 * This class implements a view that asks the user to enter some data that is needed
 * to closse the bookkeeping.
 */
public class CloseBookkeepingView extends OkCancelView {
	private static final long serialVersionUID = 1L;

	private final Document document;
    private final ConfigurationService configurationService;

    private DateModel dateModel = new DateModel();
    private StringModel descriptionModel = new StringModel();
	private ListModel<Account> accountListModel = new ListModel<>();

    private boolean dataSuccessfullyEntered;
    private MessageDialog messageDialog;

    public CloseBookkeepingView(Document document, ConfigurationService configurationService) {
        this.document = document;
        this.configurationService = configurationService;
        messageDialog = new MessageDialog(textResource, this);
    }

    @Override
    public String getTitle() {
        return textResource.getString("closeBookkeepingView.title");
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onInit() {
        try {
            initModels();
            addComponents();
        } catch (ServiceException e) {
            messageDialog.showErrorMessage(e, "gen.problemOccurred");
            close();
        }
    }

    @Override
    protected JComponent createCenterComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = widgetFactory.createLabel("closeBookkeepingView.helpText.html");
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(label, BorderLayout.NORTH);

        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);

        ifc.addField("closeBookkeepingView.date", dateModel);
        ifc.addComboBoxField("closeBookkeepingView.equityAccount", accountListModel, new AccountFormatter());

        ifc.addField("closeBookkeepingView.description", descriptionModel);

        panel.add(ifc, BorderLayout.CENTER);

    	return panel;
    }

    @Override
	protected void onOk() {
        if (dateModel.getDate() == null) {
            messageDialog.showWarningMessage("closeBookkeepingView.enterDate");
            return;
        }

        if (accountListModel.getSelectedIndex() == -1) {
            messageDialog.showWarningMessage("closeBookkeepingView.selectAccount");
            return;
        }

        dataSuccessfullyEntered = true;
        closeAction.actionPerformed(null);
    }

    /**
     * Gets the closing date of the bookkeeping.
     * @return the closing date
     */
    public Date getDate() {
        if (dataSuccessfullyEntered) {
            return dateModel.getDate();
        } else {
            return null;
        }
    }

    /**
     * Gets the account to which the result of the bookkeeping is added.
     * @return the account
     */
    public Account getAccountToAddResultTo() {
        if (dataSuccessfullyEntered) {
            return accountListModel.getSelectedItem();
        } else {
            return null;
        }
    }

    /**
     * Gets the description of the new bookkeeping.
     * @return the description
     */
    public String getDescription() {
        return descriptionModel.getString();
    }

    private void initModels() throws ServiceException {
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        dateModel.setDate(DateUtil.addYears(bookkeeping.getStartOfPeriod(), 1), null);
		accountListModel.setItems(configurationService.findAccountsOfType(document, AccountType.EQUITY));
        if (!accountListModel.getItems().isEmpty()) {
            accountListModel.setSelectedIndex(0, null);
        }

        String description = bookkeeping.getDescription();
        int year = DateUtil.getField(bookkeeping.getStartOfPeriod(), Calendar.YEAR);
        int nextYear = year+1;
        description = description.replace(Integer.toString(year), Integer.toString(nextYear));
        descriptionModel.setString(description, null);
    }
}
