/*
 * $Id: EditJournalsView.java,v 1.2 2008-03-16 16:30:34 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import nl.gogognome.framework.View;
import nl.gogognome.swing.ButtonPanel;
import nl.gogognome.swing.SortedTable;
import nl.gogognome.swing.SortedTableModel;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.Journal;
import cf.engine.Party;
import cf.ui.dialogs.EditJournalDialog;
import cf.ui.dialogs.ItemsTableModel;

/**
 * This class implements the Edit journals view. 
 *
 * @author Sander Kooijmans
 */
public class EditJournalsView extends View {
    
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
    public void onInit() {
		WidgetFactory wf = WidgetFactory.getInstance();
		
		// Create table of journals
        journalsTableModel = new JournalsTableModel(database);
		journalsTable = WidgetFactory.getInstance().createSortedTable(journalsTableModel);
        journalsTable.setTitle(TextResource.getInstance().getString("editJournalsView.journals"));
		
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
		tablesPanel.add(journalsTable.getComponent(), 
            SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 3.0, 
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(itemsTable);
        tablesPanel.add(scrollPane, SwingUtils.createPanelGBConstraints(0, 1));
        
        setLayout(new BorderLayout());
		add(tablesPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

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
            EditJournalDialog dialog = 
                new EditJournalDialog(getParentFrame(), database, "ejd.editJournal", journal, false);
            dialog.showDialog();
            Journal[] journals = dialog.getEditedJournals();
            if (journals.length > 0) {
                if (journals.length != 1) {
                    throw new IllegalStateException("journals.length must be 1");
                }
                database.updateJournal(journal, journals[0]);
                updateJournalItemTable(row);
            }
        }
    }
    
    /**
     * This method lets the user add new journals.
     */
    private void handleAddItem() {
        EditJournalDialog dialog = new EditJournalDialog(getParentFrame(), database, "ajd.title", true);
        dialog.showDialog();
        Journal[] journals = dialog.getEditedJournals();
        for (int i = 0; i < journals.length; i++) {
            database.addJournal(journals[i], true);
        }
    }

    /**
     * This method lets the user delete the selected journal.
     */
    private void handleDeleteItem() {
        int row = journalsTable.getSelectionModel().getMinSelectionIndex();
        if (row != -1) {
            // TODO: add "are you sure?" dialog.
            database.removeJournal(journalsTableModel.getJournal(row));
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
        public int getColumnCount() {
            return 3;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            return journals.size();
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#isCellEditable(int, int)
         */
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnClass(int)
         */
        public Class<?> getColumnClass(int column) {
            return column == 0 ? java.util.Date.class : String.class;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
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
            }
            return result;
        }

        /**
         * @see javax.swing.table.TableModel#setValueAt(Object, int, int)
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }
        
        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnName(int)
         */
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
            }
            return TextResource.getInstance().getString(id);
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
         */
        public void addTableModelListener(TableModelListener listener) {
            journalsTableModelListeners.add(listener);
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
         */
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
    
        public int getColumnWidth(int column) {
            switch (column) {
            case 0:
            case 1:
                return 200;
                
            case 2:
                return 500;
                
            default:
                throw new IllegalStateException("No width specified for column " + column);
            }
        }
    
        public TableCellRenderer getRendererForColumn(int column) {
            switch (column) {
            case 0:
                return new DefaultTableCellRenderer() {
                    protected void setValue(Object date) {
                        super.setValue(TextResource.getInstance().formatDate("gen.dateFormat", (Date)date));
                    }
                };
            default:
                return null;
            }
        }

        /**
         * @see DatabaseListener#databaseChanged(Database)
         */
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
    public void requestFocus() {
        journalsTable.getFocusableComponent().requestFocus();
    }

    /**
     * This method is called when the focus is requested for this view.
     */
    public boolean requestFocusInWindow() {
        return journalsTable.getFocusableComponent().requestFocusInWindow();
    }
}
