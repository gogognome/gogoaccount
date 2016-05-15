package nl.gogognome.gogoaccount.gui.views;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.gui.controllers.DeleteJournalController;
import nl.gogognome.gogoaccount.gui.controllers.EditJournalController;
import nl.gogognome.gogoaccount.gui.dialogs.JournalEntryDetailsTableModel;
import nl.gogognome.gogoaccount.component.importer.ImportBankStatementService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.component.importer.ImportedTransaction;
import nl.gogognome.gogoaccount.component.importer.RabobankCSVImporter;
import nl.gogognome.gogoaccount.component.importer.TransactionImporter;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.gui.beans.ObjectFormatter;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This view allows the user to import a bank statement and create journals
 * based on the bank statement.
 */
public class ImportBankStatementView extends View implements ModelChangeListener,
        ListSelectionListener, AddJournalForTransactionView.Plugin{

    private static final long serialVersionUID = 1L;

    private final static Logger LOGGER = LoggerFactory.getLogger(ImportBankStatementView.class);

    private final ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
    private final LedgerService ledgerService = ObjectFactory.create(LedgerService.class);
    private final ImportBankStatementService importBankStatementService = ObjectFactory.create(ImportBankStatementService.class);

    private FileModel fileSelectionModel = new FileModel();

    private JournalEntryDetailsTableModel itemsTableModel;

    private JTable transactionsJournalsTable;
    private TransactionsJournalsTableModel transactionJournalsTableModel;

    private ListModel<TransactionImporter> importersModel = new ListModel<>();

    private JButton importButton;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;

    Document document;

    /**
     * Creates the view.
     * @param document the database
     */
    public ImportBankStatementView(Document document) {
        super();
        this.document = document;
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
        List<TransactionImporter> importers = new ArrayList<>();
        importers.add(new RabobankCSVImporter());
        importersModel.setItems(importers);
    }

    private void addComponents() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.addField("importBankStatementView.selectFileToImport", fileSelectionModel);
        ifc.addComboBoxField("importBankStatementView.typeOfBankStatement", importersModel,
                new ImporterFormatter());

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.LEFT);
        importButton = buttonPanel.addButton("importBankStatementView.import", new ImportAction());

        JPanel importPanel = new JPanel(new BorderLayout());
        importPanel.add(ifc, BorderLayout.NORTH);
        importPanel.add(buttonPanel, BorderLayout.SOUTH);
        importPanel.setBorder(widgetFactory.createTitleBorderWithPadding("importBankStatementView.importSettings"));

        // Create table of journals
        transactionJournalsTableModel = new TransactionsJournalsTableModel(
                Collections.<Transaction>emptyList(), document);
        transactionsJournalsTable = widgetFactory.createSortedTable(transactionJournalsTableModel);

        // Create table of items
        itemsTableModel = new JournalEntryDetailsTableModel(document);
        JTable itemsTable = widgetFactory.createTable(itemsTableModel);
        itemsTable.setRowSelectionAllowed(false);
        itemsTable.setColumnSelectionAllowed(false);

        // Create button panel
        ButtonPanel buttonsPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonsPanel.setOpaque(false);

        editButton = buttonsPanel.addButton("ejd.editJournal", new EditAction());
        addButton = buttonsPanel.addButton("ejd.addJournal", new AddAction());
        deleteButton = buttonsPanel.addButton("ejd.deleteJournal", new DeleteAction());

        // Add components to the view.
        JPanel tablesPanel = new JPanel(new GridBagLayout());
        tablesPanel.setOpaque(false);
        tablesPanel.add(widgetFactory.createScrollPane(transactionsJournalsTable,
                "importBankStatementView.transactionsJournals"),
                SwingUtils.createPanelGBConstraints(0, 1));

        JScrollPane scrollPane = new JScrollPane(itemsTable);
        tablesPanel.add(scrollPane, SwingUtils.createPanelGBConstraints(0, 2));

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
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
        removeListeners();
    }

    /**
     * Updates the journal item table so that it shows the items for the
     * specified journal.
     */
    private void updateJournalItemTable() {
        try {
            int row = SwingUtils.getSelectedRowConvertedToModel(transactionsJournalsTable);
            List<JournalEntryDetail> journalEntryDetails;
            if (row != -1) {
                JournalEntry journalEntry = transactionJournalsTableModel.getRow(row).getJournalEntry();
                journalEntryDetails = journalEntry != null ? ledgerService.findJournalEntryDetails(document, journalEntry) : Collections.emptyList();
            } else {
                journalEntryDetails = Collections.emptyList();
            }
            itemsTableModel.setJournalEntryDetails(journalEntryDetails);
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
    }

    private void handleImport() {
        File file = fileSelectionModel.getFile();
        Reader reader = null;
        try {
            reader = new FileReader(file);
            TransactionImporter importer = importersModel.getSelectedItem();
            List<ImportedTransaction> transactions = importer.importTransactions(reader);
            fileSelectionModel.setEnabled(false, this);
            importersModel.setEnabled(false, this);
            if (!transactions.isEmpty()) {
                importButton.setEnabled(false);
                fileSelectionModel.setEnabled(true, this);
                importersModel.setEnabled(true, this);
                addTransactionsToTable(transactions);
                SwingUtils.selectFirstRow(transactionsJournalsTable);
            } else {
                MessageDialog.showWarningMessage(this, "importBankStatementView.noTransactionsFound", file.getAbsolutePath());
            }
        } catch (FileNotFoundException e) {
            MessageDialog.showErrorMessage(this,
                    "importBankStatementView.fileNotFound", file.getAbsoluteFile());
        } catch (Exception e) {
            MessageDialog.showErrorMessage(this, e,
                    "importBankStatementView.problemWhileImportingTransactions");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close the file " + file.getAbsolutePath(), e);
                }
            }
        }
    }

    private void addTransactionsToTable(List<ImportedTransaction> transactions) {
        for (ImportedTransaction t : transactions) {
            transactionJournalsTableModel.addRow(new Transaction(t, null));
        }
    }

    private void editJournalForSelectedTransaction() {
        int row = transactionsJournalsTable.getSelectionModel().getMinSelectionIndex();
        if (row != -1) {
            JournalEntry journalEntry = transactionJournalsTableModel.getRow(row).getJournalEntry();
            EditJournalController controller = new EditJournalController(this, document, journalEntry);
            controller.execute();
            if (controller.isJournalUpdated()) {
                updateTransactionJournal(row, controller.getUpdatedJournalEntry());
            }
        }
    }

    private void addJournalForSelectedTransaction() {
        HandleException.for_(this, () -> {
            AddJournalForTransactionView view = new AddJournalForTransactionView(document, this);
            ViewDialog dialog = new ViewDialog(this, view);
            dialog.showDialog();
        });
    }

    private void deleteJournalFromSelectedTransaction() {
        try {
            int row = transactionsJournalsTable.getSelectionModel().getMinSelectionIndex();
            if (row != -1) {
                JournalEntry journalEntry = transactionJournalsTableModel.getRow(row).getJournalEntry();
                DeleteJournalController controller = new DeleteJournalController(this, document, journalEntry);
                controller.execute();

                if (controller.isJournalDeleted()) {
                    updateTransactionJournal(row, null);
                }
            }
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
    }

    private void updateTransactionJournal(int row, JournalEntry journalEntry) {
        Transaction t = transactionJournalsTableModel.getRow(row);
        t.setJournalEntry(journalEntry);
        transactionJournalsTableModel.updateRow(row, t);
        updateJournalItemTable();
    }

    private void disableEnableButtons() {
        int row = SwingUtils.getSelectedRowConvertedToModel(transactionsJournalsTable);
        boolean rowSelected = row != -1;
        boolean journalPresent = rowSelected && transactionJournalsTableModel.getRow(row).getJournalEntry() != null;
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
                && importersModel.getSelectedIndex() != -1;
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
    public void journalAdded(JournalEntry journalEntry) {
        try {
            int row = transactionsJournalsTable.getSelectionModel().getMinSelectionIndex();
            if (row != -1) {
                updateTransactionJournal(row, journalEntry);
                setLinkBetweenImportedTransactionAccountAndAccount(
                        transactionJournalsTableModel.getRow(row).getImportedTransaction(), journalEntry);
            } else {
                MessageDialog.showErrorMessage(this, "ImportBankStatementView.JournalCreatedButNoTransactionSelected");
            }
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
    }

    private void setLinkBetweenImportedTransactionAccountAndAccount(ImportedTransaction transaction, JournalEntry journalEntry) throws ServiceException {
        List<JournalEntryDetail> journalEntryDetails = ledgerService.findJournalEntryDetails(document, journalEntry);
        if (journalEntryDetails.size() == 2) {
            for (JournalEntryDetail detail : journalEntryDetails) {
                Account account = configurationService.getAccount(document, detail.getAccountId());
                if (detail.isDebet()) {
                    importBankStatementService.setImportedToAccount(document, transaction, account);
                } else {
                    importBankStatementService.setImportedFromAccount(document, transaction, account);
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
                if (transactionJournalsTableModel.getRow(row).getJournalEntry() != null) {
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

    private final class ImportAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            handleImport();
        }
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
