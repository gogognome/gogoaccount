/**
 *
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
