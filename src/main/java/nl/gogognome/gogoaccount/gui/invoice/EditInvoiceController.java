package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.views.EditJournalView;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;

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

    private Component owner;
    private Document document;
    private Invoice invoice;
    private Invoice updatedInvoice;

    public EditInvoiceController(Document document, InvoiceService invoiceService, LedgerService ledgerService, ViewFactory viewFactory) {
        this.document = document;
        this.invoiceService = invoiceService;
        this.ledgerService = ledgerService;
        this.viewFactory = viewFactory;
    }

    public void setOwner(Component owner) {
        this.owner = owner;
    }

    public void setInvoiceToBeEdited(Invoice invoice) {
        this.invoice = invoice;
    }

    public void execute() {
        HandleException.for_(owner, () -> {
            EditInvoiceView view = (EditInvoiceView) viewFactory.createView(EditInvoiceView.class);
            view.setTitleId("EditInvoiceController.editInvoiceTitle");
            view.setInvoiceToBeEdited(invoice);
            ViewDialog dialog = new ViewDialog(owner, view);
            dialog.showDialog();

            updatedInvoice = view.getEditedInvoice();
            if (updatedInvoice == null) {
                return;
            }
            try {
                JournalEntry journalEntry = ledgerService.findJournalThatCreatesInvoice(document, invoice.getId());
                if (journalEntry != null) {
                    updateJournalCreatingInvoice(journalEntry);
                }

                invoiceService.updateInvoice(document, updatedInvoice, view.getEditedDescriptions(), view.getEditedAmounts());
            } catch (ServiceException e) {
                MessageDialog.showErrorMessage(owner, e, "EditJournalController.updateInvoiceException");
            }
        });
    }

    private void updateJournalCreatingInvoice(JournalEntry journalEntry) {
        HandleException.for_(owner, () -> {
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

    public boolean isInvoiceUpdated() {
        return updatedInvoice != null;
    }

    public Invoice getUpdatedInvoice() {
        return updatedInvoice;
    }
}
