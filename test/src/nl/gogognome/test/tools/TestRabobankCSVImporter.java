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
package nl.gogognome.test.tools;

import java.io.File;
import java.io.IOException;
import java.util.List;

import nl.gogognome.cf.services.importers.ImportedTransaction;
import nl.gogognome.cf.services.importers.ParseException;
import nl.gogognome.cf.services.importers.RabobankCSVImporter;

/**
 * This class tests the Rabobank CSV importer.
 *
 * @author Sander Kooijmans
 */
public class TestRabobankCSVImporter {

	public static void main(String[] args) throws Exception {
		for (String fileName : args) {
			importAndPrintFile(new File(fileName));
		}
	}

	private static void importAndPrintFile(File csvFile) throws IOException, ParseException {
		RabobankCSVImporter importer = new RabobankCSVImporter(csvFile);
		List<ImportedTransaction> transactions = importer.importTransactions();
		for (ImportedTransaction t : transactions) {
			System.out.println(t);
		}
	}
}
