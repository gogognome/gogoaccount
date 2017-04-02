package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.importer.ImportedTransaction;

/**
 * This class represents a row in the table with an imported transaction and the journal
 * that was created for that imported transaction.
 *
 * @author Sander Kooijmans
 */
class Transaction {
	private final ImportedTransaction importedTransaction;
	private JournalEntry journalEntry;

	public Transaction(ImportedTransaction importedTransaction, JournalEntry journalEntry) {
		this.importedTransaction = importedTransaction;
		this.journalEntry = journalEntry;
	}

	public ImportedTransaction getImportedTransaction() {
		return importedTransaction;
	}

	public JournalEntry getJournalEntry() {
		return journalEntry;
	}

	public void setJournalEntry(JournalEntry journalEntry) {
		this.journalEntry = journalEntry;
	}
}
