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
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import nl.gogognome.cf.services.importers.ImportedTransaction;
import nl.gogognome.cf.services.importers.RabobankCSVImporter;
import nl.gogognome.lib.gui.beans.ValuesEditPanel;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SortedTable;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.FileSelectionModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Database;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;
import cf.ui.dialogs.EditJournalDialog;
import cf.ui.dialogs.ItemsTableModel;

/**
 * This view allows the user to import a bank statement and create journals
 * based on the bank statement.
 *
 * @author Sander Kooijmans
 */
public class ImportBankStatementView extends View implements ModelChangeListener {

    private FileSelectionModel fileSelectionModel = new FileSelectionModel(null);

    /** The table containing journals. */
    private SortedTable journalsTable;

    /** The table containing journal items. */
    private JTable itemsTable;

    /** The table mdoel for the journal items. */
    private ItemsTableModel itemsTableModel;

    /** The table model for the journals. */
    private TransactionsJournalsTableModel transactionJournalsTableModel;

    private JButton importButton;

    /** The database. */
    Database database;

    private ValuesEditPanel vep;

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
		WidgetFactory wf = WidgetFactory.getInstance();

		vep = new ValuesEditPanel();
		vep.addField("importBankStatementView.selectFileToImport", fileSelectionModel);
//		vep.addField("importBankStatementView.typeOfBankStatement");

		ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
		importButton = wf.createButton("importBankStatementView.import", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleImport();
			}
		});
		buttonPanel.add(importButton);

		JPanel importPanel = new JPanel(new BorderLayout());
		importPanel.add(vep, BorderLayout.NORTH);
		importPanel.add(buttonPanel, BorderLayout.SOUTH);
		importPanel.setBorder(wf.createTitleBorderWithMargin("importBankStatementView.importSettings"));

		// Create table of journals
		transactionJournalsTableModel = new TransactionsJournalsTableModel(
				Collections.<Transaction>emptyList(), database);
		journalsTable = WidgetFactory.getInstance().createSortedTable(transactionJournalsTableModel);
        journalsTable.setTitle("importBankStatementView.transactionsJournals");

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
		TableCellRenderer rightAlignedRenderer = new DefaultTableCellRenderer() {
		    @Override
            public void setValue(Object value) {
		        super.setValue(value);
		        setHorizontalAlignment(SwingConstants.RIGHT);
		    }
		};

		columnModel.getColumn(1).setCellRenderer(rightAlignedRenderer);
		columnModel.getColumn(2).setCellRenderer(rightAlignedRenderer);

		// Create combo box for parties
		JComboBox comboBox = new JComboBox();
		Party[] parties = database.getParties();
		for (int i = 0; i < parties.length; i++)
		{
		    comboBox.addItem(parties[i].getId() + " " + parties[i].getName());
        }
		columnModel.getColumn(3).setCellEditor(new DefaultCellEditor(comboBox));

		// Let items table be updated when another row is selected in the journals table.
		final ListSelectionModel rowSM = journalsTable.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
		    public void valueChanged(ListSelectionEvent e) {
		        //Ignore extra messages.
		        if (e.getValueIsAdjusting()) { return; }

		        if (!rowSM.isSelectionEmpty()) {
		            updateJournalItemTable(rowSM.getMinSelectionIndex());
		        }
		    }
		});

		// Create button panel
		JPanel buttonsPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonsPanel.setOpaque(false);

		JButton editButton = wf.createButton("ejd.editJournal", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                handleEditItem();
            }
		});
		buttonsPanel.add(editButton);

		JButton addButton = wf.createButton("ejd.addJournal", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                handleAddItem();
            }
		});
		buttonsPanel.add(addButton);

		JButton deleteButton = wf.createButton("ejd.deleteJournal", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                handleDeleteItem();
            }
		});
		buttonsPanel.add(deleteButton);

		// Add components to the view.
        JPanel tablesPanel = new JPanel(new GridBagLayout());
        tablesPanel.setOpaque(false);
		tablesPanel.add(journalsTable.getComponent(),
				SwingUtils.createPanelGBConstraints(0, 1));

        JScrollPane scrollPane = new JScrollPane(itemsTable);
        tablesPanel.add(scrollPane, SwingUtils.createPanelGBConstraints(0, 2));

        setLayout(new BorderLayout());
        add(importPanel, BorderLayout.NORTH);
		add(tablesPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        fileSelectionModel.addModelChangeListener(this);
        modelChanged(fileSelectionModel);
	}

    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("importBankStatementView.title");
    }

    @Override
    public void onClose() {
    	vep.deinitialize();
        journalsTable = null;
        transactionJournalsTableModel = null;
    }

    /**
     * Updates the journal item table so that it shows the items for the
     * specified journal.
     * @param row the row of the journal whose items should be shown.
     */
    private void updateJournalItemTable(int row) {
    	Journal journal = transactionJournalsTableModel.getRow(row).getJournal();
    	JournalItem[] items = journal != null ? journal.getItems() : new JournalItem[0];
        itemsTableModel.setJournalItems(items);
    }

	private void handleImport() {
		File file = fileSelectionModel.getFile();
		try {
			RabobankCSVImporter importer = new RabobankCSVImporter(file);
			List<ImportedTransaction> transactions = importer.importTransactions();
			fileSelectionModel.setEnabled(false, this);
			importButton.setEnabled(false);
			addTransactionsToTable(transactions);
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
    private void handleEditItem() {
        int row = journalsTable.getSelectionModel().getMinSelectionIndex();
        if (row != -1) {
            Journal journal = transactionJournalsTableModel.getRow(row).getJournal();
            EditJournalDialog dialog =
                new EditJournalDialog(getParentFrame(), database, "ejd.editJournalTitle", journal, false);
            dialog.showDialog();
            List<Journal> journals = dialog.getEditedJournals();
            if (!journals.isEmpty()) {
                if (journals.size() != 1) {
                    throw new IllegalStateException("journals.length must be 1");
                }
                Journal newJournal = journals.get(0);
                if (!journal.equals(newJournal)) {
                    // the journal was modified
                    if (journal.createsInvoice()) {
                        EditInvoiceView editInvoiceView = new EditInvoiceView(database, "ejd.editInvoiceTitle",
                            database.getInvoice(journal.getIdOfCreatedInvoice()));
                        ViewDialog editInvoiceDialog = new ViewDialog(getParentWindow(), editInvoiceView);
                        editInvoiceDialog.showDialog();
                        Invoice newInvoice = editInvoiceView.getEditedInvoice();
                        if (newInvoice != null) {
                            try {
                                database.updateInvoice(journal.getIdOfCreatedInvoice(), newInvoice);
                            } catch (DatabaseModificationFailedException e) {
                                MessageDialog.showMessage(getParentWindow(), "gen.error", e.getMessage());
                            }
                        }
                    }
                    try {
                        database.updateJournal(journal, newJournal);
                    } catch (DatabaseModificationFailedException e) {
                        MessageDialog.showMessage(getParentWindow(), "gen.error", e.getMessage());
                    }
                    updateJournalItemTable(row);
                }
            }
        }
    }

    /**
     * This method lets the user add new journals.
     */
    private void handleAddItem() {
        EditJournalDialog dialog = new EditJournalDialog(getParentFrame(), database, "ajd.title", true);
        dialog.showDialog();
        List<Journal> journals = dialog.getEditedJournals();
        for (Journal journal : journals) {
            try {
                database.addJournal(journal, true);
            } catch (DatabaseModificationFailedException e) {
                MessageDialog.showMessage(getParentWindow(), "gen.error", e.getMessage());
            }
        }
    }

    /**
     * This method lets the user delete the selected journal.
     */
    private void handleDeleteItem() {
//        int row = journalsTable.getSelectionModel().getMinSelectionIndex();
//        if (row != -1) {
//            if (transactionJournalsTableModel.getRow(row).getJournal().createsInvoice()) {
//                if (!database.getPayments(journalsTableModel.getJournal(row).getIdOfCreatedInvoice()).isEmpty()) {
//                    MessageDialog.showMessage(getParentWindow(), "gen.warning",
//                        TextResource.getInstance().getString("editJournalsView.journalCreatingInvoiceCannotBeDeleted"));
//                    return;
//                }
//            }
//
//            MessageDialog dialog = MessageDialog.showMessage(this,
//                "gen.titleWarning",
//                TextResource.getInstance().getString("editJournalsView.areYouSureAboutDeletion"),
//                new String[] {"gen.yes", "gen.no"});
//            if (dialog.getSelectedButton() == 1) {
//                return; // do not remove the journal
//            }
//
//            try {
//                BookkeepingService.removeJournal(database, journalsTableModel.getJournal(row));
//            } catch (DeleteException e) {
//                MessageDialog.showMessage(getParentWindow(), "gen.error", e.getMessage());
//            }
//        }
    }

	@Override
	public void modelChanged(AbstractModel model) {
		if (model == fileSelectionModel) {
			importButton.setEnabled(fileSelectionModel.getFile() != null
					&& fileSelectionModel.getFile().isFile());
		}

	}

}
