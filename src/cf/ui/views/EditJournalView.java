/*
 * $Id: EditJournalView.java,v 1.4 2010-03-04 21:14:21 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.Amount;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.ui.dialogs.EditJournalItemDialog;
import cf.ui.dialogs.ItemsTableModel;

/**
 * This class implements the dialog for editing a single journal.
 *
 * @author Sander Kooijmans
 */
public class EditJournalView extends View {

    /** The database. */
    private Database database;

    /** The id of the title. */
    private String titleId;

    /**
     * The journal used to initialize the view. <code>null</code> indicates that a new journal
     * is to be edited.
     */
    private Journal initialJournal;

    /**
     * The id of the invoice that is created by the edited journal. If <code>null</code>, then
     * no invoice is created by this journal.
     */
    private String idOfCreatedInvoice;

    /** The text field of the id. */
    private JTextField tfId;

    /** The text field of the date. */
    private DateSelectionBean sbDate;

    /** The text field of the description. */
    private JTextField tfDescription;

    /** The table containing journal items. */
    private JTable itemsTable;

    private ItemsTableModel itemsTableModel;

    /** The date model used to edit the date. */
    private DateModel dateModel;

    /** The journal edited by the journal. This will only be filled when the user is editing a journal. */
    private Journal editedJournal;

    /**
     * Constructor. To edit an existing journal, give <code>journal</code> a non-<code>null</code> value.
     * To add one or more new journals, set <code>journal</code> to <code>null</code>.
     *
     * @param database the database to which the journal must be added
     * @param titleId the id of the title
     * @param journal the journal used to initialize the elements of the view. Must be <code>null</code>
     *        to edit a new journal
     */
    public EditJournalView(Database database, String titleId, Journal journal) {
        this.database = database;
        this.titleId = titleId;
        this.initialJournal = journal;
    }

    @Override
    public void onInit() {
        if (initialJournal == null) {
            initializeDialog("", new Date(), "", new ArrayList<JournalItem>());
        } else {
            idOfCreatedInvoice = initialJournal.getIdOfCreatedInvoice();
            initializeDialog(initialJournal.getId(), initialJournal.getDate(),
                initialJournal.getDescription(), Arrays.asList(initialJournal.getItems()));
        }
    }

