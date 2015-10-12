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
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.gui.ActionRunner;
import nl.gogognome.gogoaccount.gui.dialogs.ItemsTableModel;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;

/**
 * This class implements the dialog for editing a single journal.
 *
 * @author Sander Kooijmans
 */
public class EditJournalView extends View {

	private static final long serialVersionUID = 1L;

	/** The database. */
    protected Document document;

    /** The id of the title. */
    private String titleId;

    /**
     * The journal used to initialize the view. <code>null</code> indicates that a new journal
     * is to be edited.
     */
    protected JournalEntry journalEntryToBeEdited;

    /**
     * The id of the invoice that is created by the edited journal. If <code>null</code>, then
     * no invoice is created by this journal.
     */
    private String idOfCreatedInvoice;

    protected StringModel idModel = new StringModel();

    protected StringModel descriptionModel = new StringModel();

    /** The table containing journal items. */
    private JTable itemsTable;

    protected ItemsTableModel itemsTableModel;

    /** The date model used to edit the date. */
    protected DateModel dateModel = new DateModel();

    /** The journal edited by the journal. This will only be filled when the user is editing a journal. */
    protected JournalEntry editedJournalEntry;

    private InputFieldsColumn valuesEditPanel;

    /**
     * Constructor. To edit an existing journal, give <code>journal</code> a non-<code>null</code> value.
     * To add one or more new journals, set <code>journal</code> to <code>null</code>.
     *
     * @param document the database to which the journal must be added
     * @param titleId the id of the title
     * @param journalEntry the journal used to initialize the elements of the view. Must be <code>null</code>
     *        to edit a new journal
     */
    public EditJournalView(Document document, String titleId, JournalEntry journalEntry) {
        this.document = document;
        this.titleId = titleId;
        this.journalEntryToBeEdited = journalEntry;
    }

    @Override
    public void onInit() {
    	initModels();
        addComponents();
    }

    private void initModels() {
        try {
            itemsTableModel = new ItemsTableModel(document);
            itemsTableModel.setJournalItems(new JournalEntryDetail[0]);

            initModelsForJournal(journalEntryToBeEdited);
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            close();
        }
	}

	private void initModelsForJournal(JournalEntry initialValuesJournalEntry) throws ServiceException {
		if (initialValuesJournalEntry == null) {
        	dateModel.setDate(new Date(), null);
        } else {
            idOfCreatedInvoice = initialValuesJournalEntry.getIdOfCreatedInvoice();
            idModel.setString(initialValuesJournalEntry.getId());
            dateModel.setDate(initialValuesJournalEntry.getDate());
            descriptionModel.setString(initialValuesJournalEntry.getDescription());

            for (JournalEntryDetail item : initialValuesJournalEntry.getItems()) {
                itemsTableModel.addRow(item);
            }
        }
		initValuesForNextJournal();
	}

	/**
     * Adds components to the view.
     */
    private void addComponents() {
        InputFieldsColumn vep = new InputFieldsColumn();
        addCloseable(vep);
        vep.addField("gen.id", idModel);
        vep.addField("gen.date", dateModel);
        vep.addField("gen.description", descriptionModel);
        valuesEditPanel = vep;

        // Create table of items
        itemsTable = widgetFactory.createTable(itemsTableModel);

        JPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
        buttonPanel.add(createAddButton());
        buttonPanel.add(createEditButton());
        buttonPanel.add(createDeleteButton());
        buttonPanel.add(new JLabel());
        buttonPanel.add(createOkButton());

        if (journalEntryToBeEdited == null) {
	        buttonPanel.add(createOkAndNextButton());
        }

        JScrollPane scrollableTable = new JScrollPane(itemsTable);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        int row = 0;
        panel.add(vep, SwingUtils.createGBConstraints(0, row, 2, 1));
        row++;

        panel.add(scrollableTable,
                SwingUtils.createGBConstraints(0, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.BOTH,
                        10, 0, 0, 0));
        panel.add(buttonPanel,
                SwingUtils.createGBConstraints(1, row, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
                        0, 0, 0, 0));
        row++;

        setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(panel, BorderLayout.CENTER);
    }

    private JButton createAddButton() {
	    return widgetFactory.createButton("ajd.addItem", new AbstractAction() {
	        @Override
			public void actionPerformed(ActionEvent evt) {
	            ActionRunner.run(EditJournalView.this, () -> handleAddButtonPressed());
	        }
	    });
    }

    private JButton createEditButton() {
    	return widgetFactory.createButton("ajd.editItem", new AbstractAction() {
    		@Override
			public void actionPerformed(ActionEvent evt) {
	            handleEditButtonPressed();
	        }
	    });
    }

    private JButton createDeleteButton() {
    	return widgetFactory.createButton("ajd.deleteItem", new AbstractAction() {
	        @Override
			public void actionPerformed(ActionEvent evt) {
	            handleDeleteButtonPressed();
	        }
	    });
    }

