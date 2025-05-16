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
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;

import java.awt.*;

/**
 * This controller lets the user edit an existing journal entry.
 */
public class EditJournalEntryController {

    private final InvoiceService invoiceService;
	private final LedgerService ledgerService;
    private final ViewFactory viewFactory;
    private final TextResource textResource;

    private Component owner;
    private HandleException handleException;
    private Document document;
    private JournalEntry journalEntry;
    private JournalEntry updatedJournalEntry;

    public EditJournalEntryController(Document document, InvoiceService invoiceService, LedgerService ledgerService, ViewFactory viewFactory, TextResource textResource) {
        this.document = document;
        this.invoiceService = invoiceService;
        this.ledgerService = ledgerService;
        this.viewFactory = viewFactory;
        this.textResource = textResource;
    }

    public void setOwner(Component owner) {
        this.owner = owner;
        handleException = new HandleException(new MessageDialog(textResource, owner));
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public void execute() {
        handleException.of(() -> {
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
                ledgerService.updateJournalEntry(document, updatedJournalEntry, view.getEditedJournalEntryDetails());
            }
		});
    }

    private void updateInvoiceCreatedByJournal() throws ServiceException {
        handleException.of(() -> {
            EditInvoiceView editInvoiceView = (EditInvoiceView) viewFactory.createView(EditInvoiceView.class);
            editInvoiceView.setTitleId("EditJournalController.editInvoiceTitle");
            editInvoiceView.setInvoiceToBeEdited(journalEntry.getIdOfCreatedInvoice() != null ? invoiceService.getInvoice(document, journalEntry.getIdOfCreatedInvoice()) : null);
            ViewDialog editInvoiceDialog = new ViewDialog(owner, editInvoiceView);
            editInvoiceDialog.showDialog();
            Invoice newInvoice = editInvoiceView.getEditedInvoice();
            if (newInvoice != null) {
                invoiceService.updateInvoice(document, newInvoice, editInvoiceView.getEditedDescriptions(), editInvoiceView.getEditedAmounts());
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
