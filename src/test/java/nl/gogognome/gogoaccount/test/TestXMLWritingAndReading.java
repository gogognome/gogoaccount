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
package nl.gogognome.gogoaccount.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ImportBankStatementService;
import nl.gogognome.gogoaccount.services.XMLFileReader;
import nl.gogognome.gogoaccount.services.XMLFileWriter;
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

	private BookkeepingService bookkeepingService = new BookkeepingService();
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
		ImportBankStatementService ibsService = new ImportBankStatementService(database);
		ImportedTransaction transaction = new ImportedTransactionRabobankCsv("from", "fromName",
				createAmount(123), DateUtil.createDate(2011, 8, 23), "to", "toName", "test");
		ibsService.setImportedFromAccount(transaction, bookkeepingService.getAccount(database, "101"));
		ibsService.setImportedToAccount(transaction, bookkeepingService.getAccount(database, "190"));

		writeReadAndCompareDatabase();
	}

	/**
	 * Writes the database to a temporary file.
	 * Reads back the database. Check that the two databases are equal.
	 * @throws Exception
	 */
	private void writeReadAndCompareDatabase() throws Exception {
		File file = File.createTempFile("ga-test", ".xml");
		XMLFileWriter writer = new XMLFileWriter(database, file);
		writer.writeDatabaseToFile();

		XMLFileReader reader = new XMLFileReader(file);
		Database newDatabase = reader.createDatabaseFromFile();

		assertEqualDatabase(database, newDatabase);
		assertEquals(file.getAbsolutePath(), newDatabase.getFileName());
	}

}
