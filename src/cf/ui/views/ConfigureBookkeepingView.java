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
import java.awt.GridBagLayout;
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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.cf.services.CreationException;
import nl.gogognome.cf.services.DeleteException;
import nl.gogognome.cf.services.ServiceException;
import nl.gogognome.framework.View;
import nl.gogognome.framework.ViewDialog;
import nl.gogognome.framework.models.AbstractModel;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.framework.models.ModelChangeListener;
import nl.gogognome.swing.AbstractListSortedTableModel;
import nl.gogognome.swing.ButtonPanel;
import nl.gogognome.swing.ColumnDefinition;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.SortedTable;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
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

    private Database database;

    private DateModel startDateModel = new DateModel();
    private AccountTableModel tableModel;
    private SortedTable table;

    private JTextField tfDescription = new JTextField();
    private DateSelectionBean dsbStartDate = new DateSelectionBean(startDateModel);
    private JTextField tfCurrency = new JTextField();

    private JButton editAccountButton;
    private JButton deleteAccountButton;

    /**
     * Constructor.
     * @param database the database whose bookkeeping is to be configured.
     */
    public ConfigureBookkeepingView(Database database) {
        this.database = database;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("ConfigureBookkeepingView.title");
    }

    /** {@inheritDoc} */
    @Override
    public void onClose() {
    }

    /** {@inheritDoc} */
    @Override
    public void onInit() {
        setLayout(new BorderLayout());

        TextResource tr = TextResource.getInstance();
        WidgetFactory wf = WidgetFactory.getInstance();

        // Create panel with general settings
        JPanel generalSettingsPanel = new JPanel(new GridBagLayout());
        generalSettingsPanel.setBorder(BorderFactory.createCompoundBorder(
        		BorderFactory.createTitledBorder(tr.getString("ConfigureBookkeepingView.generalSettings")),
        		BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        int row = 0;
        tfDescription.setText(database.getDescription());
        generalSettingsPanel.add(wf.createLabel("ConfigureBookkeepingView.description", tfDescription),
            SwingUtils.createLabelGBConstraints(0, row));
        generalSettingsPanel.add(tfDescription,
            SwingUtils.createTextFieldGBConstraints(1, row));
        tfDescription.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) { onDescriptionChanged(); }
			public void insertUpdate(DocumentEvent e) { onDescriptionChanged(); }
			public void changedUpdate(DocumentEvent e) { onDescriptionChanged(); }
		});
        row++;

        startDateModel.setDate(database.getStartOfPeriod(), null);
        generalSettingsPanel.add(wf.createLabel("ConfigureBookkeepingView.startDate", dsbStartDate),
            SwingUtils.createLabelGBConstraints(0, row));
        generalSettingsPanel.add(dsbStartDate,
            SwingUtils.createTextFieldGBConstraints(1, row));
        startDateModel.addModelChangeListener(new ModelChangeListener() {
			public void modelChanged(AbstractModel model) {
				onStartDateChanged();
			}
		});
        row++;

        tfCurrency.setText(database.getCurrency().getCurrencyCode());
        tfCurrency.setColumns(4);
        generalSettingsPanel.add(wf.createLabel("ConfigureBookkeepingView.currency", tfCurrency),
            SwingUtils.createLabelGBConstraints(0, row));
        generalSettingsPanel.add(tfCurrency,
            SwingUtils.createLabelGBConstraints(1, row));
        tfCurrency.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) { onCurrencyChanged(); }
			public void insertUpdate(DocumentEvent e) { onCurrencyChanged(); }
			public void changedUpdate(DocumentEvent e) { onCurrencyChanged(); }
		});
        row++;

        // Create panel with accounts table
        JPanel accountsAndButtonsPanel = new JPanel(new BorderLayout());
        accountsAndButtonsPanel.setBorder(BorderFactory.createCompoundBorder(
        		BorderFactory.createTitledBorder(tr.getString("ConfigureBookkeepingView.accounts")),
        		BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        tableModel = new AccountTableModel(getAccountDefinitions(database));
        table = wf.createSortedTable(tableModel, JTable.AUTO_RESIZE_OFF);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				updateButtonState();
			}
		});
        accountsAndButtonsPanel.add(table.getComponent(), BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
        buttonPanel.add(wf.createButton("ConfigureBookkeepingView.addAccount", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                onAddAccount();
            }
        }));
        editAccountButton = wf.createButton("ConfigureBookkeepingView.editAccount", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                onEditAccount();
            }
        });
        buttonPanel.add(editAccountButton);
        deleteAccountButton = wf.createButton("ConfigureBookkeepingView.deleteAccount", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                onDeleteAccount();
            }
        });
        buttonPanel.add(deleteAccountButton);
        accountsAndButtonsPanel.add(buttonPanel, BorderLayout.EAST);

        // Add panels to view
        add(generalSettingsPanel, BorderLayout.NORTH);
        add(accountsAndButtonsPanel, BorderLayout.CENTER);

        updateButtonState();
    }

    private void onDescriptionChanged() {
		database.setDescription(tfDescription.getText());
	}

    private void onStartDateChanged() {
    	if (startDateModel.getDate() != null) {
    		database.setStartOfPeriod(startDateModel.getDate());
    	}
    }

    private void onCurrencyChanged() {
    	try {
	    	Currency currency = Currency.getInstance(tfCurrency.getText());
	    	database.setCurrency(currency);
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
    	int[] rows = table.getSelectedRows();
    	if (rows.length == 1) {
    		accountDefintion = tableModel.getRow(rows[0]);
    	}
    	return accountDefintion;
    }

    private void onDeleteAccount() {
    	AccountDefinition accountDefintion = getSelectedAccountDefinition();
    	Account account = accountDefintion.account;
    	String question = TextResource.getInstance().getString("ConfigureBookkeepingView.deleteAccountAreYouSure",
    			account.getId() + " - " + account.getName());
		MessageDialog dialog = MessageDialog.showMessage(this, "ConfigureBookkeepingView.deleteAccountTitle",
				question, new String[] { "gen.yes", "gen.no" } );
		if (dialog.getSelectedButton() == 0) {
			try {
				BookkeepingService.deleteAccount(database, account);
				int index = table.getSelectedRows()[0];
				tableModel.removeRow(index);
			} catch (DeleteException e) {
				MessageDialog.showMessage(getParentWindow(), e);
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
				table.setSortingStatus(0, SortedTable.ASCENDING);
			} catch (CreationException e) {
				MessageDialog.showMessage(getParentWindow(), e);
			}
		}
	}

	private void onEditAccount() {
    	int[] rows = table.getSelectedRows();
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
					MessageDialog.showMessage(getParentWindow(), e);
				}
			}
		}
	}

	private static List<AccountDefinition> getAccountDefinitions(Database database) {
    	Account[] accounts = database.getAllAccounts();
    	List<AccountDefinition> result = new ArrayList<AccountDefinition>(accounts.length);
    	for (Account account : accounts) {
    		AccountDefinition accountDefinition = new AccountDefinition();
    		accountDefinition.account = account;
    		accountDefinition.used = database.isAccountUsed(account.getId());
    		result.add(accountDefinition);
    	}
    	return result;
    }

    private static class AccountDefinition {
        public Account account;
        public boolean used;
    }

    private static class AccountTableModel extends AbstractListSortedTableModel<AccountDefinition> {

        private final static ColumnDefinition ID =
            new ColumnDefinition("ConfigureBookkeepingView.id", String.class, 50, null, null);

        private final static ColumnDefinition NAME =
            new ColumnDefinition("ConfigureBookkeepingView.name", String.class, 400, null, null);

        private final static ColumnDefinition USED =
            new ColumnDefinition("ConfigureBookkeepingView.used", Icon.class, 50, null, null);

        private final static ColumnDefinition TYPE =
            new ColumnDefinition("ConfigureBookkeepingView.type", String.class, 150, null, null);

        private final static List<ColumnDefinition> COLUMN_DEFINITIONS = Arrays.asList(
            ID, NAME, USED, TYPE
        );

        public AccountTableModel(List<AccountDefinition> accountDefinitions) {
            super(COLUMN_DEFINITIONS, accountDefinitions);
        }

        /** {@inheritDoc} */
        public Object getValueAt(int rowIndex, int columnIndex) {
            ColumnDefinition colDef = getColumnDefinition(columnIndex);
            if (ID.equals(colDef)) {
                return getRow(rowIndex).account.getId();
            } else if (NAME.equals(colDef)) {
                return getRow(rowIndex).account.getName();
            } else if (USED.equals(colDef)) {
                return getRow(rowIndex).used ?
                    WidgetFactory.getInstance().createIcon("ConfigureBookkeepingView.tickIcon16") : null;
            } else if (TYPE.equals(colDef)) {
                return TextResource.getInstance().getString("gen.TYPE_" +
                    getRow(rowIndex).account.getType().name());
            }
            return null;
        }
    }
}
