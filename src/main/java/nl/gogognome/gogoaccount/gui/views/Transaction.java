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
package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.services.importers.ImportedTransaction;

/**
 * This class represents a row in the table with an imported transaction and the journal
 * that was created for that imported transaction.
 *
 * @author Sander Kooijmans
 */
class Transaction {
	private final ImportedTransaction importedTransaction;
	private Journal journal;

	public Transaction(ImportedTransaction importedTransaction, Journal journal) {
		this.importedTransaction = importedTransaction;
		this.journal = journal;
	}

	public ImportedTransaction getImportedTransaction() {
		return importedTransaction;
	}

	public Journal getJournal() {
		return journal;
	}

	public void setJournal(Journal journal) {
		this.journal = journal;
	}
}