    private JButton createOkButton() {
    	return widgetFactory.createButton("gen.ok", new AbstractAction() {
	        @Override
			public void actionPerformed(ActionEvent evt) {
	            handleOkButtonPressed();
	        }
	    });
    }

    private JButton createOkAndNextButton() {
    	return widgetFactory.createButton("ajd.okAndNextJournal", new AbstractAction() {
	        @Override
			public void actionPerformed(ActionEvent evt) {
	        	handleOkAndNextButtonPressed();
	        }
	    });
    }

    /** Handles the OK button. Closes the dialog. */
    private void handleOkButtonPressed() {
        JournalEntry journalEntry = getJournalFromDialog();
        if (journalEntry != null) {
        	try {
        		createNewOrStoreUpdatedJournal(journalEntry);
                requestClose();
        	} catch (Exception e) {
                MessageDialog.showErrorMessage(this,e, "ajd.addJournalException");
        	}
        }
    }

	/** Handles the Ok + next button. */
    private void handleOkAndNextButtonPressed() {
        JournalEntry journalEntry = getJournalFromDialog();
        if (journalEntry != null) {
            try {
                createNewOrStoreUpdatedJournal(journalEntry);
                itemsTableModel.clear();
                valuesEditPanel.requestFocus();
                initValuesForNextJournal();
            } catch (Exception e) {
                MessageDialog.showErrorMessage(this, e, "ajd.addJournalException");
            }
        }
    }

	private void createNewOrStoreUpdatedJournal(JournalEntry journalEntry) throws Exception {
        if (journalEntryToBeEdited == null) {
            createNewJournal(journalEntry);
        } else {
            // Set the edited journal
            editedJournalEntry = journalEntry;
        }
	}

	protected void createNewJournal(JournalEntry journalEntry) throws DocumentModificationFailedException, ServiceException {
		document.addJournal(journalEntry, true);
	}

	protected void initValuesForNextJournal() throws ServiceException {
	}

    /**
     * Gets the journal from the values filled in the dialog.
     * @return the journal or <code>null</code> if the values are not valid,
     *          in which case the user has been notified about the problem with
     *          the input values.
     */
    private JournalEntry getJournalFromDialog() {
        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog.showMessage(this, "gen.titleError", "gen.invalidDate");
            return null;
        }

        String id = idModel.getString();
        String description = descriptionModel.getString();
        List<JournalEntryDetail> items = itemsTableModel.getRows();

        try {
            return new JournalEntry(id, description, date, items, idOfCreatedInvoice);
        }
        catch (IllegalArgumentException e) {
            MessageDialog.showMessage(this, "gen.titleError", "gen.itemsNotInBalance");
            return null;
        }
    }

    /** Handles the add button. Lets the user add a journal item. */
    private void handleAddButtonPressed() throws ServiceException {
    	JournalEntryDetail defaultItem = createDefaultItemToBeAdded();
    	EditJournalItemView view = new EditJournalItemView(document, defaultItem);
    	ViewDialog dialog = new ViewDialog(this, view);
    	dialog.showDialog();

        JournalEntryDetail item = view.getEnteredJournalEntryDetail();
        if (item != null) {
            itemsTableModel.addRow(item);
        }
    }

    /**
     * This method is called when a new journal item is to be created.
     * This method creates a JournalItem that will be used to set the initial values
     * of the new journal item.
     * @return the journal item containing the initial values; null is allowed
     */
    protected JournalEntryDetail createDefaultItemToBeAdded() throws ServiceException {
		return null;
	}

	/** Handles the edit button. Lets the user edit a journal item. */
    private void handleEditButtonPressed() {
        int row = itemsTable.getSelectedRow();
        JournalEntryDetail item = itemsTableModel.getRow(row);
        if (item != null) {
        	EditJournalItemView view = new EditJournalItemView(document, item);
        	ViewDialog dialog = new ViewDialog(this, view);
        	dialog.showDialog();

	        item = view.getEnteredJournalEntryDetail();
	        if (item != null) {
	            itemsTableModel.updateRow(row, item);
	        }
        }
    }

    /** Handles the delete button. Deletes a journal item. */
    private void handleDeleteButtonPressed() {
        itemsTableModel.removeRow(itemsTable.getSelectedRow());
    }

    /**
     * Gets the journal entered by the user. This method only returns a non-<code>null</code> value
     * if this view was initialized with a journal and the user closed the view by pressing the
     * Ok button. Otherwise, this method returns <code>null</code>.
     * @return the journal entered by the user or <code>null</code>
     */
    public JournalEntry getEditedJournalEntry() {
        return editedJournalEntry;
    }

    @Override
    public String getTitle() {
        return textResource.getString(titleId);
    }

    @Override
    public void onClose() {
    }

}