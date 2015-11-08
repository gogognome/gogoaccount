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
import static junit.framework.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.services.ImportBankStatementService;
import nl.gogognome.gogoaccount.services.importers.ImportedTransaction;
import nl.gogognome.gogoaccount.services.importers.RabobankCSVImporter;
import nl.gogognome.gogoaccount.services.importers.TransactionImporter;
import nl.gogognome.lib.util.DateUtil;

import org.junit.Before;
import org.junit.Test;


/**
 * Tests the ImportBankStatementService.
 *
 * @author Sander Kooijmans
 */
public class TestImportBankStatementService extends AbstractBookkeepingTest {

	private ImportBankStatementService ibsService;

	@Before
	public void initService() {
		ibsService = new ImportBankStatementService(database);
	}

	@Test
	public void testGettersFromImportedTransaction() throws Exception {
		List<ImportedTransaction> transactions = importRabobankTransactions(
				"'0170059286','EUR',20030111,'C',450.00,'P0063925','FIRMA JANSSEN',20030110,'','','REFUND VAN 16-12-2002','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);

		assertEquals(createAmount(450), it.getAmount());
		assertEqualDayOfYear(DateUtil.createDate(2003, 1, 10), it.getDate());
		assertEquals("0170059286", it.getToAccount());
		assertNull(it.getToName());
		assertEquals("P0063925", it.getFromAccount());
		assertEquals("FIRMA JANSSEN", it.getFromName());
	}

	@Test
	public void setAndGetFromAccount() throws Exception {
		Account account100 = database.getAccount("100");
		Account account101 = database.getAccount("101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
			"'0170059286','EUR',20030111,'C',450.00,'P0063925','FIRMA JANSSEN',20030110,'','','REFUND VAN 16-12-2002','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);

		assertNull(ibsService.getFromAccount(it));
		ibsService.setImportedFromAccount(it, account101);
		assertEquals(account101, ibsService.getFromAccount(it));

		ibsService.setImportedFromAccount(it, account100);
		assertEquals(account100, ibsService.getFromAccount(it));
	}

	@Test
	public void setAndGetToAccount() throws Exception {
		Account account100 = database.getAccount("100");
		Account account101 = database.getAccount("101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
			"'0170059286','EUR',20030111,'C',450.00,'P0063925','FIRMA JANSSEN',20030110,'','','REFUND VAN 16-12-2002','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);

		assertNull(ibsService.getToAccount(it));
		ibsService.setImportedToAccount(it, account101);
		assertEquals(account101, ibsService.getToAccount(it));

		ibsService.setImportedToAccount(it, account100);
		assertEquals(account100, ibsService.getToAccount(it));
	}

	@Test
	public void setAndGetFromAccountWithUnknownAccount() throws Exception {
		Account account100 = database.getAccount("100");
		Account account101 = database.getAccount("101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
			"'0170059308','EUR',20030105,'C',9550.00,'0000000000','STORTING',20030103,'','','','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);
		assertNull(it.getFromAccount());

		assertNull(ibsService.getFromAccount(it));
		ibsService.setImportedFromAccount(it, account101);
		assertEquals(account101, ibsService.getFromAccount(it));

		ibsService.setImportedFromAccount(it, account100);
		assertEquals(account100, ibsService.getFromAccount(it));
	}

	@Test
	public void setAndGetToAccountWithUnknownAccount() throws Exception {
		Account account100 = database.getAccount("100");
		Account account101 = database.getAccount("101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
			"'0000000000','EUR',20030111,'C',450.00,'P0063925','FIRMA JANSSEN',20030110,'','','REFUND VAN 16-12-2002','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);
		assertNull(it.getToAccount());

		assertNull(ibsService.getToAccount(it));
		ibsService.setImportedToAccount(it, account101);
		assertEquals(account101, ibsService.getToAccount(it));

		ibsService.setImportedToAccount(it, account100);
		assertEquals(account100, ibsService.getToAccount(it));
	}

	private List<ImportedTransaction> importRabobankTransactions(String... lines) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (String l : lines) {
			sb.append(l.replace('\'', '"')).append('\n');
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes("utf-8"));
		Reader reader = new InputStreamReader(bais);
		try {
			TransactionImporter importer = new RabobankCSVImporter();
			return importer.importTransactions(reader);
		} finally {
			reader.close();
		}
	}
}
