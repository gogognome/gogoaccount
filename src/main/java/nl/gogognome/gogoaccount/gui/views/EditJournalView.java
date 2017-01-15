package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.dialogs.JournalEntryDetailsTableModel;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.action.Actions;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.models.Tables;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Date;
import java.util.List;

/**
 * This class implements the dialog for editing a single journal.
 */
public class EditJournalView extends View {

	private static final long serialVersionUID = 1L;

    protected final Document document;
    private InputFieldsColumn valuesEditPanel;
    private final ConfigurationService configurationService;
    private final InvoiceService invoiceService;
    private final LedgerService ledgerService;
    private final PartyService partyService;
    private final ViewFactory viewFactory;
    private final MessageDialog messageDialog;

    private String titleId;

    /**
     * The journal used to initialize the editJournalEntryDetailView. <code>null</code> indicates that a new journal
     * is to be edited.
     */
    protected JournalEntry journalEntryToBeEdited;

    /**
     * The journal details used to initialize the editJournalEntryDetailView. <code>null</code> indicates that a new journal
     * is to be edited.
     */
    protected List<JournalEntryDetail> journalEntryDetailsToBeEdited;

    /**
     * The id of the invoice that is created by the edited journal. If <code>null</code>, then
     * no invoice is created by this journal.
     */
    private String idOfCreatedInvoice;

    protected StringModel idModel = new StringModel();
    protected StringModel descriptionModel = new StringModel();
    private JTable journalEntryDetailsTable;
    protected JournalEntryDetailsTableModel journalEntryDetailsTableModel;
    protected DateModel dateModel = new DateModel();
    protected JournalEntry editedJournalEntry;
    protected List<JournalEntryDetail> editedJournalEntryDetails;

    public EditJournalView(Document document, ConfigurationService configurationService, InvoiceService invoiceService,
                           LedgerService ledgerService, PartyService partyService, ViewFactory viewFactory) {
        this.document = document;
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.ledgerService = ledgerService;
        this.partyService = partyService;
        this.viewFactory = viewFactory;
        messageDialog = new MessageDialog(textResource, this);
    }

    public void setJournalEntryToBeEdited(JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails) {
        this.journalEntryToBeEdited = journalEntry;
        this.journalEntryDetailsToBeEdited = journalEntryDetails;
    }

    public void setTitle(String titleId) {
        this.titleId = titleId;
    }

    @Override
    public void onInit() {
    	initModels();
        addComponents();
    }

    private void initModels() {
        try {
            journalEntryDetailsTableModel = new JournalEntryDetailsTableModel(document, configurationService, invoiceService, partyService);

            initModelsForJournal(journalEntryToBeEdited, journalEntryDetailsToBeEdited);
        } catch (ServiceException e) {
            messageDialog.showErrorMessage(e, "gen.problemOccurred");
            close();
        }
	}

	private void initModelsForJournal(JournalEntry initialValuesJournalEntry, List<JournalEntryDetail> initialDetails) throws ServiceException {
		if (initialValuesJournalEntry == null) {
        	dateModel.setDate(new Date(), null);
        } else {
            idOfCreatedInvoice = initialValuesJournalEntry.getIdOfCreatedInvoice();
            idModel.setString(initialValuesJournalEntry.getId());
            dateModel.setDate(initialValuesJournalEntry.getDate());
            descriptionModel.setString(initialValuesJournalEntry.getDescription());

            for (JournalEntryDetail item : initialDetails) {
                journalEntryDetailsTableModel.addRow(item);
            }
        }
		initValuesForNextJournal();
	}

	/**
     * Adds components to the editJournalEntryDetailView.
     */
    private void addComponents() {
        InputFieldsColumn vep = new InputFieldsColumn();
        addCloseable(vep);
        vep.addField("gen.id", idModel);
        vep.addField("gen.date", dateModel);
        vep.addField("gen.description", descriptionModel);
        valuesEditPanel = vep;

        // Create table of items
        journalEntryDetailsTable = Tables.createTable(journalEntryDetailsTableModel);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
        addCloseable(buttonPanel);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
        buttonPanel.add(createAddButton());
        buttonPanel.add(createEditButton());
        buttonPanel.add(createDeleteButton());
        buttonPanel.add(new JLabel());
        buttonPanel.add(createOkButton());

        if (journalEntryToBeEdited == null) {
	        buttonPanel.add(createOkAndNextButton());
        }

        JScrollPane scrollableTable = new JScrollPane(journalEntryDetailsTable);

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
        return widgetFactory.createButton("ajd.addItem", Actions.build(this, this::handleAddButtonPressed));
    }

    private JButton createEditButton() {
        Action action = Actions.build(this, this::handleEditButtonPressed);
        addCloseable(Tables.onSelectionChange(journalEntryDetailsTable, () -> {
            LoggerFactory.getLogger(EditJournalView.class).info("Selection changed");
            action.setEnabled(journalEntryDetailsTable.getSelectedRowCount() == 1);
        }));
        return widgetFactory.createButton("ajd.editItem", action);
    }

