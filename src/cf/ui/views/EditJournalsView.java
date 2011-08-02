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
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;

import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.Journal;
import cf.ui.controllers.DeleteJournalController;
import cf.ui.controllers.EditJournalController;
import cf.ui.dialogs.ItemsTableModel;

/**
 * This class allows the user to browse the journals and to edit individual journals.
 *
 * @author Sander Kooijmans
 */
public class EditJournalsView extends View {

	private static final long serialVersionUID = 1L;

	private final static Color BACKGROUND_COLOR = new Color(255, 230, 230);

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
        setBackground(BACKGROUND_COLOR);
		WidgetFactory wf = WidgetFactory.getInstance();

		// Create table of journals
        journalsTableModel = new JournalsTableModel(database);
		journalsTable = WidgetFactory.getInstance().createSortedTable(journalsTableModel);

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

		// Let items table be updated when another row is selected in the journals table.
		listSelectionListener = new ListSelectionListenerImpl();
		journalsTable.getSelectionModel().addListSelectionListener(listSelectionListener);

		// Create button panel
		JPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.setOpaque(false);

		JButton editButton = wf.createButton("ejd.editJournal", new EditAction());
		buttonPanel.add(editButton);

		JButton addButton = wf.createButton("ejd.addJournal", new AddAction());
		buttonPanel.add(addButton);

		JButton deleteButton = wf.createButton("ejd.deleteJournal", new DeleteAction());
		buttonPanel.add(deleteButton);

		// Add components to the view.
        JPanel tablesPanel = new JPanel(new GridBagLayout());
        tablesPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(journalsTable);
        scrollPane.setBorder(wf.createTitleBorder("editJournalsView.journals"));
		tablesPanel.add(scrollPane, SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 3.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10, 10, 10));

        scrollPane = new JScrollPane(itemsTable);
        scrollPane.setBorder(wf.createTitleBorder("editJournalsView.journalItems"));
        tablesPanel.add(scrollPane, SwingUtils.createPanelGBConstraints(0, 1));

        setLayout(new BorderLayout());
		add(tablesPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        SwingUtils.selectFirstRow(journalsTable);
	}

    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("editJournalsView.title");
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
