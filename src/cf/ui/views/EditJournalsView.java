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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.swing.SortedTable;
import nl.gogognome.lib.swing.SortedTableModel;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.Invoice;
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

    /** The table containing journals. */
    private SortedTable journalsTable;

    /** The table containing journal items. */
    private JTable itemsTable;

    /** The table mdoel for the journal items. */
    private ItemsTableModel itemsTableModel;

    /** The table model for the journals. */
    private JournalsTableModel journalsTableModel;

    /** The database. */
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
        journalsTable.setTitle("editJournalsView.journals");

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
		final ListSelectionModel rowSM = journalsTable.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
		    @Override
			public void valueChanged(ListSelectionEvent e) {
		        //Ignore extra messages.
		        if (e.getValueIsAdjusting()) { return; }

		        if (!rowSM.isSelectionEmpty()) {
		            updateJournalItemTable(rowSM.getMinSelectionIndex());
		        }
		    }
		});

		// Create button panel
		JPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.setOpaque(false);

		JButton editButton = wf.createButton("ejd.editJournal", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                handleEditItem();
            }
		});
		buttonPanel.add(editButton);

		JButton addButton = wf.createButton("ejd.addJournal", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                handleAddItem();
            }
		});
		buttonPanel.add(addButton);

		JButton deleteButton = wf.createButton("ejd.deleteJournal", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                handleDeleteItem();
            }
		});
		buttonPanel.add(deleteButton);

		// Add components to the view.
        JPanel tablesPanel = new JPanel(new GridBagLayout());
        tablesPanel.setOpaque(false);
		tablesPanel.add(journalsTable.getComponent(),
            SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 3.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(itemsTable);
        tablesPanel.add(scrollPane, SwingUtils.createPanelGBConstraints(0, 1));

        setLayout(new BorderLayout());
		add(tablesPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        journalsTable.selectFirstRow();
	}

    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("editJournalsView.title");
    }

    @Override
    public void onClose() {
        journalsTable = null;
        journalsTableModel.dispose();
    }

    /**
     * Updates the journal item table so that it shows the items for the
     * specified journal.
     * @param row the row of the journal whose items should be shown.
     */
    private void updateJournalItemTable(int row) {
        itemsTableModel.setJournalItems(journalsTableModel.getJournal(row).getItems());
    }

    /**
     * This method lets the user edit the selected journal.
     */
    private void handleEditItem() {
        int row = journalsTable.getSelectionModel().getMinSelectionIndex();
        if (row != -1) {
            Journal journal = journalsTableModel.getJournal(row);
            EditJournalController controller = new EditJournalController(this, database, journal);
            controller.execute();
            updateJournalItemTable(row);
        }
    }

    /**
     * This method lets the user add new journals.
     */
    private void handleAddItem() {
        EditJournalView view = new EditJournalView(database, "ajd.title", null);
        ViewDialog dialog = new ViewDialog(this, view);
        dialog.showDialog();
    }

    /**
     * This method lets the user delete the selected journal.
     */
    private void handleDeleteItem() {
        int row = journalsTable.getSelectionModel().getMinSelectionIndex();
        if (row != -1) {
        	DeleteJournalController controller =
        		new DeleteJournalController(this, database, journalsTableModel.getJournal(row));
        	controller.execute();
        }
    }

    /**
     * This class implements a table model for a table containing journals.
     */
    private class JournalsTableModel implements SortedTableModel, DatabaseListener {

        /** Contains the <code>TableModelListener</code>s of this <code>TableModel</code>. */
        private ArrayList<TableModelListener> journalsTableModelListeners = new ArrayList<TableModelListener>();

        private List<Journal> journals;

        private Database database;

        /**
         * Constructor.
         * @param database the database from which to take the data
         */
        public JournalsTableModel(Database database) {
            this.database = database;
            this.journals = database.getJournals();
            database.addListener(this);
        }

        /**
         * Disposes this table model. After this method has been called, this table model
         * should not be used anymore.
         */
        public void dispose() {
            database.removeListener(this);
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        @Override
		public int getColumnCount() {
            return 4;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        @Override
		public int getRowCount() {
            return journals.size();
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#isCellEditable(int, int)
         */
        @Override
		public boolean isCellEditable(int row, int column) {
            return false;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnClass(int)
         */
        @Override
		public Class<?> getColumnClass(int column) {
            return column == 0 ? java.util.Date.class : String.class;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        @Override
		public Object getValueAt(int row, int col) {
            Journal journal = journals.get(row);
            Object result = null;
            switch(col) {
            case 0:
                result = journal.getDate();
                break;
            case 1:
                result = journal.getId();
                break;
            case 2:
                result = journal.getDescription();
                break;
            case 3:
                String id = journal.getIdOfCreatedInvoice();
                if (id != null) {
                    Invoice invoice = database.getInvoice(id);
                    result = invoice.getId() + " (" + invoice.getConcerningParty().getName() + ")";
                }
            }
            return result;
        }

        /**
         * @see javax.swing.table.TableModel#setValueAt(Object, int, int)
         */
        @Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnName(int)
         */
        @Override
		public String getColumnName(int col) {
            String id = null;
            switch(col) {
            case 0:
                id = "gen.date";
                break;
            case 1:
                id = "gen.id";
                break;
            case 2:
                id = "gen.description";
                break;
            case 3:
                id = "gen.invoice";
            }
            return TextResource.getInstance().getString(id);
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
         */
        @Override
		public void addTableModelListener(TableModelListener listener) {
            journalsTableModelListeners.add(listener);
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
         */
        @Override
		public void removeTableModelListener(TableModelListener listener) {
            journalsTableModelListeners.remove(listener);
        }

        /**
         * Notifies the listeners of a change in the table.
         * @param event describes the change.
         */
        private void notifyListeners(TableModelEvent event) {
            for (Iterator<TableModelListener> iter = journalsTableModelListeners.iterator(); iter.hasNext();) {
                TableModelListener listener = iter.next();
                listener.tableChanged(event);
            }
        }

        @Override
		public int getColumnWidth(int column) {
            switch (column) {
            case 0:
            case 1:
                return 200;

            case 2:
                return 500;

            case 3:
                return 200;
            default:
                throw new IllegalStateException("No width specified for column " + column);
            }
        }

        @Override
		public TableCellRenderer getRendererForColumn(int column) {
            switch (column) {
            case 0:
                return new DefaultTableCellRenderer() {
                    @Override
                    protected void setValue(Object date) {
                        super.setValue(TextResource.getInstance().formatDate("gen.dateFormat", (Date)date));
                    }
                };
            default:
                return null;
            }
        }

        @Override
        public TableCellEditor getEditorForColumn(int column) {
        	return null;
        }

        @Override
		public Comparator<Object> getComparator(int column) {
            return null;
        }

        /**
         * @see DatabaseListener#databaseChanged(Database)
         */
        @Override
		public void databaseChanged(Database db) {
            journals = database.getJournals();
            notifyListeners(new TableModelEvent(this));
        }

        /**
         * Gets the journal at the specified row.
         * @param row the row
         * @return the journal
         */
        public Journal getJournal(int row) {
            return journals.get(row);
        }

    }

    /**
     * This method is called when the focus is requested for this view.
     */
    @Override
    public void requestFocus() {
        journalsTable.getFocusableComponent().requestFocus();
    }

    /**
     * This method is called when the focus is requested for this view.
     */
    @Override
    public boolean requestFocusInWindow() {
        return journalsTable.getFocusableComponent().requestFocusInWindow();
    }
}
