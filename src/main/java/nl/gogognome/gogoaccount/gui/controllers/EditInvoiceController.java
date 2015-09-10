package nl.gogognome.gogoaccount.gui.controllers;

import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.gui.views.EditInvoiceView;
import nl.gogognome.gogoaccount.gui.views.EditJournalView;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;

import java.awt.*;

/**
 * This controller lets the user edit an existing invoice. If the invoice
 * has been changed and the journal creating the invoice is present
 * in the database, then the user can also edit this journal.
 */
public class EditInvoiceController {

    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);

	private Component owner;
	private Document document;
	private Invoice invoice;
	private Invoice updatedInvoice;

	/**
	 * Constructs the controller.
	 * @param owner the component that uses this controller
	 * @param document the database
	 * @param invoice the invoice to be edited
	 */
	public EditInvoiceController(Component owner, Document document, Invoice invoice) {
		super();
		this.owner = owner;
		this.document = document;
		this.invoice = invoice;
	}

	public void execute() {
        EditInvoiceView view = new EditInvoiceView(document,
        		"EditInvoiceController.editInvoiceTitle", invoice);
        ViewDialog dialog = new ViewDialog(owner, view);
        dialog.showDialog();

        updatedInvoice = view.getEditedInvoice();
        Journal journal = document.getCreatingJournal(invoice.getId());
        if (journal != null) {
            updateJournalCreatingInvoice(journal);
        }

        try {
            invoiceService.updateInvoice(document, updatedInvoice, view.getEditedDescriptions(), view.getEditedAmounts());
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(owner, e, "EditJournalController.updateInvoiceException");
        }
	}

	private void updateJournalCreatingInvoice(Journal journal) {
		EditJournalView view = new EditJournalView(document,
				"EditInvoiceController.editJournalTitle", journal);
		ViewDialog dialog = new ViewDialog(owner, view);
		dialog.showDialog();
		Journal updatedJournal = view.getEditedJournal();
		if (updatedJournal != null) {
		    try {
		        document.updateJournal(journal, updatedJournal);
		    } catch (ServiceException | DocumentModificationFailedException e) {
		        MessageDialog.showErrorMessage(owner, e, "EditJournalController.updateJournalException");
		    }
		}
	}

	public boolean isInvoiceUpdated() {
		return updatedInvoice != null;
	}

	public Invoice getUpdatedInvoice() {
		return updatedInvoice;
	}
}
