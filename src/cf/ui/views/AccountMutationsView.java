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

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import nl.gogognome.lib.gui.beans.ObjectFormatter;
import nl.gogognome.lib.gui.beans.ValuesEditPanel;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Account;
import cf.engine.Database;

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
	private AccountOverviewTableModel model;

	private DateModel dateModel = new DateModel(new Date());
	private ListModel<Account> accountListModel = new ListModel<Account>();

	private ModelChangeListener listener;

	public AccountMutationsView(Database database) {
		super();
		this.database = database;
	}

	@Override
	public String getTitle() {
		return TextResource.getInstance().getString("AccountMutationsView.title");
	}

	@Override
	public void onInit() {
		initModels();
		addComponents();
		addListeners();
	}

	private void initModels() {
		accountListModel.setItems(Arrays.asList(database.getAllAccounts()));
		if (!accountListModel.getItems().isEmpty()) {
			accountListModel.setSelectedIndices(new int[] {0}, null);
		}
	}

	private void addComponents() {
		WidgetFactory wf = WidgetFactory.getInstance();
		model = new AccountOverviewTableModel(database, accountListModel.getSingleSelectedItem(), dateModel.getDate());
		table = wf.createSortedTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		JPanel accountAndDatePanel = new JPanel(new FlowLayout());
		ValuesEditPanel vep = new ValuesEditPanel();
		addCloseable(vep);
		vep.addComboBoxField("AccountMutationsView.account", accountListModel, new AccountFormatter());
		accountAndDatePanel.add(vep);

		vep = new ValuesEditPanel();
		addCloseable(vep);
		vep.addField("AccountMutationsView.date", dateModel);
		accountAndDatePanel.add(vep);

		setLayout(new GridBagLayout());
		add(accountAndDatePanel, SwingUtils.createGBConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                10, 10, 10, 10));
		tableScrollPane = new JScrollPane(table);
		tableScrollPane.setBorder(wf.createTitleBorder("AccountMutationsView.initialTableTitle"));
		add(tableScrollPane, SwingUtils.createPanelGBConstraints(0, 1));
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
		WidgetFactory wf = WidgetFactory.getInstance();

		Account account = accountListModel.getSingleSelectedItem();
		Date date = dateModel.getDate();
		model.setAccountAndDate(account, date);

		if (account != null && date != null) {
			TextResource tr = TextResource.getInstance();
			tableScrollPane.setBorder(wf.createTitleBorder("vao.accountAtDate",
			        account.getId() + " - " + account.getName(),
			        tr.formatDate("gen.dateFormat", date)));
		} else {
			model.clear();
			tableScrollPane.setBorder(wf.createTitleBorder("AccountMutationsView.initialTableTitle"));
		}
	}

	private static class AccountFormatter implements ObjectFormatter<Account> {

		@Override
		public String format(Account a) {
			return a != null ? a.getId() + ' ' + a.getName() : "";
		}

	}
}
