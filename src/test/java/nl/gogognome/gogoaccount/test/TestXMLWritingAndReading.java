package nl.gogognome.gogoaccount.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.gogoaccount.services.importers.ImportedTransaction;
import nl.gogognome.gogoaccount.services.importers.ImportedTransactionRabobankCsv;
import nl.gogognome.lib.util.DateUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests storing to and retrieval from a database in an XML file.
 *
 * @author Sander Kooijmans
 */
public class TestXMLWritingAndReading extends AbstractBookkeepingTest {

	private final ConfigurationService configurationService = new ConfigurationService();
	private final BookkeepingService bookkeepingService = new BookkeepingService();
	private File file;

	@Before
	public void initFile() throws IOException {
		file = File.createTempFile("ga-test", ".xml");
	}

	@After
	public void deleteFile() {
		if (file.exists()) {
			assertTrue("Failed to delete temp file " + file.getAbsolutePath(),
					file.delete());
		}
	}

	@Test
	public void testDefaultDatabase() throws Exception {
		writeReadAndCompareDatabase();
	}

	@Test
	public void testImportedTransactions() throws Exception {
		ImportBankStatementService ibsService = new ImportBankStatementService(document);
		ImportedTransaction transaction = new ImportedTransactionRabobankCsv("from", "fromName",
				createAmount(123), DateUtil.createDate(2011, 8, 23), "to", "toName", "test");
		ibsService.setImportedFromAccount(transaction, configurationService.getAccount(document, "101"));
		ibsService.setImportedToAccount(transaction, configurationService.getAccount(document, "190"));

		writeReadAndCompareDatabase();
	}

	/**
	 * Writes the database to a temporary file.
	 * Reads back the database. Check that the two databases are equal.
	 * @throws Exception
	 */
	private void writeReadAndCompareDatabase() throws Exception {
		ServiceTransaction.withoutResult(() -> {
			File file = File.createTempFile("ga-test", ".xml");
			XMLFileWriter writer = new XMLFileWriter(document, file);
			writer.writeDatabaseToFile();

			XMLFileReader reader = new XMLFileReader(file);
			Document newDocument = reader.createDatabaseFromFile();

			assertEqualDatabase(document, newDocument);
			assertEquals(file.getAbsolutePath(), newDocument.getFileName());
		});
	}

}
