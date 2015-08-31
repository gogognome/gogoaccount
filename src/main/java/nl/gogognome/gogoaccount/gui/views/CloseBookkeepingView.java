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
package nl.gogognome.gogoaccount.gui.views;

import java.util.Calendar;
import java.util.Date;

import javax.swing.JComponent;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.gui.components.AccountFormatter;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.util.DateUtil;


/**
 * This class implements a view that asks the user to enter some data that is needed
 * to closse the bookkeeping.
 *
 * @author Sander Kooijmans
 */
public class CloseBookkeepingView extends OkCancelView {
	private static final long serialVersionUID = 1L;

	private Document document;

    private DateModel dateModel = new DateModel();
    private StringModel descriptionModel = new StringModel();
	private ListModel<Account> accountListModel = new ListModel<>();

    private boolean dataSuccessfullyEntered;

    /**
     * Constructor.
     * @param document the database whose bookkeeping is to be closed
     */
    public CloseBookkeepingView(Document document) {
        this.document = document;
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
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            close();
        }
    }

    @Override
    protected JComponent createCenterComponent() {
		InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);

        ifc.addField("closeBookkeepingView.date", dateModel);
        ifc.addComboBoxField("closeBookkeepingView.equityAccount", accountListModel,
        		new AccountFormatter());

        ifc.addField("closeBookkeepingView.description", descriptionModel);

    	return ifc;
    }

    @Override
	protected void onOk() {
        if (dateModel.getDate() == null) {
            MessageDialog.showWarningMessage(this, "closeBookkeepingView.enterDate");
            return;
        }

        if (accountListModel.getSelectedIndex() == -1) {
            MessageDialog.showWarningMessage(this, "closeBookkeepingView.selectAccount");
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
        dateModel.setDate(DateUtil.addYears(document.getStartOfPeriod(), 1), null);
		accountListModel.setItems(ObjectFactory.create(ConfigurationService.class).findAllAccounts(document));

        String description = document.getDescription();
        int year = DateUtil.getField(document.getStartOfPeriod(), Calendar.YEAR);
        int nextYear = year+1;
        description = description.replace(Integer.toString(year), Integer.toString(nextYear));
        descriptionModel.setString(description, null);
    }
}
