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
package nl.gogognome.gogoaccount.gui.controllers;

import java.awt.Component;
import java.util.Arrays;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.gui.views.EditInvoiceView;
import nl.gogognome.gogoaccount.gui.views.EditJournalView;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.ComparatorUtil;

/**
 * This controller lets the user edit an existing journal.
 *
 * @author Sander Kooijmans
 */
public class EditJournalController {

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
            if (journal.createsInvoice()) {
                updateInvoiceCreatedByJournal();
            }

            try {
                document.updateJournal(journal, updatedJournal);
            } catch (DocumentModificationFailedException e) {
                MessageDialog.showErrorMessage(owner, e, "EditJournalController.updateJournalException");
            }
        }
	}

	private boolean journalModified() {
		if (updatedJournal == null) {
			return false;
		}

		return !(ComparatorUtil.equals(journal.getId(), updatedJournal.getId())
			&& ComparatorUtil.equals(journal.getDate(), updatedJournal.getDate())
			&& ComparatorUtil.equals(journal.getDescription(), updatedJournal.getDescription())
			&& Arrays.equals(journal.getItems(), updatedJournal.getItems()));
	}

	private void updateInvoiceCreatedByJournal() {
		EditInvoiceView editInvoiceView = new EditInvoiceView(document,
				"EditJournalController.editInvoiceTitle",
				document.getInvoice(journal.getIdOfCreatedInvoice()));
		ViewDialog editInvoiceDialog = new ViewDialog(owner, editInvoiceView);
		editInvoiceDialog.showDialog();
		Invoice newInvoice = editInvoiceView.getEditedInvoice();
		if (newInvoice != null) {
		    try {
		        document.updateInvoice(journal.getIdOfCreatedInvoice(), newInvoice);
		    } catch (DocumentModificationFailedException e) {
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
