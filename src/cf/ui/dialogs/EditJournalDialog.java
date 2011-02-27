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
package cf.ui.dialogs;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.OkCancelDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
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
    private DateSelectionBean sbDate;

    /** The text field of the description. */
    private JTextField tfDescription;

    /** The table containing journal items. */
    private JTable itemsTable;

    private ItemsTableModel itemsTableModel;

    /** The parent frame of this dialog. */
    private Frame parent;

    /** The date model used to edit the date. */
    private DateModel dateModel;

    /** If not <code>null</code>, then this is the id of the invoice created by this journal. */
    private String idOfCreatedInvoice;

    /** The database. */
    private Database database;

    /**
     * Contains the journals as edited by the user.
     */
    private List<Journal> enteredJournals = new LinkedList<Journal>();

    /** Indicates whether the Ok + next button has to be shown. */
    private boolean showNextButton;

    /**
     * Constructor.
     *
     * @param parent the paren frame
     * @param database the database
     * @param id the id of the title
     */
    public EditJournalDialog(Frame parent, Database database, String id, boolean showNextButton) {
        super(parent, id);
        this.parent = parent;
        this.database = database;
        this.showNextButton = showNextButton;
        initializeDialog("", new Date(), "", new JournalItem[0]);
    }

    /**
     * Constructor.
     *
     * @param parent the paren frame
     * @param database the database
     * @param id the id of the title
     * @param journal the journal used to initialize the elements of the dialog
     */
    public EditJournalDialog(Frame parent, Database database, String id, Journal journal, boolean showNextButton) {
        super(parent, id);
        this.parent = parent;
        this.database = database;
        this.showNextButton = showNextButton;
        this.idOfCreatedInvoice = journal.getIdOfCreatedInvoice();
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
    private void initializeDialog(String id, Date date, String description, JournalItem[] items) {
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
        columnModel.getColumn(3).setPreferredWidth(300);

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
	@Override
	protected void handleCancel() {
	    enteredJournals.clear(); // remove all entred journals
		hideDialog();
	}

    /*
     * (non-Javadoc)
     *
     * @see cf.ui.OkCancelDialog#handleOk()
     */
    @Override
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
        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog.showMessage(parent, "gen.titleError", tr.getString("gen.invalidDate"));
            return null;
        }

        String id = tfId.getText();
        String description = tfDescription.getText();
        JournalItem[] items = itemsTableModel.getItems();

        try {
            return new Journal(id, description, date, items, idOfCreatedInvoice);
        }
        catch (IllegalArgumentException e) {
            new MessageDialog(parent, "gen.titleError",
                    tr.getString("gen.itemsNotInBalance"));
            return null;
        }
    }

    private void handleAddButtonPressed(ActionEvent e)
    {
        EditJournalItemDialog dialog = new EditJournalItemDialog(parent, database, "ajd.addJournalItem");
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
	            new EditJournalItemDialog(parent, database, "ajd.editJournalItem", item);
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
    public List<Journal> getEditedJournals() {
        return enteredJournals;
    }
}