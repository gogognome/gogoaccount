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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.cf.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.gui.beans.ObjectFormatter;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.CurrencyUtil;
import nl.gogognome.lib.util.Factory;
import cf.engine.Account;
import cf.engine.Database;

/**
 * In this view the user can configure the following aspects of the bookkeeping:
 * <ul>
 *   <li>description
 *   <li>start date
 *   <li>currency
 *   <li>accounts
 * </ul>
 *
 * @author Sander Kooijmans
 */
public class ConfigureBookkeepingView extends View {

	private static final long serialVersionUID = 1L;

	private Database database;

    private StringModel descriptionModel = new StringModel();
    private ListModel<Currency> currencyModel = new ListModel<Currency>();
    private DateModel startDateModel = new DateModel();

    private AccountTableModel tableModel;
    private JTable table;

    private ModelChangeListener modelChangeListener;

    private JButton editAccountButton;
    private JButton deleteAccountButton;

    /**
     * Constructor.
     * @param database the database whose bookkeeping is to be configured.
     */
    public ConfigureBookkeepingView(Database database) {
        this.database = database;
    }

    @Override
    public String getTitle() {
        return textResource.getString("ConfigureBookkeepingView.title");
    }

    @Override
    public void onInit() {
    	initModels();
    	addComponents();
        updateButtonState();
    	addListeners();
    }

	private void initModels() {
    	startDateModel.setDate(database.getStartOfPeriod());
    	descriptionModel.setString(database.getDescription());

    	currencyModel.setItems(CurrencyUtil.getAllCurrencies());
       	currencyModel.setSelectedItem(database.getCurrency(), null);
	}

    private void addListeners() {
		modelChangeListener = new ModelChangeListenerImpl();
		startDateModel.addModelChangeListener(modelChangeListener);
		descriptionModel.addModelChangeListener(modelChangeListener);
		currencyModel.addModelChangeListener(modelChangeListener);
	}

	private void addComponents() {
        setLayout(new BorderLayout());

        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding(
        		"ConfigureBookkeepingView.generalSettings"));

        ifc.addField("ConfigureBookkeepingView.description", descriptionModel);
        ifc.addField("ConfigureBookkeepingView.startDate", startDateModel);
        ifc.addComboBoxField("ConfigureBookkeepingView.currency", currencyModel,
        		new CurrencyFormatter());

