package nl.gogognome.gogoaccount.gui.controllers;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.invoice.EditInvoiceView;
import nl.gogognome.gogoaccount.gui.views.EditJournalView;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;

import java.awt.*;

/**
 * This controller lets the user edit an existing journal.
 */
public class EditJournalController {

    private final InvoiceService invoiceService;
	private final LedgerService ledgerService;
    private final ViewFactory viewFactory;

    private Component owner;
    private Document document;
    private JournalEntry journalEntry;
    private JournalEntry updatedJournalEntry;

    public EditJournalController(Document document, InvoiceService invoiceService, LedgerService ledgerService, ViewFactory viewFactory) {
        this.document = document;
        this.invoiceService = invoiceService;
        this.ledgerService = ledgerService;
        this.viewFactory = viewFactory;
    }

    public void setOwner(Component owner) {
        this.owner = owner;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public void execute() {
        HandleException.for_(owner, () -> {
			EditJournalView view = (EditJournalView) viewFactory.createView(EditJournalView.class);
            view.setJournalEntryToBeEdited(journalEntry, ledgerService.findJournalEntryDetails(document, journalEntry));
            view.setTitle("ejd.editJournalTitle");
			ViewDialog dialog = new ViewDialog(owner, view);
			dialog.showDialog();

			updatedJournalEntry = view.getEditedJournalEntry();
            if (updatedJournalEntry != null) {
                if (journalEntry.createsInvoice()) {
                    updateInvoiceCreatedByJournal();
                }
                ledgerService.updateJournal(document, updatedJournalEntry, view.getEditedJournalEntryDetails());
            }
		});
    }

    private void updateInvoiceCreatedByJournal() throws ServiceException {
        HandleException.for_(owner, () -> {
            EditInvoiceView editInvoiceView = new EditInvoiceView(document,
                    "EditJournalController.editInvoiceTitle",
                    journalEntry.getIdOfCreatedInvoice() != null ? invoiceService.getInvoice(document, journalEntry.getIdOfCreatedInvoice()) : null);
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
        });
    }

    public boolean isJournalUpdated() {
        return updatedJournalEntry != null;
    }

    public JournalEntry getUpdatedJournalEntry() {
        return updatedJournalEntry;
    }
}
