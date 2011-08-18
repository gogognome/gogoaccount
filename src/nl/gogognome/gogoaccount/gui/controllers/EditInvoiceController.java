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

import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.database.DatabaseModificationFailedException;
import nl.gogognome.gogoaccount.gui.views.EditInvoiceView;
import nl.gogognome.gogoaccount.gui.views.EditJournalView;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.ComparatorUtil;

/**
 * This controller lets the user edit an existing invoice. If the invoice
 * has been changed and the journal creating the invoice is present
 * in the database, then the user can also edit this journal.
 *
 * @author Sander Kooijmans
 */
public class EditInvoiceController {

	private Component owner;
	private Database database;
	private Invoice invoice;
	private Invoice updatedInvoice;

	/**
	 * Constructs the controller.
	 * @param owner the component that uses this controller
	 * @param database the database
	 * @param invoice the invoice to be edited
	 */
	public EditInvoiceController(Component owner, Database database, Invoice invoice) {
		super();
		this.owner = owner;
		this.database = database;
		this.invoice = invoice;
	}

	public void execute() {
        EditInvoiceView view = new EditInvoiceView(database,
        		"EditInvoiceController.editInvoiceTitle", invoice);
        ViewDialog dialog = new ViewDialog(owner, view);
        dialog.showDialog();

        updatedInvoice = view.getEditedInvoice();
        if (invoiceModified()) {
        	Journal journal = database.getCreatingJournal(invoice.getId());
            if (journal != null) {
                updateJournalCreatingInvoice(journal);
            }

            try {
                database.updateInvoice(invoice.getId(), updatedInvoice);
            } catch (DatabaseModificationFailedException e) {
                MessageDialog.showErrorMessage(owner, e, "EditJournalController.updateInvoiceException");
            }
        }
	}

	private boolean invoiceModified() {
		if (updatedInvoice == null) {
			return false;
		}

		return !(ComparatorUtil.equals(invoice.getId(), updatedInvoice.getId())
			&& ComparatorUtil.equals(invoice.getIssueDate(), updatedInvoice.getIssueDate())
			&& ComparatorUtil.equals(invoice.getAmountToBePaid(), updatedInvoice.getAmountToBePaid())
			&& ComparatorUtil.equals(invoice.getConcerningParty(), updatedInvoice.getConcerningParty())
			&& ComparatorUtil.equals(invoice.getPayingParty(), updatedInvoice.getPayingParty())
			&& Arrays.equals(invoice.getAmounts(), updatedInvoice.getAmounts())
			&& Arrays.equals(invoice.getDescriptions(), updatedInvoice.getDescriptions()));
	}

	private void updateJournalCreatingInvoice(Journal journal) {
		EditJournalView view = new EditJournalView(database,
				"EditInvoiceController.editJournalTitle", journal);
		ViewDialog dialog = new ViewDialog(owner, view);
		dialog.showDialog();
		Journal updatedJournal = view.getEditedJournal();
		if (updatedJournal != null) {
		    try {
		        database.updateJournal(journal, updatedJournal);
		    } catch (DatabaseModificationFailedException e) {
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
