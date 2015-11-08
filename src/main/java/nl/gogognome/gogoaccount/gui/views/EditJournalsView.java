package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentListener;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.gui.controllers.DeleteJournalController;
import nl.gogognome.gogoaccount.gui.controllers.EditJournalController;
import nl.gogognome.gogoaccount.gui.dialogs.JournalEntryDetailsTableModel;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;

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
    private ListSelectionListener listSelectionListener;

    private Document document;
    private final LedgerService ledgerService = ObjectFactory.create(LedgerService.class);

	/**
	 * Creates the "Edit journals" view.
	 * @param document the database whose journals are being edited
	 */
	public EditJournalsView(Document document) {
		super();
        this.document = document;
    }

    /**
     * @see View#onInit()
     */
    @Override
    public void onInit() {
        // Create table of journals
        try {
            journalsTableModel = new JournalsTableModel(document);
            journalsTable = widgetFactory.createSortedTable(journalsTableModel);

            // Create table of items
            itemsTableModel = new JournalEntryDetailsTableModel(document);
            itemsTable = widgetFactory.createTable(itemsTableModel);
            itemsTable.setRowSelectionAllowed(false);
            itemsTable.setColumnSelectionAllowed(false);

            // Let items table be updated when another row is selected in the journals table.
            listSelectionListener = new ListSelectionListenerImpl();
            journalsTable.getSelectionModel().addListSelectionListener(listSelectionListener);

            // Create button panel
            JPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
            buttonPanel.setOpaque(false);

            JButton editButton = widgetFactory.createButton("ejd.editJournal", new EditAction());
            buttonPanel.add(editButton);

            JButton addButton = widgetFactory.createButton("ejd.addJournal", new AddAction());
            buttonPanel.add(addButton);

            JButton deleteButton = widgetFactory.createButton("ejd.deleteJournal", new DeleteAction());
            buttonPanel.add(deleteButton);

            // Add components to the view.
            JPanel tablesPanel = new JPanel(new GridBagLayout());
            tablesPanel.setOpaque(false);

            JScrollPane scrollPane = widgetFactory.createScrollPane(journalsTable, "editJournalsView.journals");
            tablesPanel.add(scrollPane, SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 3.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, 0, 0, 0, 0));

            scrollPane = widgetFactory.createScrollPane(itemsTable, "editJournalsView.journalItems");
            tablesPanel.add(scrollPane, SwingUtils.createPanelGBConstraints(0, 1));

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            add(tablesPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);

            SwingUtils.selectFirstRow(journalsTable);
        } catch (ServiceException e) {
            logger.warn("Ignored exception: " + e.getMessage(), e);
            close();
        }
	}

    @Override
    public String getTitle() {
        return textResource.getString("editJournalsView.title");
    }

    @Override
    public void onClose() {
		journalsTable.getSelectionModel().removeListSelectionListener(listSelectionListener);
        journalsTable = null;
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
    		document.addListener(documentListener);
	        EditJournalView view = new EditJournalView(document, "ajd.title", null, null);
	        ViewDialog dialog = new ViewDialog(this, view);
	        dialog.showDialog();
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

	private class EditAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    editJournal();
		}
	}

	private class AddAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    addJournal();
		}
	}

    private class DeleteAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    deleteJournal();
		}
	}

	private class ListSelectionListenerImpl implements ListSelectionListener {
	    @Override
		public void valueChanged(ListSelectionEvent event) {
	        //Ignore extra messages.
	        if (event.getValueIsAdjusting()) { return; }

	        int row = SwingUtils.getSelectedRowConvertedToModel(journalsTable);
	        if (row != -1) {
                try {
                    updateJournalItemTable(row);
                } catch (ServiceException e) {
                    logger.warn("Ignored exception: " + e.getMessage(), e);
                }
            }
	    }
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