    /**
     * Initializes the dialog with the specified journal.
     *
     * @param id the id of the journal.
     * @param date the date of the journal.
     * @param description the description of the journal.
     * @param items the items of the journal.
     */
    private void initializeDialog(String id, Date date, String description, List<JournalItem> items) {
        WidgetFactory wf = WidgetFactory.getInstance();

        // Create panel with ID, date and description.
        GridBagLayout gbl = new GridBagLayout();
        JPanel topPanel = new JPanel(gbl);

        JLabel label = wf.createLabel("gen.id");
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, 0));

        tfId = wf.createTextField(id);
        topPanel.add(tfId, SwingUtils.createTextFieldGBConstraints(1, 0));

        label = wf.createLabel("gen.date");
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, 1));

        dateModel = new DateModel();
        dateModel.setDate(date, null);
        sbDate = new DateSelectionBean(dateModel);
        topPanel.add(sbDate, SwingUtils.createTextFieldGBConstraints(1, 1));

        label = wf.createLabel("gen.description");
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, 2));

        tfDescription = wf.createTextField(description);
        topPanel.add(tfDescription, SwingUtils.createTextFieldGBConstraints(1, 2));

        // Create table of items
        itemsTableModel = new ItemsTableModel(database);
        itemsTableModel.setJournalItems(new JournalItem[0]);
        itemsTable = new JTable(itemsTableModel);
        itemsTable.setRowSelectionAllowed(true);
        itemsTable.setColumnSelectionAllowed(false);

        // Set column widths
        TableColumnModel columnModel = itemsTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(300);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(500);

        // Set renderers for column 1 and 2.
        TableCellRenderer rightAlignedRenderer = new DefaultTableCellRenderer() {
            @Override
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
                handleAddButtonPressed();
            }
        });

        JButton editButton = wf.createButton("ajd.editItem", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                handleEditButtonPressed();
            }
        });

        JButton deleteButton = wf.createButton("ajd.deleteItem", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                handleDeleteButtonPressed();
            }
        });

        JButton okButton = wf.createButton("gen.ok", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                handleOkButtonPressed();
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
        buttonPanel.add(okButton,
            SwingUtils.createGBConstraints(0, 3, 1, 1, 1.0, 0.0,
                    GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL,
                    20, 10, 0, 0));

        if (initialJournal == null) {
	        buttonPanel.add(okAndNextButton,
	                SwingUtils.createGBConstraints(0, 4, 1, 1, 1.0, 0.0,
	                        GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL,
	                        10, 10, 0, 0));
        }

        JScrollPane scrollableTable = new JScrollPane(itemsTable);

        gbl = new GridBagLayout();
        JPanel panel = new JPanel(gbl);
        panel.setOpaque(false);
        panel.add(topPanel, SwingUtils.createGBConstraints(0, 0, 2, 1));
        panel.add(scrollableTable,
                SwingUtils.createGBConstraints(0, 1, 1, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.BOTH,
                        10, 0, 0, 0));
        panel.add(buttonPanel,
                SwingUtils.createGBConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
                        0, 0, 0, 0));

        for (JournalItem item : items) {
            itemsTableModel.addItem(item);
        }

        setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(panel, BorderLayout.CENTER);
    }

    /** Handles the OK button. Closes the dialog. */
    private void handleOkButtonPressed() {
        Journal journal = getJournalFromDialog();
        if (journal != null) {
            if (initialJournal == null) {
                // Add the new journal to the database
                try {
                    database.addJournal(journal, true);
                } catch (DatabaseModificationFailedException e) {
                    MessageDialog.showMessage(getParentWindow(), "gen.error", e.getMessage());
                    return; // do not close the view
                }
            } else {
                // Set the edited journal
                editedJournal = journal;
            }
            closeAction.actionPerformed(null);
        }
    }

    /** Handles the Ok + next button. */
    private void handleOkAndNextButtonPressed() {
        Journal journal = getJournalFromDialog();
        if (journal != null) {
            try {
                database.addJournal(journal, true);
            } catch (DatabaseModificationFailedException e) {
                MessageDialog.showMessage(getParentWindow(), "gen.error", e.getMessage());
                return; // do not clear the table model
            }
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
        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog.showMessage(this, "gen.titleError", tr.getString("gen.invalidDate"));
            return null;
        }

        String id = tfId.getText();
        String description = tfDescription.getText();
        JournalItem[] items = itemsTableModel.getItems();

        try {
            return new Journal(id, description, date, items, idOfCreatedInvoice);
        }
        catch (IllegalArgumentException e) {
            MessageDialog.showMessage(this, "gen.titleError", tr.getString("gen.itemsNotInBalance"));
            return null;
        }
    }

    /** Handles the add button. Lets the user add a journal item. */
    private void handleAddButtonPressed() {
    	JournalItem defaultItem = null;
        if (itemsTableModel.getRowCount() > 0) {
        	// Create dialog with default values that make the journal balance.
        	Amount debetAmount = Amount.getZero(database.getCurrency());
        	for (JournalItem item : itemsTableModel.getItems()) {
        		if (item.isDebet()) {
        			debetAmount = debetAmount.add(item.getAmount());
        		} else {
        			debetAmount = debetAmount.subtract(item.getAmount());
        		}
        	}
        	boolean debet = debetAmount.isNegative();
        	defaultItem = new JournalItem(debet ? debetAmount.negate() : debetAmount,
        			database.getAllAccounts()[0], debet, null, null);
        }
        EditJournalItemDialog dialog = new EditJournalItemDialog(this, database, "ajd.addJournalItem", defaultItem);
        dialog.showDialog();
        JournalItem item = dialog.getEnteredJournalItem();
        if (item != null) {
            itemsTableModel.addItem(item);
        }
    }

    /** Handles the edit button. Lets the user edit a journal item. */
    private void handleEditButtonPressed() {
        int row = itemsTable.getSelectedRow();
        JournalItem item = itemsTableModel.getItem(row);
        if (item != null) {
	        EditJournalItemDialog dialog =
	            new EditJournalItemDialog(this, database, "ajd.editJournalItem", item);
	        dialog.showDialog();
	        item = dialog.getEnteredJournalItem();
	        if (item != null) {
	            itemsTableModel.updateItem(row, item);
	        }
        }
    }

    /** Handles the delete button. Deletes a journal item. */
    private void handleDeleteButtonPressed() {
        itemsTableModel.deleteItem(itemsTable.getSelectedRow());
    }

    /**
     * Gets the journal entered by the user. This method only returns a non-<code>null</code> value
     * if this view was initialized with a journal and the user closed the view by pressing the
     * Ok button. Otherwise, this method returns <code>null</code>.
     * @return the journal entered by the user or <code>null</code>
     */
    public Journal getEditedJournal() {
        return editedJournal;
    }

    @Override
    public String getTitle() {
        return TextResource.getInstance().getString(titleId);
    }

    @Override
    public void onClose() {
    }

}