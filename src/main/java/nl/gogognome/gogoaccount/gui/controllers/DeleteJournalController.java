package nl.gogognome.gogoaccount.gui.controllers;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.MessageDialog;

import java.awt.*;

/**
 * This controller lets the user delete an existing journal.
 */
public class DeleteJournalController {

    private final Document document;
    private final InvoiceService invoiceService;
    private final LedgerService ledgerService;

    private Component owner;
    private JournalEntry journalEntry;

    private boolean journalDeleted;

    public DeleteJournalController(Document document, InvoiceService invoiceService, LedgerService ledgerService) {
        this.document = document;
        this.invoiceService = invoiceService;
        this.ledgerService = ledgerService;
    }

    public void setOwner(Component owner) {
        this.owner = owner;
    }

    public void setJournalEntryToBeDeleted(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public void execute() throws ServiceException {
        if (invoiceService.hasPayments(document, journalEntry.getIdOfCreatedInvoice())) {
            MessageDialog.showWarningMessage(owner, "editJournalsView.journalCreatingInvoiceCannotBeDeleted");
            return;
        }

        int choice = MessageDialog.showYesNoQuestion(owner, "gen.titleWarning",
            "editJournalsView.areYouSureAboutDeletion");
        if (choice != MessageDialog.YES_OPTION) {
            return; // do not remove the journal
        }

        try {
            ledgerService.removeJournal(document, journalEntry);
            journalDeleted = true;
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(owner, e, "DeleteJournalController.serviceException");
        }
    }

    public boolean isJournalDeleted() {
        return journalDeleted;
    }

}
