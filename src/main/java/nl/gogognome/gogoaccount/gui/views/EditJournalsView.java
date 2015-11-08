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
package nl.gogognome.gogoaccount.gui.views;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.database.DatabaseListener;
import nl.gogognome.gogoaccount.gui.controllers.DeleteJournalController;
import nl.gogognome.gogoaccount.gui.controllers.EditJournalController;
import nl.gogognome.gogoaccount.gui.dialogs.ItemsTableModel;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;

/**
 * This class allows the user to browse the journals and to edit individual journals.
 *
 * @author Sander Kooijmans
 */
public class EditJournalsView extends View {

	private static final long serialVersionUID = 1L;

    private JTable itemsTable;
    private ItemsTableModel itemsTableModel;

    private JournalsTableModel journalsTableModel;
    private JTable journalsTable;
    private ListSelectionListener listSelectionListener;

    private Database database;

	/**
	 * Creates the "Edit journals" view.
	 * @param database the database whose journals are being edited
	 */
	public EditJournalsView(Database database) {
		super();
        this.database = database;
    }

    /**
     * @see View#onInit()
     */
    @Override
    public void onInit() {
        // Create table of journals
        journalsTableModel = new JournalsTableModel(database);
		journalsTable = widgetFactory.createSortedTable(journalsTableModel);

		// Create table of items
		itemsTableModel = new ItemsTableModel(database);
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
    private void updateJournalItemTable(int row) {
        itemsTableModel.setJournalItems(journalsTableModel.getRow(row).getItems());
    }

    /**
     * This method lets the user edit the selected journal.
     */
    private void editJournal() {
        int row = SwingUtils.getSelectedRowConvertedToModel(journalsTable);
        if (row != -1) {
            Journal journal = journalsTableModel.getRow(row);
            EditJournalController controller = new EditJournalController(this, database, journal);
            controller.execute();
            updateJournalItemTable(row);
        }
    }

    /**
     * This method lets the user add new journals.
     */
    private void addJournal() {
    	DatabaseListener databaseListener = new DatabaseListenerImpl();
    	try {
    		database.addListener(databaseListener);
	        EditJournalView view = new EditJournalView(database, "ajd.title", null);
	        ViewDialog dialog = new ViewDialog(this, view);
	        dialog.showDialog();
	    } finally {
    		database.removeListener(databaseListener);
	    }
    }

    /**
     * This method lets the user delete the selected journal.
     */
    private void deleteJournal() {
        int row = SwingUtils.getSelectedRowConvertedToModel(journalsTable);
        if (row != -1) {
        	DeleteJournalController controller =
        		new DeleteJournalController(this, database, journalsTableModel.getRow(row));
        	controller.execute();
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
		public void valueChanged(ListSelectionEvent e) {
	        //Ignore extra messages.
	        if (e.getValueIsAdjusting()) { return; }

	        int row = SwingUtils.getSelectedRowConvertedToModel(journalsTable);
	        if (row != -1) {
	            updateJournalItemTable(row);
	        }
	    }
    }

	private class DatabaseListenerImpl implements DatabaseListener {
		@Override
		public void databaseChanged(Database db) {
			journalsTableModel.replaceRows(db.getJournals());
		}
	}
}
