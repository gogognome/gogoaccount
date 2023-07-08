package nl.gogognome.gogoaccount.component.importer;

import java.io.*;
import java.util.List;

/**
 * This interface specifies an importer for a file containing transactions.
 *
 * @author Sander Kooijmans
 */
public interface TransactionImporter {

	/**
	 * Reads a bank statement and returns a list of transactions.
	 * This method does not close the reader.
	 *
	 * @param file contains the bank statement
	 * @return the transactions
	 * @throws IOException if a problem occurred while reading the file
	 * @throws ParseException if a problem occurred while interpreting the file
	 */
	public List<ImportedTransaction> importTransactions(File file)
		throws IOException, ParseException;
}
