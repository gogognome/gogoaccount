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

import nl.gogognome.lib.gui.beans.InputFieldsRow;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import cf.engine.Account;
import cf.engine.Database;
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

	private ModelChangeListener listener;

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
	}

	private void initModels() {
		tableModel = new AccountOverviewTableModel(database,
				accountListModel.getSingleSelectedItem(), dateModel.getDate());
		accountListModel.setItems(Arrays.asList(database.getAllAccounts()));
		if (!accountListModel.getItems().isEmpty()) {
			accountListModel.setSelectedIndices(new int[] {0}, null);
		}
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
		listener = new ModelChangeListener() {
			@Override
			public void modelChanged(AbstractModel model) {
				updateTableModel();
			}
		};

		dateModel.addModelChangeListener(listener);
		accountListModel.addModelChangeListener(listener);
	}

	private void removeListeners() {
		dateModel.removeModelChangeListener(listener);
		accountListModel.removeModelChangeListener(listener);
	}

	private void updateTableModel() {
		Account account = accountListModel.getSingleSelectedItem();
		Date date = dateModel.getDate();
		tableModel.setAccountAndDate(account, date);

		if (account != null && date != null) {
			tableScrollPane.setBorder(widgetFactory.createTitleBorder("vao.accountAtDate",
			        account.getId() + " - " + account.getName(),
			        textResource.formatDate("gen.dateFormat", date)));
		} else {
			tableModel.clear();
			tableScrollPane.setBorder(widgetFactory.createTitleBorder("AccountMutationsView.initialTableTitle"));
		}
	}
}
