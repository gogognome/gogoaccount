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
import java.util.Arrays;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.cf.services.ServiceException;
import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.lib.gui.beans.InputFieldsRow;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.ui.components.AccountFormatter;

/**
 * This view shows all mutations for an account.
 *
 * @author Sander Kooijmans
 */
public class AccountMutationsView extends View {

	private static final long serialVersionUID = 1L;

	private Database database;

	private JTable table;
	private JScrollPane tableScrollPane;
	private AccountOverviewTableModel tableModel;

	private DateModel dateModel = new DateModel(new Date());
	private ListModel<Account> accountListModel = new ListModel<Account>();

	private ModelChangeListener modelListener;
	private DatabaseListener databaseListener;

	private Report report;

	public AccountMutationsView(Database database) {
		super();
		this.database = database;
	}

	@Override
	public String getTitle() {
		return textResource.getString("AccountMutationsView.title");
	}

	@Override
	public void onInit() {
		initModels();
		addComponents();
		addListeners();
		updateTableModel();

		if (!accountListModel.getItems().isEmpty()) {
			accountListModel.setSelectedIndex(0, null);
		}
	}

	private void initModels() {
		tableModel = new AccountOverviewTableModel();
		setAccountsInListModel();
	}

	private void addComponents() {
		JPanel northPanel = createInputFieldsPanel();
		northPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));

		table = widgetFactory.createSortedTable(tableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableScrollPane = widgetFactory.createScrollPane(table);

		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());
		add(northPanel, BorderLayout.NORTH);
		add(tableScrollPane, BorderLayout.CENTER);
	}

	private JPanel createInputFieldsPanel() {
		InputFieldsRow row = new InputFieldsRow();
		addCloseable(row);

		row.addComboBoxField("AccountMutationsView.account", accountListModel,
				new AccountFormatter());
		row.addField("AccountMutationsView.date", dateModel);

		return row;
	}

	@Override
	public void onClose() {
		removeListeners();
	}

	private void addListeners() {
		modelListener = new ModelChangeListenerImpl();
		dateModel.addModelChangeListener(modelListener);
		accountListModel.addModelChangeListener(modelListener);

		databaseListener = new DatabaseListenerImpl();
		database.addListener(databaseListener);
	}

	private void removeListeners() {
		database.removeListener(databaseListener);
		dateModel.removeModelChangeListener(modelListener);
		accountListModel.removeModelChangeListener(modelListener);
	}

	private void updateReportAndTableModel() {
		Date date = dateModel.getDate();
		if (date != null) {
			updateReport(date);
		} else {
			report = null;
		}

		updateTableModel();
	}

	private void updateReport(Date date) {
		try {
			report = BookkeepingService.createReport(database, date);
		} catch (ServiceException e) {
			report = null;
			MessageDialog.showErrorMessage(this, e, "gen.internalError");
		}
	}

	private void updateTableModel() {
		Account account = accountListModel.getSelectedItem();
		tableModel.setAccountAndDate(report, account);

		if (account != null && report != null) {
			tableScrollPane.setBorder(widgetFactory.createTitleBorder("vao.accountAtDate",
			        account.getId() + " - " + account.getName(),
			        textResource.formatDate("gen.dateFormat", report.getEndDate())));
		} else {
			tableModel.clear();
			tableScrollPane.setBorder(widgetFactory.createTitleBorder("AccountMutationsView.initialTableTitle"));
		}
	}

	private void setAccountsInListModel() {
		accountListModel.setItems(Arrays.asList(database.getAllAccounts()));
	}

	private final class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
			if (model == accountListModel && report != null) {
				updateTableModel();
			} else {
				updateReportAndTableModel();
			}
		}
	}

	private final class DatabaseListenerImpl implements DatabaseListener {
		@Override
		public void databaseChanged(Database db) {
			setAccountsInListModel();
			updateReportAndTableModel();
		}
	}
}
