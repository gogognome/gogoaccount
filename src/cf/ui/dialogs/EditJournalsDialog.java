/*
 * $Id: EditJournalsDialog.java,v 1.8 2007-05-21 15:55:47 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

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
import javax.swing.table.TableModel;

import nl.gogognome.swing.OkCancelDialog;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.engine.Journal;
import cf.engine.Party;
import cf.ui.components.AccountCellEditor;

/**
 * This class implements the Edit journals dialog. 
 *
 * @author Sander Kooijmans
 */
public class EditJournalsDialog extends OkCancelDialog implements TableModel
{
    /** The table containing journals. */
    private JTable journalsTable;
    
    /** The table containing journal items. */
    private JTable itemsTable;
    
    private ItemsTableModel itemsTableModel;
    
    /** Contains the journals as shown in the user interface. */
    private Vector journalsInUi = new Vector();
    
    /** Contains the <code>TableModelListener</code>s of this <code>TableModel</code>. */
    private Vector journalsTableModelListeners = new Vector();
    
    /** The parent frame of this dialog. */
    private Frame parent;
    
	/** 
	 * Creates a "Edit journals" dialog.
	 * @param parent the frame that owns this dialog. 
	 * @param date the date of the balance.
	 */
	public EditJournalsDialog( Frame parent ) 
	{
		super(parent, "ej.editJournals");
		
		this.parent = parent;
		
		WidgetFactory wf = WidgetFactory.getInstance();
		
		Journal[] journals = Database.getInstance().getJournals();
		for (int i=0; i<journals.length; i++)
		{
		    journalsInUi.addElement(journals[i]);
		}
		// Create table of journals
		// TODO: when table sorter is used, the items table show the wrong items!
		//       Therefore, the table sorter has been commented out for the time being.
//		TableSorter sorter = new TableSorter(this);		
		journalsTable = new JTable(this);
//		sorter.setTableHeader(journalsTable.getTableHeader());
		TableColumnModel columnModel = journalsTable.getColumnModel();
		journalsTable.setRowSelectionAllowed(true);
		journalsTable.setColumnSelectionAllowed(false);
		
		// Set column widths
		columnModel.getColumn(0).setPreferredWidth(100);
		columnModel.getColumn(1).setPreferredWidth(100);
		columnModel.getColumn(2).setPreferredWidth(300);
		
		// Set column renderer and editor for dates
		columnModel.getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
		    protected void setValue(Object date) {
		        super.setValue(TextResource.getInstance().formatDate("gen.dateFormat", (Date)date));
		    }
		});
		//journalsTable.setDefaultEditor(Date.class, new DateEditor());

		// Create table of items
		itemsTableModel = new ItemsTableModel();
		if (journals.length > 0)
		{
		    itemsTableModel.setJournalItems(journals[0].getItems());
		}
		itemsTable = new JTable(itemsTableModel);
		itemsTable.setRowSelectionAllowed(true);
		itemsTable.setColumnSelectionAllowed(false);

		// Set column widths
		columnModel = itemsTable.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(300);
		columnModel.getColumn(1).setPreferredWidth(100);
		columnModel.getColumn(2).setPreferredWidth(100);
		columnModel.getColumn(3).setPreferredWidth(300);
		
		// Create combo box for accounts
		columnModel.getColumn(0).setCellEditor(new AccountCellEditor(null));
		
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
		Party[] parties = Database.getInstance().getParties();
		for (int i = 0; i < parties.length; i++) 
		{
		    comboBox.addItem(parties[i].getId() + " " + parties[i].getName());
        }
		columnModel.getColumn(3).setCellEditor(new DefaultCellEditor(comboBox));
		
		// Let items table be updated when another row is selected in the journals table.
		ListSelectionModel rowSM = journalsTable.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
		    public void valueChanged(ListSelectionEvent e) {
		        //Ignore extra messages.
		        if (e.getValueIsAdjusting()) { return; }
		        
		        ListSelectionModel lsm = (ListSelectionModel)e.getSource();
		        if (!lsm.isSelectionEmpty()) {
		            updateJournalItemTable(lsm.getMinSelectionIndex());
		        }
		    }
		});
		
		// Create button panel
		JPanel buttonsPanel = new JPanel(new FlowLayout());

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
		
		// Add components to the dialog.
		JPanel panel = new JPanel(new GridLayout(1,2));
		JScrollPane scrollPane = new JScrollPane(journalsTable);
		panel.add(scrollPane);
		scrollPane = new JScrollPane(itemsTable);
		JPanel itemsPanel = new JPanel(new BorderLayout());
		itemsPanel.add(scrollPane, BorderLayout.CENTER);
		itemsPanel.add(buttonsPanel, BorderLayout.SOUTH);
		panel.add(itemsPanel);
		componentInitialized(panel);
		setResizable(true);
	}

	/**
	 * Handles the OK button.
	 */
	protected void handleOk() 
	{
	    Journal[] journalsArray = new Journal[journalsInUi.size()];
	    journalsInUi.copyInto(journalsArray);
	    Database.getInstance().setJournals(journalsArray);
		hideDialog();
	}

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() 
    {
        return 3;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() 
    {
        return journalsInUi.size();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#isCellEditable(int, int)
     */
    public boolean isCellEditable(int row, int column) 
    {
        return false;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnClass(int)
     */
    public Class getColumnClass(int column) 
    {
        return column == 0 ? java.util.Date.class : String.class;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int row, int col) 
    {
        Journal journal = (Journal)journalsInUi.elementAt(row);
        Object result = null;
        switch(col)
        {
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

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
     */
    public void setValueAt(Object value, int row, int col) 
    {
        Journal journal = (Journal)journalsInUi.elementAt(row);
        Date date = journal.getDate();
        String id = journal.getId();
        String description = journal.getDescription();
        switch(col)
        {
        case 0:
            date = (Date)value;
            break;
        case 1:
            id = (String)value;
            break;
        case 2:
            description = (String)value;
            break;
        }
        
        journal = new Journal(id, description, date, journal.getItems());
        journalsInUi.setElementAt(journal, row);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnName(int)
     */
    public String getColumnName(int col) 
    {
        String id = null;
        switch(col)
        {
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
    public void addTableModelListener(TableModelListener listener) 
    {
        journalsTableModelListeners.addElement(listener);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
     */
    public void removeTableModelListener(TableModelListener listener) 
    {
        journalsTableModelListeners.remove(listener);
    }
    
    private void handleEditItem()
    {
        int row = journalsTable.getSelectedRow();
        if (row != -1)
        {
            Journal journal = (Journal)journalsInUi.elementAt(row);
            EditJournalDialog dialog = 
                new EditJournalDialog(parent, "ejd.editJournal", journal, false);
            dialog.showDialog();
            Journal[] journals = dialog.getEditedJournals();
            if (journals.length > 0) {
                if (journals.length != 1) {
                    throw new IllegalStateException("journals.length must be 1");
                }
                journalsInUi.setElementAt(journals[0], row);
                notifyListeners(new TableModelEvent(this));
                updateJournalItemTable(row);
            }
        }
    }
    
    private void handleAddItem()
    {
        EditJournalDialog dialog = new EditJournalDialog(parent, "ajd.title", true);
        dialog.showDialog();
        Journal[] journals = dialog.getEditedJournals();
        for (int i = 0; i < journals.length; i++) {
            journalsInUi.addElement(journals[i]);
        }
        if (journals.length > 0) {
            notifyListeners(new TableModelEvent(this));
        }
    }

    private void handleDeleteItem()
    {
        int row = journalsTable.getSelectedRow();
        if (row != -1)
        {
            // TODO: add "are you sure?" dialog.
            journalsInUi.removeElementAt(row);
            notifyListeners(new TableModelEvent(this));
        }
    }
    
    /**
     * Notifies the listeners of a change in the table.
     * @param event describes the change.
     */
    private void notifyListeners(TableModelEvent event)
    {
        for (Enumeration enum = journalsTableModelListeners.elements(); enum.hasMoreElements();)
        {
            TableModelListener listener = (TableModelListener)enum.nextElement();
            listener.tableChanged(event);
        }
    }

    /**
     * Updates the journal item table so that it shows the items for the
     * specified journal. 
     * @param row the row of the journal whose items should be shown.
     */
    private void updateJournalItemTable(int row)
    {
        itemsTableModel.setJournalItems(
                ((Journal)journalsInUi.elementAt(row)).getItems());
    }
}
