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
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.SwingConstants;

import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.util.DateUtil;
import cf.engine.Account;
import cf.engine.Database;
import cf.ui.components.AccountFormatter;


/**
 * This class implements a view that asks the user to enter some data that is needed
 * to closse the bookkeeping.
 *
 * @author Sander Kooijmans
 */
public class CloseBookkeepingView extends View {
	private static final long serialVersionUID = 1L;

	private Database database;

    private DateModel dateModel = new DateModel();
    private StringModel descriptionModel = new StringModel();
	private ListModel<Account> accountListModel = new ListModel<Account>();

    private boolean dataSuccessfullyEntered;

    /**
     * Constructor.
     * @param database the database whose bookkeeping is to be closed
     */
    public CloseBookkeepingView(Database database) {
        this.database = database;
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
        initModels();
        addComponents();
    }

	private void addComponents() {
		InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.addField("closeBookkeepingView.date", dateModel);
		accountListModel.setItems(database.getAllAccounts());
        ifc.addComboBoxField("closeBookkeepingView.equityAccount", accountListModel,
        		new AccountFormatter());

        ifc.addField("closeBookkeepingView.description", descriptionModel);
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.LEFT);
        buttonPanel.addButton("gen.ok", new OkAction());
        buttonPanel.addButton("gen.cancel", closeAction);

        setLayout(new GridBagLayout());
        add(ifc, SwingUtils.createPanelGBConstraints(0, 0));
        add(buttonPanel, SwingUtils.createPanelGBConstraints(0, 1));
	}

    private void onOk() {
        if (dateModel.getDate() == null) {
            MessageDialog.showMessage(this, "gen.warning", "closeBookkeepingView.enterDate");
            return;
        }

        if (accountListModel.getSelectedIndex() == -1) {
            MessageDialog.showMessage(this, "gen.warning", "closeBookkeepingView.selectAccount");
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

    private void initModels() {
        dateModel.setDate(DateUtil.addYears(database.getStartOfPeriod(), 1), null);

        String description = database.getDescription();
        int year = DateUtil.getField(database.getStartOfPeriod(), Calendar.YEAR);
        int nextYear = year+1;
        description = description.replace(Integer.toString(year), Integer.toString(nextYear));
        descriptionModel.setString(description, null);
    }

    private final class OkAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    onOk();
		}
	}

}
