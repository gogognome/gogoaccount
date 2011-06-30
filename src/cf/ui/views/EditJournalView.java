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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import nl.gogognome.lib.gui.beans.ValuesEditPanel;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Database;
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

    public interface JournalAddListener {
    	public void journalAdded(Journal journal);
    }

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

    private StringModel idModel = new StringModel();

    private StringModel descriptionModel = new StringModel();

    /** The table containing journal items. */
    private JTable itemsTable;

    private ItemsTableModel itemsTableModel;

    /** The date model used to edit the date. */
    private DateModel dateModel = new DateModel();

    /** The journal edited by the journal. This will only be filled when the user is editing a journal. */
    private Journal editedJournal;

    private List<JournalAddListener> listeners = new ArrayList<EditJournalView.JournalAddListener>();

    private ValuesEditPanel valuesEditPanel;

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

    public void addListener(JournalAddListener listener) {
    	listeners.add(listener);
    }

    public void removeListener(JournalAddListener listener) {
    	listeners.remove(listener);
    }

    @Override
    public void onInit() {
    	initModels();
        addComponents();
    }

    private void initModels() {
        itemsTableModel = new ItemsTableModel(database);
        itemsTableModel.setJournalItems(new JournalItem[0]);

        if (initialJournal == null) {
        	dateModel.setDate(new Date(), null);
        } else {
            idOfCreatedInvoice = initialJournal.getIdOfCreatedInvoice();
            idModel.setString(initialJournal.getId());
            dateModel.setDate(initialJournal.getDate());
            descriptionModel.setString(initialJournal.getDescription());

            for (JournalItem item : initialJournal.getItems()) {
                itemsTableModel.addItem(item);
            }
        }
	}

	/**
     * Adds components to the view.
     */
    private void addComponents() {
        WidgetFactory wf = WidgetFactory.getInstance();

        ValuesEditPanel vep = new ValuesEditPanel();
        addDeinitializable(vep);
        vep.addField("gen.id", idModel);
        vep.addField("gen.date", dateModel);
        vep.addField("gen.description", descriptionModel);
        valuesEditPanel = vep;

        // Create table of items
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

        JPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
        buttonPanel.add(createAddButton());
        buttonPanel.add(createEditButton());
        buttonPanel.add(createDeleteButton());
        buttonPanel.add(new JLabel());
        buttonPanel.add(createOkButton());

        if (initialJournal == null) {
	        buttonPanel.add(createOkAndNextButton());
        }

        JScrollPane scrollableTable = new JScrollPane(itemsTable);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.add(vep, SwingUtils.createGBConstraints(0, 0, 2, 1));
        panel.add(scrollableTable,
                SwingUtils.createGBConstraints(0, 1, 1, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.BOTH,
                        10, 0, 0, 0));
        panel.add(buttonPanel,
                SwingUtils.createGBConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
                        0, 0, 0, 0));

        setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(panel, BorderLayout.CENTER);
    }

    private JButton createAddButton() {
	    return WidgetFactory.getInstance().createButton("ajd.addItem", new AbstractAction() {
	        @Override
			public void actionPerformed(ActionEvent evt) {
	            handleAddButtonPressed();
	        }
	    });
    }

    private JButton createEditButton() {
    	return WidgetFactory.getInstance().createButton("ajd.editItem", new AbstractAction() {
    		@Override
			public void actionPerformed(ActionEvent evt) {
	            handleEditButtonPressed();
	        }
	    });
    }

    private JButton createDeleteButton() {
    	return WidgetFactory.getInstance().createButton("ajd.deleteItem", new AbstractAction() {
	        @Override
			public void actionPerformed(ActionEvent evt) {
	            handleDeleteButtonPressed();
	        }
	    });
    }

    private JButton createOkButton() {
    	return WidgetFactory.getInstance().createButton("gen.ok", new AbstractAction() {
	        @Override
			public void actionPerformed(ActionEvent evt) {
	            handleOkButtonPressed();
	        }
	    });
    }

    private JButton createOkAndNextButton() {
    	return WidgetFactory.getInstance().createButton("ajd.okAndNextJournal", new AbstractAction() {
	        @Override
			public void actionPerformed(ActionEvent evt) {
	        	handleOkAndNextButtonPressed();
	        }
	    });
    }

    /** Handles the OK button. Closes the dialog. */
    private void handleOkButtonPressed() {
        Journal journal = getJournalFromDialog();
        if (journal != null) {
        	try {
        		createNewOrStoreUpdatedJournal(journal);
                requestClose();
        	} catch (Exception e) {
                MessageDialog.showErrorMessage(this,e, "ajd.addJournalException");
        	}
        }
    }

	/** Handles the Ok + next button. */
    private void handleOkAndNextButtonPressed() {
        Journal journal = getJournalFromDialog();
        if (journal != null) {
            try {
                createNewOrStoreUpdatedJournal(journal);
                itemsTableModel.clear();
                valuesEditPanel.requestFocus();
            } catch (Exception e) {
                MessageDialog.showErrorMessage(this, e, "ajd.addJournalException");
            }
        }
    }

    private void createNewOrStoreUpdatedJournal(Journal journal) throws Exception {
        if (initialJournal == null) {
            // Add the new journal to the database
            database.addJournal(journal, true);
        } else {
            // Set the edited journal
            editedJournal = journal;
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

        String id = idModel.getString();
        String description = descriptionModel.getString();
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
    	listeners.clear();
    }

}