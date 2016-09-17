package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentListener;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.FormattedJournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.controllers.DeleteJournalController;
import nl.gogognome.gogoaccount.gui.controllers.EditJournalController;
import nl.gogognome.gogoaccount.gui.dialogs.JournalEntryDetailsTableModel;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.action.ActionWrapper;
import nl.gogognome.lib.swing.action.Actions;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.models.Tables;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.criteria.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static nl.gogognome.lib.util.StringUtil.isNullOrEmpty;

/**
 * This class allows the user to browse the journals and to edit individual journals.
 */
public class EditJournalsView extends View {

	private static final long serialVersionUID = 1L;

    private Logger logger = LoggerFactory.getLogger(EditJournalsView.class);

    private Document document;

    private JTable journalEntryDetailsTable;
    private JournalEntryDetailsTableModel journalEntryDetailsTableModel;

    private FormattedJournalEntriesTableModel journalEntriesTableModel;
    private JTable journalEntriesTable;
    private StringModel searchCriterionModel = new StringModel();

    private final ConfigurationService configurationService;
    private final InvoiceService invoiceService;
    private final LedgerService ledgerService;
    private final PartyService partyService;
    private final ViewFactory viewFactory;
    private final DeleteJournalController deleteJournalController;
    private final EditJournalController editJournalController;

    private DocumentListener documentListener;

	public EditJournalsView(Document document, ConfigurationService configurationService, InvoiceService invoiceService,
                            LedgerService ledgerService, PartyService partyService, ViewFactory viewFactory,
                            DeleteJournalController deleteJournalController, EditJournalController editJournalController) {
        this.document = document;
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.ledgerService = ledgerService;
        this.partyService = partyService;
        this.deleteJournalController = deleteJournalController;
        this.editJournalController = editJournalController;
        this.viewFactory = viewFactory;
    }

    @Override
    public void onInit() {
        try {
            journalEntriesTableModel = new FormattedJournalEntriesTableModel(getFilteredRows());
            journalEntriesTable = Tables.createSortedTable(journalEntriesTableModel);

            // Create table of items
            journalEntryDetailsTableModel = new JournalEntryDetailsTableModel(document, configurationService, invoiceService, partyService);
            journalEntryDetailsTable = Tables.createTable(journalEntryDetailsTableModel);
            journalEntryDetailsTable.setRowSelectionAllowed(false);
            journalEntryDetailsTable.setColumnSelectionAllowed(false);

            Tables.onSelectionChange(journalEntriesTable, () -> {
                int row = Tables.getSelectedRowConvertedToModel(journalEntriesTable);
                if (row != -1) {
                    updateJournalItemTable(row);
                }
            });

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            add(buildPanelWithTables(), BorderLayout.CENTER);
            add(createButtonPanel(), BorderLayout.SOUTH);

            Tables.selectFirstRow(journalEntriesTable);

            documentListener = doc -> onSearch();
            document.addListener(documentListener);
        } catch (ServiceException e) {
            logger.warn("Ignored exception: " + e.getMessage(), e);
            close();
        }
	}

    private JPanel buildPanelWithTables() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("editJournalsView.filter"));

        ifc.addField("gen.filterCriterion", searchCriterionModel);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        ActionWrapper actionWrapper = widgetFactory.createActionWrapper("gen.btnSearch");
        actionWrapper.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSearch();
            }
        });
        JButton search = new JButton(actionWrapper);

        buttonPanel.add(search);
        ifc.add(buttonPanel, SwingUtils.createGBConstraints(0, 10, 2, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, 5, 0, 0, 0));
        setDefaultButton(search);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(ifc, BorderLayout.NORTH);

        JPanel tablesPanel = new JPanel(new GridBagLayout());
        tablesPanel.setOpaque(false);

        JScrollPane scrollPane = widgetFactory.createScrollPane(journalEntriesTable, "editJournalsView.journals");
        tablesPanel.add(scrollPane, SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 3.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, 0, 0, 0, 0));

        scrollPane = widgetFactory.createScrollPane(journalEntryDetailsTable, "editJournalsView.journalItems");
        tablesPanel.add(scrollPane, SwingUtils.createPanelGBConstraints(0, 1));

        panel.add(tablesPanel, BorderLayout.CENTER);
        return panel;
    }

    private void onSearch() {
        try {
            journalEntriesTableModel.setRows(getFilteredRows());
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
    }

    private ButtonPanel createButtonPanel() {
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.setOpaque(false);

        Action editAction = Actions.build(this, this::editJournal);
        addCloseable(Tables.onSelectionChange(journalEntriesTable, () -> editAction.setEnabled(journalEntriesTable.getSelectedRowCount() == 1)));
        buttonPanel.addButton("ejd.editJournal", editAction);

        buttonPanel.addButton("ejd.addJournal", this::addJournal);

        Action deleteAction = Actions.build(this, this::deleteJournal);
        addCloseable(Tables.onSelectionChange(journalEntriesTable, () -> deleteAction.setEnabled(journalEntriesTable.getSelectedRowCount() > 0)));
        buttonPanel.addButton("ejd.deleteJournal", deleteAction);
        return buttonPanel;
    }

    @Override
    public String getTitle() {
        return textResource.getString("editJournalsView.title");
    }

    @Override
    public void onClose() {
        document.removeListener(documentListener);
    }

    /**
     * Updates the journal item table so that it shows the items for the
     * specified journal.
     * @param row the row of the journal whose items should be shown.
     */
    private void updateJournalItemTable(int row) throws ServiceException {
        journalEntryDetailsTableModel.setJournalEntryDetails(ledgerService.findJournalEntryDetails(document, journalEntriesTableModel.getRow(row).journalEntry));
    }

    /**
     * This method lets the user edit the selected journal.
     */
    private void editJournal() {
        try {
            int row = Tables.getSelectedRowConvertedToModel(journalEntriesTable);
            if (row != -1) {
                JournalEntry journalEntry = journalEntriesTableModel.getRow(row).journalEntry;
                editJournalController.setOwner(this);
                editJournalController.setJournalEntry(journalEntry);
                editJournalController.execute();
                updateJournalItemTable(row);
            }
        } catch (ServiceException e) {
            logger.warn("Ignored exception: " + e.getMessage(), e);
        }
    }

    /**
     * This method lets the user add new journals.
     */
    private void addJournal() {
        HandleException.for_(this, () -> {
            EditJournalView view = (EditJournalView) viewFactory.createView(EditJournalView.class);
            view.setTitle("ajd.title");
            ViewDialog dialog = new ViewDialog(this, view);
            dialog.showDialog();
        });
    }

    /**
     * This method lets the user delete the selected journal.
     */
    private void deleteJournal() {
        int row = Tables.getSelectedRowConvertedToModel(journalEntriesTable);
        if (row != -1) {
            deleteJournalController.setOwner(this);
            deleteJournalController.setJournalEntryToBeDeleted(journalEntriesTableModel.getRow(row).journalEntry);
            try {
                deleteJournalController.execute();
            } catch (ServiceException e) {
                MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            }
        }
    }

    public List<FormattedJournalEntry> getFilteredRows() throws ServiceException {
        Criterion criterion = isNullOrEmpty(searchCriterionModel.getString()) ? null : new Parser().parse(searchCriterionModel.getString());
        return ledgerService.findFormattedJournalEntries(document, criterion);
    }
}
