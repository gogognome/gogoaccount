package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.importer.ImportBankStatementService;
import nl.gogognome.gogoaccount.component.importer.ImportedTransaction;
import nl.gogognome.gogoaccount.component.importer.RabobankCSVImporter;
import nl.gogognome.gogoaccount.component.importer.TransactionImporter;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.controllers.DeleteJournalController;
import nl.gogognome.gogoaccount.gui.controllers.EditJournalController;
import nl.gogognome.gogoaccount.gui.dialogs.JournalEntryDetailsTableModel;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.gui.beans.ObjectFormatter;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.*;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * This view allows the user to import a bank statement and create journals
 * based on the bank statement.
 */
public class ImportBankStatementView extends View implements ModelChangeListener,
        ListSelectionListener, AddJournalForTransactionView.Plugin{

    private static final long serialVersionUID = 1L;

    private final static Logger LOGGER = LoggerFactory.getLogger(ImportBankStatementView.class);
    public static final String IMPORT_BANK_STATEMENT_VIEW_PATH = "ImportBankStatementView.path";

    private final ConfigurationService configurationService;
    private final LedgerService ledgerService;
    private final ImportBankStatementService importBankStatementService;
    private final InvoiceService invoiceService;
    private final PartyService partyService;
    private final SettingsService settingsService;
    private final ViewFactory viewFactory;
    private final EditJournalController editJournalController;

    private FileModel fileSelectionModel = new FileModel();

    private JournalEntryDetailsTableModel itemsTableModel;

    private JTable transactionsJournalsTable;
    private TransactionsJournalsTableModel transactionJournalsTableModel;

    private ListModel<TransactionImporter> importersModel = new ListModel<>();

    private JButton importButton;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;

    private final Document document;
    private final AmountFormat amountFormat;

    public ImportBankStatementView(Document document, AmountFormat amountFormat, ConfigurationService configurationService, ImportBankStatementService importBankStatementService, InvoiceService invoiceService, LedgerService ledgerService,
                                   PartyService partyService, SettingsService settingsService, ViewFactory viewFactory, EditJournalController editJournalController) {
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.ledgerService = ledgerService;
        this.importBankStatementService = importBankStatementService;
        this.partyService = partyService;
        this.settingsService = settingsService;
        this.document = document;
        this.amountFormat = amountFormat;
        this.viewFactory = viewFactory;
        this.editJournalController = editJournalController;
    }

    @Override
    public void onInit() {
        initModels();
        addComponents();
        addListeners();
        updateButtonsStatus();
        updateJournalItemTable();
    }

    private void initModels() {
        List<TransactionImporter> importers = new ArrayList<>();
        importers.add(new RabobankCSVImporter());
        importersModel.setItems(importers);
        importersModel.setSelectedIndex(0, null);

        String defaultPath;
        try {
            defaultPath = settingsService.findValueForSetting(document, IMPORT_BANK_STATEMENT_VIEW_PATH);
            if (defaultPath != null) {
                fileSelectionModel.setFile(new File(defaultPath), null);
            }
        } catch (ServiceException e) {
            LOGGER.warn("Ignored exception while getting setting " + IMPORT_BANK_STATEMENT_VIEW_PATH, e);
        }
    }

    private void addComponents() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.addField("importBankStatementView.selectFileToImport", fileSelectionModel);
        ifc.addComboBoxField("importBankStatementView.typeOfBankStatement", importersModel,
                new ImporterFormatter());

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.LEFT);
        importButton = buttonPanel.addButton("importBankStatementView.import", this::handleImport);

        JPanel importPanel = new JPanel(new BorderLayout());
        importPanel.add(ifc, BorderLayout.NORTH);
        importPanel.add(buttonPanel, BorderLayout.SOUTH);
        importPanel.setBorder(widgetFactory.createTitleBorderWithPadding("importBankStatementView.importSettings"));

        // Create table of journals
        transactionJournalsTableModel = new TransactionsJournalsTableModel(amountFormat, invoiceService, partyService, document);
        transactionsJournalsTable = Tables.createSortedTable(transactionJournalsTableModel);

        // Create table of items
        itemsTableModel = new JournalEntryDetailsTableModel(document, configurationService, invoiceService, partyService);
        JTable itemsTable = Tables.createTable(itemsTableModel);
        itemsTable.setRowSelectionAllowed(false);
        itemsTable.setColumnSelectionAllowed(false);

        // Create button panel
        ButtonPanel buttonsPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonsPanel.setOpaque(false);

        editButton = buttonsPanel.addButton("ejd.editJournal", this::editJournalForSelectedTransaction);
        addButton = buttonsPanel.addButton("ejd.addJournal", this::addJournalForSelectedTransaction);
        deleteButton = buttonsPanel.addButton("ejd.deleteJournal", this::deleteJournalFromSelectedTransaction);

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
            int row = getSelectedRowIndexInTableModel();
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
            settingsService.save(document, IMPORT_BANK_STATEMENT_VIEW_PATH, file.getParent());

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
                Tables.selectFirstRow(transactionsJournalsTable);
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
        int row = getSelectedRowIndexInTableModel();
        if (row != -1) {
            JournalEntry journalEntry = transactionJournalsTableModel.getRow(row).getJournalEntry();
            editJournalController.setOwner(this);
            editJournalController.setJournalEntry(journalEntry);
            editJournalController.execute();
            if (editJournalController.isJournalUpdated()) {
                updateTransactionJournal(row, editJournalController.getUpdatedJournalEntry());
            }
        }
    }

    private void addJournalForSelectedTransaction() {
        HandleException.for_(this, () -> {
            AddJournalForTransactionView view = (AddJournalForTransactionView) viewFactory.createView(AddJournalForTransactionView.class);
            view.setInitialValuesPlugin(this);
            ViewDialog dialog = new ViewDialog(this, view);
            dialog.showDialog();
        });
    }

    private void deleteJournalFromSelectedTransaction() {
        try {
            int row = getSelectedRowIndexInTableModel();
            if (row != -1) {
                JournalEntry journalEntry = transactionJournalsTableModel.getRow(row).getJournalEntry();
                DeleteJournalController controller = new DeleteJournalController(this, document, journalEntry);
                controller.execute();

                if (controller.isJournalDeleted()) {
                    updateTransactionJournal(row, null);
                    updateButtonsStatus();
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

    private void updateButtonsStatus() {
        int row = getSelectedRowIndexInTableModel();
        boolean rowSelected = row != -1;
        boolean selectedRowHasJournalEntry = rowSelected && transactionJournalsTableModel.getRow(row).getJournalEntry() != null;
        addButton.setEnabled(rowSelected);
        editButton.setEnabled(selectedRowHasJournalEntry);
        deleteButton.setEnabled(selectedRowHasJournalEntry);
    }

    private int getSelectedRowIndexInTableModel() {
        return Tables.getSelectedRowConvertedToModel(transactionsJournalsTable);
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
        if (!e.getValueIsAdjusting()) {
            updateJournalItemTable();
        }

        updateButtonsStatus();
    }

    @Override
    public void journalAdded(JournalEntry journalEntry) {
        try {
            int row = getSelectedRowIndexInTableModel();
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

        int tableRowIndex = transactionsJournalsTable.getSelectionModel().getMinSelectionIndex();
        if (tableRowIndex != -1) {
            while (tableRowIndex < transactionJournalsTableModel.getRowCount()) {
                int modelIndex = transactionsJournalsTable.convertRowIndexToModel(tableRowIndex);
                if (transactionJournalsTableModel.getRow(modelIndex).getJournalEntry() != null) {
                    tableRowIndex++;
                } else {
                    break;
                }
            }
        }

        if (tableRowIndex != -1 && tableRowIndex < transactionJournalsTableModel.getRowCount()) {
            transactionsJournalsTable.getSelectionModel().setSelectionInterval(tableRowIndex, tableRowIndex);
            int modelIndex = transactionsJournalsTable.convertRowIndexToModel(tableRowIndex);
            importedTransaction = transactionJournalsTableModel.getRow(modelIndex).getImportedTransaction();
        }
        return importedTransaction;
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
