/*
 * $Id: EditJournalDialog.java,v 1.11 2007-05-21 15:55:27 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.OkCancelDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Journal;
import cf.engine.JournalItem;

/**
 * This class implements the dialog for editing a journal.
 * 
 * @author Sander Kooijmans
 */
public class EditJournalDialog extends OkCancelDialog
{
    /** The text field of the id. */
    private JTextField tfId;

    /** The text field of the date. */
    private JTextField tfDate;

    /** The text field of the description. */
    private JTextField tfDescription;

    /** The table containing journal items. */
    private JTable itemsTable;

    private ItemsTableModel itemsTableModel;

    /** The parent frame of this dialog. */
    private Frame parent;

    /** The format used for the dates shown in this dialog. */
    private SimpleDateFormat dateFormat;

    /**
     * Contains the journals as edited by the user.
     */
    private Collection enteredJournals = new LinkedList();
    
    /** Indicates whether the Ok + next button has to be shown. */ 
    private boolean showNextButton;
    
    /** Template array used by <code>getEditedJournals()</code>. */
    private final static Journal[] TEMPLATE = new Journal[0];
    
    /**
     * Constructor.
     * 
     * @param parent the paren frame
     * @param id the id of the title
     */
    public EditJournalDialog(Frame parent, String id, boolean showNextButton) 
    {
        super(parent, id);
        this.parent = parent;
        this.showNextButton = showNextButton;
        initializeDialog("", new Date(), "", new JournalItem[0]);
    }

    /**
     * Constructor.
     * 
     * @param parent the paren frame
     * @param id the id of the title
     * @param journal the journal used to initialize the elements of the dialog
     */
    public EditJournalDialog(Frame parent, String id, Journal journal, boolean showNextButton) 
    {
        super(parent, id);
        this.parent = parent;
        this.showNextButton = showNextButton;
        initializeDialog(journal.getId(), journal.getDate(), journal.getDescription(),
                journal.getItems());
    }
    
