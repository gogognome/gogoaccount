package nl.gogognome.gogoaccount.gui.controllers;

import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.MessageDialog;

import java.awt.*;

/**
 * This controller lets the user delete an existing journal.
 */
public class DeleteJournalController {

    private final BookkeepingService bookkeepingService = ObjectFactory.create(BookkeepingService.class);
	private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);

	private Component owner;
	private Document document;
	private Journal journal;

	private boolean journalDeleted;

	/**
	 * Constructs the controller.
	 * @param owner the component that uses this controller
	 * @param document the database
	 * @param journal the journal to be deleted
	 */
	public DeleteJournalController(Component owner, Document document, Journal journal) {
		super();
		this.owner = owner;
		this.document = document;
		this.journal = journal;
	}

	public void execute() throws ServiceException {
        if (invoiceService.hasPayments(document, journal.getIdOfCreatedInvoice())) {
            MessageDialog.showWarningMessage(owner, "editJournalsView.journalCreatingInvoiceCannotBeDeleted");
            return;
        }

        int choice = MessageDialog.showYesNoQuestion(owner, "gen.titleWarning",
        	"editJournalsView.areYouSureAboutDeletion");
	    if (choice != MessageDialog.YES_OPTION) {
	        return; // do not remove the journal
	    }

	    try {
	        bookkeepingService.removeJournal(document, journal);
	        journalDeleted = true;
	    } catch (ServiceException e) {
	        MessageDialog.showErrorMessage(owner, e, "DeleteJournalController.serviceException");
	    }
	}

	public boolean isJournalDeleted() {
		return journalDeleted;
	}

}
