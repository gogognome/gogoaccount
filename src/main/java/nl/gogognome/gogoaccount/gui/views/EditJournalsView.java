package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentListener;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.gui.controllers.DeleteJournalController;
import nl.gogognome.gogoaccount.gui.controllers.EditJournalController;
import nl.gogognome.gogoaccount.gui.dialogs.JournalEntryDetailsTableModel;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.action.Actions;
import nl.gogognome.lib.swing.models.Tables;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * This class allows the user to browse the journals and to edit individual journals.
 */
public class EditJournalsView extends View {

	private static final long serialVersionUID = 1L;

    private Logger logger = LoggerFactory.getLogger(EditJournalsView.class);

    private JTable itemsTable;
    private JournalEntryDetailsTableModel itemsTableModel;

    private JournalsTableModel journalsTableModel;
    private JTable journalsTable;

    private Document document;
    private final LedgerService ledgerService = ObjectFactory.create(LedgerService.class);

	/**
	 * @param document the database whose journals are being edited
	 */
	public EditJournalsView(Document document) {
        this.document = document;
    }

    @Override
    public void onInit() {
        try {
            journalsTableModel = new JournalsTableModel(document);
            journalsTable = widgetFactory.createSortedTable(journalsTableModel);

            // Create table of items
            itemsTableModel = new JournalEntryDetailsTableModel(document);
            itemsTable = widgetFactory.createTable(itemsTableModel);
            itemsTable.setRowSelectionAllowed(false);
            itemsTable.setColumnSelectionAllowed(false);

            Tables.onSelectionChange(journalsTable, () -> {
                int row = SwingUtils.getSelectedRowConvertedToModel(journalsTable);
                if (row != -1) {
                    updateJournalItemTable(row);
                }
            });

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            add(buildPanelWithTables(), BorderLayout.CENTER);
            add(createButtonPanel(), BorderLayout.SOUTH);

            SwingUtils.selectFirstRow(journalsTable);
        } catch (ServiceException e) {
            logger.warn("Ignored exception: " + e.getMessage(), e);
            close();
        }
	}

    private JPanel buildPanelWithTables() {
        JPanel tablesPanel = new JPanel(new GridBagLayout());
        tablesPanel.setOpaque(false);

        JScrollPane scrollPane = widgetFactory.createScrollPane(journalsTable, "editJournalsView.journals");
        tablesPanel.add(scrollPane, SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 3.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, 0, 0, 0, 0));

        scrollPane = widgetFactory.createScrollPane(itemsTable, "editJournalsView.journalItems");
        tablesPanel.add(scrollPane, SwingUtils.createPanelGBConstraints(0, 1));
        return tablesPanel;
    }

    private ButtonPanel createButtonPanel() {
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.setOpaque(false);

        Action editAction = Actions.build(this, this::editJournal);
        addCloseable(Tables.onSelectionChange(journalsTable, () -> editAction.setEnabled(journalsTable.getSelectedRowCount() == 1)));
        buttonPanel.addButton("ejd.editJournal", editAction);

        buttonPanel.addButton("ejd.addJournal", this::addJournal);

        Action deleteAction = Actions.build(this, this::deleteJournal);
        addCloseable(Tables.onSelectionChange(journalsTable, () -> deleteAction.setEnabled(journalsTable.getSelectedRowCount() > 0)));
        buttonPanel.addButton("ejd.deleteJournal", deleteAction);
        return buttonPanel;
    }

    @Override
    public String getTitle() {
        return textResource.getString("editJournalsView.title");
    }

    @Override
    public void onClose() {
    }

    /**
     * Updates the journal item table so that it shows the items for the
     * specified journal.
     * @param row the row of the journal whose items should be shown.
     */
    private void updateJournalItemTable(int row) throws ServiceException {
        itemsTableModel.setJournalEntryDetails(ledgerService.findJournalEntryDetails(document, journalsTableModel.getRow(row)));
    }

    /**
     * This method lets the user edit the selected journal.
     */
    private void editJournal() {
        try {
            int row = SwingUtils.getSelectedRowConvertedToModel(journalsTable);
            if (row != -1) {
                JournalEntry journalEntry = journalsTableModel.getRow(row);
                EditJournalController controller = new EditJournalController(this, document, journalEntry);
                controller.execute();
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
    	DocumentListener documentListener = new DocumentListenerImpl();
    	try {
            HandleException.for_(this, () -> {
                document.addListener(documentListener);
                EditJournalView view = new EditJournalView(document, "ajd.title", null, null);
                ViewDialog dialog = new ViewDialog(this, view);
                dialog.showDialog();
            });
	    } finally {
    		document.removeListener(documentListener);
	    }
    }

    /**
     * This method lets the user delete the selected journal.
     */
    private void deleteJournal() {
        int row = SwingUtils.getSelectedRowConvertedToModel(journalsTable);
        if (row != -1) {
        	DeleteJournalController controller =
        		new DeleteJournalController(this, document, journalsTableModel.getRow(row));
            try {
                controller.execute();
            } catch (ServiceException e) {
                MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            }
        }
    }

    /**
     * This method is called when the focus is requested for this view.
     */
    @Override
    public void requestFocus() {
        journalsTable.requestFocus();
    }

    /**
     * This method is called when the focus is requested for this view.
     */
    @Override
    public boolean requestFocusInWindow() {
        return journalsTable.requestFocusInWindow();
    }

	private class DocumentListenerImpl implements DocumentListener {
		@Override
		public void documentChanged(Document document) {
            try {
                journalsTableModel.replaceRows(ledgerService.findJournalEntries(document));
            } catch (ServiceException e) {
                logger.warn("Ignored exception: " + e.getMessage(), e);
            }
        }
	}
}