    /**
     * Initializes the dialog with the specified journal.
     * 
     * @param id the id of the journal.
     * @param date the date of the journal.
     * @param description the description of the journal.
     * @param items the items of the journal.
     */
    private void initializeDialog(String id, Date date, String description, JournalItem[] items)
    {
        TextResource tr = TextResource.getInstance();
        WidgetFactory wf = WidgetFactory.getInstance();
        dateFormat = new SimpleDateFormat(tr.getString("gen.dateFormat"));

        // Create panel with ID, date and description.
        GridBagLayout gbl = new GridBagLayout();
        JPanel topPanel = new JPanel(gbl);

        JLabel label = wf.createLabel("gen.id");
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, 0));
        
        tfId = wf.createTextField(id);
        topPanel.add(tfId, SwingUtils.createTextFieldGBConstraints(1, 0));

        label = wf.createLabel("gen.date");
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, 1));
        
        tfDate = wf.createTextField(dateFormat.format(date));
        topPanel.add(tfDate, SwingUtils.createTextFieldGBConstraints(1, 1));

        label = wf.createLabel("gen.description");
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, 2));
        
        tfDescription = wf.createTextField(description);
        topPanel.add(tfDescription, SwingUtils.createTextFieldGBConstraints(1, 2));

        // Create table of items
        itemsTableModel = new ItemsTableModel();
        itemsTableModel.setJournalItems(new JournalItem[0]);
        itemsTable = new JTable(itemsTableModel);
        itemsTable.setRowSelectionAllowed(true);
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

        // Add buttons
        JButton addButton = wf.createButton("ajd.addItem", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                handleAddButtonPressed(evt);
            }
        });

        JButton editButton = wf.createButton("ajd.editItem", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                handleEditButtonPressed(evt);
            }
        });
        
        JButton deleteButton = wf.createButton("ajd.deleteItem", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                handleDeleteButtonPressed(evt);
            }
        });

        JButton okAndNextButton = wf.createButton("ajd.okAndNextJournal", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                handleOkAndNextButtonPressed();
            }
        });
        
        gbl = new GridBagLayout();
        JPanel buttonPanel = new JPanel(gbl);
        buttonPanel.add(addButton,
                SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 0.0, 
                        GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, 
                        10, 10, 0, 0));
        buttonPanel.add(editButton,
                SwingUtils.createGBConstraints(0, 1, 1, 1, 1.0, 0.0, 
                        GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, 
                        10, 10, 0, 0));
        buttonPanel.add(deleteButton,
                SwingUtils.createGBConstraints(0, 2, 1, 1, 1.0, 0.0, 
                        GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, 
                        10, 10, 0, 0));

        if (showNextButton) {
	        buttonPanel.add(okAndNextButton,
	                SwingUtils.createGBConstraints(0, 3, 1, 1, 1.0, 0.0, 
	                        GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, 
	                        10, 10, 0, 0));
        }
        
        JScrollPane scrollableTable = new JScrollPane(itemsTable);
        
        gbl = new GridBagLayout();
        JPanel panel = new JPanel(gbl);
        panel.add(topPanel, SwingUtils.createGBConstraints(0, 0, 2, 1));
        panel.add(scrollableTable,
                SwingUtils.createGBConstraints(0, 1, 1, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.BOTH, 
                        10, 0, 0, 0));
        panel.add(buttonPanel,
                SwingUtils.createGBConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
                        0, 0, 0, 0));

        for (int i=0; i<items.length; i++)
        {
            itemsTableModel.addItem(items[i]);
        }
        
        componentInitialized(panel);
    }

	/**
	 * Handles the cancel event. In this particular implementation, this method is
	 * called when the dialog is closed not by pressing one of the buttons.
	 */	
	protected void handleCancel() {
	    enteredJournals.clear(); // remove all entred journals
		hideDialog();
	}
    
    /*
     * (non-Javadoc)
     * 
     * @see cf.ui.OkCancelDialog#handleOk()
     */
    protected void handleOk() {
        Journal journal = getJournalFromDialog();
        if (journal != null) {
            enteredJournals.add(journal);
            hideDialog();
        }
    }

    /** Handles the Ok + next button. */
    private void handleOkAndNextButtonPressed() {
        Journal journal = getJournalFromDialog();
        if (journal != null) {
            enteredJournals.add(journal);
            
            itemsTableModel.clear();
            tfId.requestFocus();
        }
    }
    
    /**
     * Gets the journal from the values filled in the dialog.
     * @return the journal or <code>null</code> if the values are not valid, 
     *          in which case the user has been notified about the problem with
     *          the input values.
     */
    private Journal getJournalFromDialog() {
        TextResource tr = TextResource.getInstance();
        Date date = null;
        try 
        {
            date = dateFormat.parse(tfDate.getText());
        } 
        catch (ParseException e) 
        {
            MessageDialog.showMessage(parent, "gen.titleError", 
                    tr.getString("gen.invalidDate"));
            return null;
        }

        String id = tfId.getText();
        String description = tfDescription.getText();
        JournalItem[] items = itemsTableModel.getItems();

        try 
        {
            return new Journal(id, description, date, items);
        } 
        catch (IllegalArgumentException e) 
        {
            new MessageDialog(parent, "gen.titleError", 
                    tr.getString("gen.itemsNotInBalance"));
            return null;
        }
    }
    
    private void handleAddButtonPressed(ActionEvent e) 
    {
        EditJournalItemDialog dialog = new EditJournalItemDialog(parent, "ajd.addJournalItem");
        dialog.showDialog();
        JournalItem item = dialog.getEnteredJournalItem();
        if (item != null)
        {
            itemsTableModel.addItem(item);
        }
    }

    private void handleEditButtonPressed(ActionEvent e)
    {
        int row = itemsTable.getSelectedRow();
        JournalItem item = itemsTableModel.getItem(row);
        if (item != null)
        {
	        EditJournalItemDialog dialog = 
	            new EditJournalItemDialog(parent, "ajd.editJournalItem", item);
	        dialog.showDialog();
	        item = dialog.getEnteredJournalItem();
	        if (item != null)
	        {
	            itemsTableModel.updateItem(row, item);
	        }
        }
    }
    
    private void handleDeleteButtonPressed(ActionEvent e) 
    {
        itemsTableModel.deleteItem(itemsTable.getSelectedRow());
    }

    /**
     * Gets the journals entered by the user.
     * @return the journals
     */
    public Journal[] getEditedJournals() {
        return (Journal[])enteredJournals.toArray(TEMPLATE);
    }
}