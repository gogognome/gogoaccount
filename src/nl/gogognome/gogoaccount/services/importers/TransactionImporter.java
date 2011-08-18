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
package nl.gogognome.gogoaccount.services.importers;

import java.io.IOException;
import java.io.Reader;
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
	 * @param reader contains the bank statement
	 * @return the transactions
	 * @throws IOException if a problem occurred while reading the file
	 * @throws ParseException if a problem occurred while interpreting the file
	 */
	public List<ImportedTransaction> importTransactions(Reader reader)
		throws IOException, ParseException;
}
