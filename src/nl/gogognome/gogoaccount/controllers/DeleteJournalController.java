/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package nl.gogognome.gogoaccount.controllers;

import java.awt.Component;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.cf.services.ServiceException;
import nl.gogognome.lib.swing.MessageDialog;
import cf.engine.Database;
import cf.engine.Journal;

/**
 * This controller lets the user delete an existing journal.
 *
 * @author Sander Kooijmans
 */
public class DeleteJournalController {

	private Component owner;
	private Database database;
	private Journal journal;

	private boolean journalDeleted;

	/**
	 * Constructs the controller.
	 * @param owner the component that uses this controller
	 * @param database the database
	 * @param journal the journal to be deleted
	 */
	public DeleteJournalController(Component owner, Database database, Journal journal) {
		super();
		this.owner = owner;
		this.database = database;
		this.journal = journal;
	}

	public void execute() {
        if (!database.getPayments(journal.getIdOfCreatedInvoice()).isEmpty()) {
            MessageDialog.showWarningMessage(owner, "editJournalsView.journalCreatingInvoiceCannotBeDeleted");
            return;
        }

        int choice = MessageDialog.showYesNoQuestion(owner, "gen.titleWarning",
        	"editJournalsView.areYouSureAboutDeletion");
	    if (choice != MessageDialog.YES_OPTION) {
	        return; // do not remove the journal
	    }

	    try {
	        BookkeepingService.removeJournal(database, journal);
	        journalDeleted = true;
	    } catch (ServiceException e) {
	        MessageDialog.showErrorMessage(owner, e, "DeleteJournalController.serviceException");
	    }
	}

	public boolean isJournalDeleted() {
		return journalDeleted;
	}

}