        // Create panel with accounts table
        JPanel accountsAndButtonsPanel = new JPanel(new BorderLayout());
        accountsAndButtonsPanel.setBorder(BorderFactory.createCompoundBorder(
        		BorderFactory.createTitledBorder(textResource.getString("ConfigureBookkeepingView.accounts")),
        		BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        tableModel = new AccountTableModel(getAccountDefinitions(database));
        table = widgetFactory.createSortedTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateButtonState();
			}
		});
        accountsAndButtonsPanel.add(widgetFactory.createScrollPane(table), BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
        buttonPanel.addButton("ConfigureBookkeepingView.addAccount", new AddAccountAction());
        editAccountButton = buttonPanel.addButton("ConfigureBookkeepingView.editAccount",
        		new EditAccountAction());
        deleteAccountButton = buttonPanel.addButton("ConfigureBookkeepingView.deleteAccount",
        		new DeleteAccountAction());
        accountsAndButtonsPanel.add(buttonPanel, BorderLayout.EAST);

        // Add panels to view
        add(ifc, BorderLayout.NORTH);
        add(accountsAndButtonsPanel, BorderLayout.CENTER);
    }

    @Override
    public void onClose() {
    	removeListeners();
    }

    private void removeListeners() {
		startDateModel.removeModelChangeListener(modelChangeListener);
		descriptionModel.removeModelChangeListener(modelChangeListener);
		currencyModel.removeModelChangeListener(modelChangeListener);
	}

	private void updateDatabaseWithEnteredValues() {
		database.setDescription(descriptionModel.getString());
    	if (startDateModel.getDate() != null) {
    		database.setStartOfPeriod(startDateModel.getDate());
    	}

    	try {
	    	Currency currency = currencyModel.getSelectedItem();
	    	if (currency != null) {
	    		database.setCurrency(currency);
	    	}
    	} catch (Exception e) {
    		// Probably an invalid currency was entered
    	}
	}

	private void updateButtonState() {
    	AccountDefinition accountDefinition = getSelectedAccountDefinition();
    	deleteAccountButton.setEnabled(accountDefinition != null && !accountDefinition.used);
    	editAccountButton.setEnabled(accountDefinition != null);
    }

    /**
     * Gets the definition of the selected account.
     * @return the definition of the selected account or <code>null</code> if not exactly one
     *         row has been selected
     */
    private AccountDefinition getSelectedAccountDefinition() {
    	AccountDefinition accountDefintion = null;
    	int[] rows = SwingUtils.getSelectedRowsConvertedToModel(table);
    	if (rows.length == 1) {
    		accountDefintion = tableModel.getRow(rows[0]);
    	}
    	return accountDefintion;
    }

    private void onDeleteAccount() {
    	AccountDefinition accountDefintion = getSelectedAccountDefinition();
    	Account account = accountDefintion.account;
		int choice = MessageDialog.showYesNoQuestion(this, "ConfigureBookkeepingView.deleteAccountTitle",
				"ConfigureBookkeepingView.deleteAccountAreYouSure",	account.getId() + " - " + account.getName());
		if (choice == MessageDialog.YES_OPTION) {
			try {
				BookkeepingService.deleteAccount(database, account);
				int index = SwingUtils.getSelectedRowConvertedToModel(table);
				tableModel.removeRow(index);
			} catch (ServiceException e) {
				MessageDialog.showErrorMessage(getParentWindow(), e, "ConfigureBookkeepingView.deleteAccountException");
			}
		}
	}

	private void onAddAccount() {
		EditAccountView eav = new EditAccountView(null);
		ViewDialog dialog = new ViewDialog(getParentWindow(), eav);
		dialog.showDialog();
		Account account = eav.getEditedAccount();
		if (account != null) {
			try {
				BookkeepingService.addAccount(database, account);
				AccountDefinition definition = new AccountDefinition();
				definition.account = account;
				tableModel.addRow(definition);
			} catch (ServiceException e) {
				MessageDialog.showErrorMessage(this, e, "ConfigureBookkeepingView.addAccountException");
			}
		}
	}

	private void onEditAccount() {
    	int[] rows = SwingUtils.getSelectedRowsConvertedToModel(table);
    	if (rows.length == 1) {
			AccountDefinition definition = tableModel.getRow(rows[0]);
			EditAccountView eav = new EditAccountView(definition.account);

			ViewDialog dialog = new ViewDialog(getParentWindow(), eav);
			dialog.showDialog();
			Account account = eav.getEditedAccount();
			if (account != null) {
				try {
					BookkeepingService.updateAccount(database, account);
					definition.account = account;
			        tableModel.updateRow(rows[0], definition);
				} catch (ServiceException e) {
					MessageDialog.showErrorMessage(this, e, "ConfigureBookkeepingView.updateAccountException");
				}
			}
		}
	}

	private static List<AccountDefinition> getAccountDefinitions(Database database) {
    	List<Account> accounts = database.getAllAccounts();
    	List<AccountDefinition> result = new ArrayList<AccountDefinition>(accounts.size());
    	for (Account account : accounts) {
    		AccountDefinition accountDefinition = new AccountDefinition();
    		accountDefinition.account = account;
    		accountDefinition.used = database.isAccountUsed(account.getId());
    		result.add(accountDefinition);
    	}
    	return result;
    }

	private final class AddAccountAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent evt) {
		    onAddAccount();
		}
	}

	private final class EditAccountAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent evt) {
		    onEditAccount();
		}
	}

	private final class DeleteAccountAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent evt) {
		    onDeleteAccount();
		}
	}

	private static class AccountDefinition {
        public Account account;
        public boolean used;
    }

	private class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
			updateDatabaseWithEnteredValues();
		}
	}

	private static class CurrencyFormatter implements ObjectFormatter<Currency> {
		@Override
		public String format(Currency currency) {
			return currency.getCurrencyCode();
		}
	}

    private static class AccountTableModel extends AbstractListTableModel<AccountDefinition> {

        private final static ColumnDefinition ID =
            new ColumnDefinition("ConfigureBookkeepingView.id", String.class, 50);

        private final static ColumnDefinition NAME =
            new ColumnDefinition("ConfigureBookkeepingView.name", String.class, 400);

        private final static ColumnDefinition USED =
            new ColumnDefinition("ConfigureBookkeepingView.used", Icon.class, 50);

        private final static ColumnDefinition TYPE =
            new ColumnDefinition("ConfigureBookkeepingView.type", String.class, 150);

        private final static List<ColumnDefinition> COLUMN_DEFINITIONS = Arrays.asList(
            ID, NAME, USED, TYPE
        );

        public AccountTableModel(List<AccountDefinition> accountDefinitions) {
            super(COLUMN_DEFINITIONS, accountDefinitions);
        }

        /** {@inheritDoc} */
        @Override
		public Object getValueAt(int rowIndex, int columnIndex) {
            ColumnDefinition colDef = getColumnDefinition(columnIndex);
            if (ID.equals(colDef)) {
                return getRow(rowIndex).account.getId();
            } else if (NAME.equals(colDef)) {
                return getRow(rowIndex).account.getName();
            } else if (USED.equals(colDef)) {
                return getRow(rowIndex).used ?
                		Factory.getInstance(WidgetFactory.class).createIcon("ConfigureBookkeepingView.tickIcon16") : null;
            } else if (TYPE.equals(colDef)) {
                return Factory.getInstance(TextResource.class).getString("gen.TYPE_" +
                    getRow(rowIndex).account.getType().name());
            }
            return null;
        }
    }
}
