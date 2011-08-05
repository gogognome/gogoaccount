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
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;

import nl.gogognome.cf.services.ImportBankStatementService;
import nl.gogognome.cf.services.importers.ImportedTransaction;
import nl.gogognome.cf.services.importers.RabobankCSVImporter;
import nl.gogognome.cf.services.importers.TransactionImporter;
import nl.gogognome.gogoaccount.controllers.DeleteJournalController;
import nl.gogognome.gogoaccount.controllers.EditJournalController;
import nl.gogognome.lib.gui.beans.ObjectFormatter;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import cf.engine.Database;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.ui.dialogs.ItemsTableModel;

/**
 * This view allows the user to import a bank statement and create journals
 * based on the bank statement.
 *
 * @author Sander Kooijmans
 */
public class ImportBankStatementView extends View
	implements ModelChangeListener, ListSelectionListener, AddJournalForTransactionView.Plugin{

    private FileModel fileSelectionModel = new FileModel();

    private JTable itemsTable;
    private ItemsTableModel itemsTableModel;

    private JTable transactionsJournalsTable;
    private TransactionsJournalsTableModel transactionJournalsTableModel;

    private ListModel<TransactionImporter> importersModel = new ListModel<TransactionImporter>();

    private JButton importButton;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;

    Database database;

    private InputFieldsColumn vep;

	/**
	 * Creates the view.
	 * @param database the database
	 */
	public ImportBankStatementView(Database database) {
		super();
        this.database = database;
    }

    /**
     * @see View#onInit()
     */
    @Override
    public void onInit() {
    	initModels();
		addComponents();
        addListeners();
        disableEnableButtons();
        updateJournalItemTable();
	}

	private void initModels() {
		List<TransactionImporter> importers = new ArrayList<TransactionImporter>();
		importers.add(new RabobankCSVImporter());
		importersModel.setItems(importers);
	}

	private void addComponents() {
		vep = new InputFieldsColumn();
		vep.addField("importBankStatementView.selectFileToImport", fileSelectionModel);
		vep.addComboBoxField("importBankStatementView.typeOfBankStatement", importersModel,
				new ImporterFormatter());

		ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.LEFT);
		importButton = widgetFactory.createButton("importBankStatementView.import", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleImport();
			}
		});
		buttonPanel.add(importButton);

		JPanel importPanel = new JPanel(new BorderLayout());
		importPanel.add(vep, BorderLayout.NORTH);
		importPanel.add(buttonPanel, BorderLayout.SOUTH);
		importPanel.setBorder(widgetFactory.createTitleBorderWithMarginAndPadding("importBankStatementView.importSettings"));

		// Create table of journals
		transactionJournalsTableModel = new TransactionsJournalsTableModel(
				Collections.<Transaction>emptyList(), database);
		transactionsJournalsTable = widgetFactory.createSortedTable(transactionJournalsTableModel);
        transactionsJournalsTable.setBorder(widgetFactory.createTitleBorderWithPadding("importBankStatementView.transactionsJournals"));

		// Create table of items
		itemsTableModel = new ItemsTableModel(database);
		itemsTable = new JTable(itemsTableModel);
		itemsTable.setRowSelectionAllowed(false);
		itemsTable.setColumnSelectionAllowed(false);

		// Set column widths
		TableColumnModel columnModel = itemsTable.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(300);
		columnModel.getColumn(1).setPreferredWidth(100);
		columnModel.getColumn(2).setPreferredWidth(100);
		columnModel.getColumn(3).setPreferredWidth(300);

		// Set renderers for column 1 and 2.
		columnModel.getColumn(1).setCellRenderer(new RightAlignedRenderer());
		columnModel.getColumn(2).setCellRenderer(new RightAlignedRenderer());

		// Create button panel
		JPanel buttonsPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonsPanel.setOpaque(false);

		editButton = widgetFactory.createButton("ejd.editJournal", new EditAction());
		buttonsPanel.add(editButton);

		addButton = widgetFactory.createButton("ejd.addJournal", new AddAction());
		buttonsPanel.add(addButton);

		deleteButton = widgetFactory.createButton("ejd.deleteJournal", new DeleteAction());
		buttonsPanel.add(deleteButton);

		// Add components to the view.
        JPanel tablesPanel = new JPanel(new GridBagLayout());
        tablesPanel.setOpaque(false);
		tablesPanel.add(new JScrollPane(transactionsJournalsTable),
				SwingUtils.createPanelGBConstraints(0, 1));

        JScrollPane scrollPane = new JScrollPane(itemsTable);
        tablesPanel.add(scrollPane, SwingUtils.createPanelGBConstraints(0, 2));

        setLayout(new BorderLayout());
        add(importPanel, BorderLayout.NORTH);
		add(tablesPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
	}

	private void addListeners() {
		fileSelectionModel.addModelChangeListener(this);
		importersModel.addModelChangeListener(this);
		transactionsJournalsTable.getSelectionModel().addListSelectionListener(this);
        modelChanged(fileSelectionModel);
	}

	private void removeListeners() {
		fileSelectionModel.removeModelChangeListener(this);
		importersModel.removeModelChangeListener(this);
		transactionsJournalsTable.getSelectionModel().removeListSelectionListener(this);
	}

    @Override
    public String getTitle() {
        return textResource.getString("importBankStatementView.title");
    }

    @Override
    public void onClose() {
    	vep.close();
    	removeListeners();
    }

    /**
     * Updates the journal item table so that it shows the items for the
     * specified journal.
     */
    private void updateJournalItemTable() {
    	int row = SwingUtils.getSelectedRowConvertedToModel(transactionsJournalsTable);
    	JournalItem[] items;
    	if (row != -1) {
	    	Journal journal = transactionJournalsTableModel.getRow(row).getJournal();
	    	items = journal != null ? journal.getItems() : new JournalItem[0];
    	} else {
    		items = new JournalItem[0];
    	}
    	itemsTableModel.setJournalItems(items);
    }

	private void handleImport() {
		File file = fileSelectionModel.getFile();
		try {
			TransactionImporter importer = importersModel.getSingleSelectedItem();
			List<ImportedTransaction> transactions = importer.importTransactions(file);
			fileSelectionModel.setEnabled(false, this);
			importersModel.setEnabled(false, this);
			importButton.setEnabled(false);
			addTransactionsToTable(transactions);
			SwingUtils.selectFirstRow(transactionsJournalsTable);
		} catch (FileNotFoundException e) {
			MessageDialog.showErrorMessage(this,
					"importBankStatementView.fileNotFound", file.getAbsoluteFile());
		} catch (Exception e) {
			MessageDialog.showErrorMessage(this, e,
					"importBankStatementView.problemWhileImportingTransactions");
		}
	}

    private void addTransactionsToTable(List<ImportedTransaction> transactions) {
    	for (ImportedTransaction t : transactions) {
    		transactionJournalsTableModel.addRow(new Transaction(t, null));
    	}
	}

	/**
     * This method lets the user edit the selected journal.
     */
    private void editJournalForSelectedTransaction() {
        int row = transactionsJournalsTable.getSelectionModel().getMinSelectionIndex();
        if (row != -1) {
            Journal journal = transactionJournalsTableModel.getRow(row).getJournal();
            EditJournalController controller = new EditJournalController(this, database, journal);
            controller.execute();
            if (controller.isJournalUpdated()) {
            	updateTransactionJournal(row, controller.getUpdatedJournal());
            }
        }
    }

    /**
     * This method lets the user add new journals.
     */
    private void addJournalForSelectedTransaction() {
        AddJournalForTransactionView view = new AddJournalForTransactionView(database, this);
        ViewDialog dialog = new ViewDialog(this, view);
        dialog.showDialog();
    }

    /**
     * This method lets the user delete the selected journal.
     */
    private void deleteJournalFromSelectedTransaction() {
        int row = transactionsJournalsTable.getSelectionModel().getMinSelectionIndex();
        if (row != -1) {
            Journal journal = transactionJournalsTableModel.getRow(row).getJournal();
        	DeleteJournalController controller = new DeleteJournalController(this, database, journal);
        	controller.execute();

        	if (controller.isJournalDeleted()) {
        		updateTransactionJournal(row, null);
        	}
        }
    }

	private void updateTransactionJournal(int row, Journal journal) {
        Transaction t = transactionJournalsTableModel.getRow(row);
    	t.setJournal(journal);
		transactionJournalsTableModel.updateRow(row, t);
		updateJournalItemTable();
	}

	private void disableEnableButtons() {
		int row = SwingUtils.getSelectedRowConvertedToModel(transactionsJournalsTable);
		boolean rowSelected = row != -1;
		boolean journalPresent = rowSelected && transactionJournalsTableModel.getRow(row).getJournal() != null;
		addButton.setEnabled(rowSelected);
		editButton.setEnabled(journalPresent);
		deleteButton.setEnabled(journalPresent);
	}

	@Override
	public void modelChanged(AbstractModel model) {
		importButton.setEnabled(canImportBeStarted());
	}

	private boolean canImportBeStarted() {
		return fileSelectionModel.getFile() != null
				&& fileSelectionModel.getFile().isFile()
				&& importersModel.getSingleSelectedIndex() != -1;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
        //Ignore extra messages.
        if (e.getValueIsAdjusting()) {
        	return;
        }

        updateJournalItemTable();
		disableEnableButtons();
	}

	@Override
	public void journalAdded(Journal journal) {
        int row = transactionsJournalsTable.getSelectionModel().getMinSelectionIndex();
        if (row != -1) {
        	updateTransactionJournal(row, journal);
        	setLinkBetweenImportedTransactionAccountAndAccount(
        			transactionJournalsTableModel.getRow(row).getImportedTransaction(), journal);
        } else {
        	MessageDialog.showErrorMessage(this, "ImportBankStatementView.JournalCreatedButNoTransactionSelected");
        }
	}

	private void setLinkBetweenImportedTransactionAccountAndAccount(
			ImportedTransaction transaction, Journal journal) {
		JournalItem[] items = journal.getItems();
		ImportBankStatementService service = new ImportBankStatementService(database);
		if (items.length == 2) {
			for (JournalItem item : items) {
				String importedAccount;
				if (item.isDebet()) {
					service.setImportedToAccount(transaction, item.getAccount());
				} else {
					service.setImportedFromAccount(transaction, item.getAccount());
				}
			}
		}
	}

	@Override
	public ImportedTransaction getNextImportedTransaction() {
        ImportedTransaction importedTransaction = null;

        int row = transactionsJournalsTable.getSelectionModel().getMinSelectionIndex();
        if (row != -1) {
        	while (row < transactionJournalsTableModel.getRowCount()) {
	        	if (transactionJournalsTableModel.getRow(row).getJournal() != null) {
	        		row++;
	        	} else {
	        		break;
	        	}
        	}
        }

		if (row != -1 && row < transactionJournalsTableModel.getRowCount()) {
			transactionsJournalsTable.getSelectionModel().setSelectionInterval(row, row);
			importedTransaction = transactionJournalsTableModel.getRow(row).getImportedTransaction();
		}
		return importedTransaction;
	}

	private class AddAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			addJournalForSelectedTransaction();
		}
	}

	private class EditAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			editJournalForSelectedTransaction();
		}
	}

	private class DeleteAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			deleteJournalFromSelectedTransaction();
		}
	}
}

class ImporterFormatter implements ObjectFormatter<TransactionImporter> {

	@Override
	public String format(TransactionImporter t) {
		String id;
		if (t instanceof RabobankCSVImporter) {
			id = "importBankStatementView.rabobankCsv";
		} else {
			id = "unknown importer";
		}
		return Factory.getInstance(TextResource.class).getString(id);
	}

}
