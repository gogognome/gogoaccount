package nl.gogognome.gogoaccount.gui.controllers;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.gui.invoice.EditInvoiceView;
import nl.gogognome.gogoaccount.gui.views.EditJournalView;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;

import java.awt.*;

/**
 * This controller lets the user edit an existing journal.
 */
public class EditJournalController {

    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
	private final LedgerService ledgerService = ObjectFactory.create(LedgerService.class);

    private Component owner;
    private Document document;
    private JournalEntry journalEntry;
    private JournalEntry updatedJournalEntry;

    /**
     * Constructs the controller.
     * @param owner the component that uses this controller
     * @param document the database
     * @param journalEntry the journal to be edited
     */
    public EditJournalController(Component owner, Document document, JournalEntry journalEntry) {
        super();
        this.owner = owner;
        this.document = document;
        this.journalEntry = journalEntry;
    }

    public void execute() {
        HandleException.for_(owner, () -> {
			EditJournalView view = new EditJournalView(document, "ejd.editJournalTitle", journalEntry, ledgerService.findJournalEntryDetails(document, journalEntry));
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