    private JButton createDeleteButton() {
        Action action = Actions.build(this, this::handleDeleteButtonPressed);
        addCloseable(Tables.onSelectionChange(journalEntryDetailsTable, () -> action.setEnabled(journalEntryDetailsTable.getSelectedRowCount() > 0)));
        return widgetFactory.createButton("ajd.deleteItem", action);
    }

    private JButton createOkButton() {
    	return widgetFactory.createButton("gen.ok", Actions.build(this, this::handleOkButtonPressed));
    }

    private JButton createOkAndNextButton() {
    	return widgetFactory.createButton("ajd.okAndNextJournal", Actions.build(this, this::handleOkAndNextButtonPressed));
    }

    private void handleOkButtonPressed() {
        JournalEntry journalEntry = getJournalEntryFromDialog();
        if (journalEntry != null) {
        	try {
        		createNewOrStoreUpdatedJournal(journalEntry, getJournalEntryDetailsFromDialog());
                requestClose();
        	} catch (Exception e) {
                messageDialog.showErrorMessage(e, "ajd.addJournalException");
        	}
        }
    }

    private void handleOkAndNextButtonPressed() {
        JournalEntry journalEntry = getJournalEntryFromDialog();
        if (journalEntry != null) {
            try {
                createNewOrStoreUpdatedJournal(journalEntry, getJournalEntryDetailsFromDialog());
                journalEntryDetailsTableModel.clear();
                valuesEditPanel.requestFocus();
                initValuesForNextJournal();
            } catch (Exception e) {
                messageDialog.showErrorMessage(e, "ajd.addJournalException");
            }
        }
    }

	private void createNewOrStoreUpdatedJournal(JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails) throws Exception {
        if (journalEntryToBeEdited == null) {
            createNewJournal(journalEntry, journalEntryDetails);
        } else {
            // Set the edited journal
            editedJournalEntry = journalEntry;
            editedJournalEntryDetails = journalEntryDetails;
        }
	}

	protected JournalEntry createNewJournal(JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails) throws ServiceException {
		return ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, true);
	}

	protected void initValuesForNextJournal() throws ServiceException {
	}

    private JournalEntry getJournalEntryFromDialog() {
        JournalEntry journalEntry = new JournalEntry(journalEntryToBeEdited != null ? journalEntryToBeEdited.getUniqueId() : -1);
        journalEntry.setDate(dateModel.getDate());
        journalEntry.setId(idModel.getString());
        journalEntry.setIdOfCreatedInvoice(idOfCreatedInvoice);
        journalEntry.setDescription(descriptionModel.getString());

        return journalEntry;
    }

    private List<JournalEntryDetail> getJournalEntryDetailsFromDialog() {
        return journalEntryDetailsTableModel.getRows();
    }

    /** Handles the add button. Lets the user add a journal item. */
    private void handleAddButtonPressed() throws ServiceException {
        HandleException.for_(this, () -> {
            EditJournalEntryDetailView editJournalEntryDetailView = (EditJournalEntryDetailView) viewFactory.createView(EditJournalEntryDetailView.class);
            editJournalEntryDetailView.setItemToBeEdited(createDefaultItemToBeAdded());
            ViewDialog dialog = new ViewDialog(this, editJournalEntryDetailView);
            dialog.showDialog();

            JournalEntryDetail item = editJournalEntryDetailView.getEnteredJournalEntryDetail();
            if (item != null) {
                journalEntryDetailsTableModel.addRow(item);
            }
        });
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
        HandleException.for_(this, () -> {
            int row = journalEntryDetailsTable.getSelectedRow();
            if (row != -1) {
                JournalEntryDetail item = journalEntryDetailsTableModel.getRow(row);
                if (item != null) {
                    EditJournalEntryDetailView editJournalEntryDetailView = (EditJournalEntryDetailView) viewFactory.createView(EditJournalEntryDetailView.class);
                    editJournalEntryDetailView.setItemToBeEdited(item);
                    ViewDialog dialog = new ViewDialog(this, editJournalEntryDetailView);
                    dialog.showDialog();

                    item = editJournalEntryDetailView.getEnteredJournalEntryDetail();
                    if (item != null) {
                        journalEntryDetailsTableModel.updateRow(row, item);
                    }
                }
            }
        });
    }

    /** Handles the delete button. Deletes a journal item. */
    private void handleDeleteButtonPressed() {
        journalEntryDetailsTableModel.removeRow(journalEntryDetailsTable.getSelectedRow());
    }

    /**
     * Gets the journal entered by the user. This method only returns a non-<code>null</code> value
     * if this editJournalEntryDetailView was initialized with a journal and the user closed the editJournalEntryDetailView by pressing the
     * Ok button. Otherwise, this method returns <code>null</code>.
     * @return the journal entered by the user or <code>null</code>
     */
    public JournalEntry getEditedJournalEntry() {
        return editedJournalEntry;
    }

    public List<JournalEntryDetail> getEditedJournalEntryDetails() {
        return editedJournalEntryDetails;
    }

    @Override
    public String getTitle() {
        return textResource.getString(titleId);
    }

    @Override
    public void onClose() {
    }

}