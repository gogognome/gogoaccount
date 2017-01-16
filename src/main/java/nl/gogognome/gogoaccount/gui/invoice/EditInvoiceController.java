package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.views.EditJournalView;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;

import java.awt.*;

/**
 * This controller lets the user edit an existing invoice. If the invoice
 * has been changed and the journal creating the invoice is present
 * in the database, then the user can also edit this journal.
 */
public class EditInvoiceController {

    private final InvoiceService invoiceService;
    private final LedgerService ledgerService;
    private final ViewFactory viewFactory;
    private final TextResource textResource;

    private Component owner;
    private HandleException handleException;
    private Document document;
    private Invoice invoice;
    private Invoice updatedInvoice;

    public EditInvoiceController(Document document, InvoiceService invoiceService, LedgerService ledgerService, ViewFactory viewFactory, TextResource textResource) {
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

    public void setInvoiceToBeEdited(Invoice invoice) {
        this.invoice = invoice;
    }

    public void execute() {
        handleException.of(() -> {
            EditInvoiceView view = (EditInvoiceView) viewFactory.createView(EditInvoiceView.class);
            view.setTitleId("EditInvoiceController.editInvoiceTitle");
            view.setInvoiceToBeEdited(invoice);
            ViewDialog dialog = new ViewDialog(owner, view);
            dialog.showDialog();

            updatedInvoice = view.getEditedInvoice();
            if (updatedInvoice == null) {
                return;
            }
            JournalEntry journalEntry = ledgerService.findJournalThatCreatesInvoice(document, invoice.getId());
            if (journalEntry != null) {
                updateJournalCreatingInvoice(journalEntry);
            }

            invoiceService.updateInvoice(document, updatedInvoice, view.getEditedDescriptions(), view.getEditedAmounts());

        });
    }

    private void updateJournalCreatingInvoice(JournalEntry journalEntry) {
        handleException.of(() -> {
            EditJournalView view = (EditJournalView) viewFactory.createView(EditJournalView.class);
            view.setJournalEntryToBeEdited(journalEntry, ledgerService.findJournalEntryDetails(document, journalEntry));
            view.setTitle("EditInvoiceController.editJournalTitle");
            ViewDialog dialog = new ViewDialog(owner, view);
            dialog.showDialog();
            JournalEntry updatedJournalEntry = view.getEditedJournalEntry();
            if (updatedJournalEntry != null) {
                ledgerService.updateJournalEntry(document, updatedJournalEntry, view.getEditedJournalEntryDetails());
            }
        });
    }

}
