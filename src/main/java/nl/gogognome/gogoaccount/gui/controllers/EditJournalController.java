package nl.gogognome.gogoaccount.gui.controllers;

import java.awt.Component;
import java.util.Arrays;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.gui.views.EditInvoiceView;
import nl.gogognome.gogoaccount.gui.views.EditJournalView;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.ComparatorUtil;

/**
 * This controller lets the user edit an existing journal.
 */
public class EditJournalController {

	private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);

	private Component owner;
	private Document document;
	private Journal journal;
	private Journal updatedJournal;

	/**
	 * Constructs the controller.
	 * @param owner the component that uses this controller
	 * @param document the database
	 * @param journal the journal to be edited
	 */
	public EditJournalController(Component owner, Document document, Journal journal) {
		super();
		this.owner = owner;
		this.document = document;
		this.journal = journal;
	}

	public void execute() {
        EditJournalView view = new EditJournalView(document, "ajd.title", journal);
        ViewDialog dialog = new ViewDialog(owner, view);
        dialog.showDialog();

        updatedJournal = view.getEditedJournal();
        if (journalModified()) {
            try {
                if (journal.createsInvoice()) {
                    updateInvoiceCreatedByJournal();
                }
                document.updateJournal(journal, updatedJournal);
            } catch (ServiceException | DocumentModificationFailedException e) {
                MessageDialog.showErrorMessage(owner, e, "EditJournalController.updateJournalException");
            }
        }
	}

	private boolean journalModified() {
        return updatedJournal != null && !(ComparatorUtil.equals(journal.getId(), updatedJournal.getId()) && ComparatorUtil.equals(journal.getDate(), updatedJournal.getDate()) && ComparatorUtil.equals(journal.getDescription(), updatedJournal.getDescription()) && Arrays.equals(journal.getItems(), updatedJournal.getItems()));

    }

	private void updateInvoiceCreatedByJournal() throws ServiceException {
		EditInvoiceView editInvoiceView = new EditInvoiceView(document,
				"EditJournalController.editInvoiceTitle",
                journal.getIdOfCreatedInvoice() != null ? invoiceService.getInvoice(document, journal.getIdOfCreatedInvoice()) : null);
		ViewDialog editInvoiceDialog = new ViewDialog(owner, editInvoiceView);
		editInvoiceDialog.showDialog();
		Invoice newInvoice = editInvoiceView.getEditedInvoice();
		if (newInvoice != null) {
		    try {
                invoiceService.updateInvoice(document, newInvoice, editInvoiceView.getEditedDescriptions(), editInvoiceView.getEditedAmounts());
		    } catch (ServiceException e) {
		        MessageDialog.showErrorMessage(owner, e, "EditJournalController.updateInvoiceException");
		    }
		}
	}

	public boolean isJournalUpdated() {
		return updatedJournal != null;
	}

	public Journal getUpdatedJournal() {
		return updatedJournal;
	}
}
