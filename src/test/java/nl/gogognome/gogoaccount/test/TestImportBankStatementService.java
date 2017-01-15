package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.importer.ImportedTransaction;
import nl.gogognome.gogoaccount.component.importer.RabobankCSVImporter;
import nl.gogognome.gogoaccount.component.importer.TransactionImporter;
import nl.gogognome.gogoaccount.test.builders.AmountBuilder;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class TestImportBankStatementService extends AbstractBookkeepingTest {

	@Test
	public void testGettersFromImportedTransaction() throws Exception {
		List<ImportedTransaction> transactions = importRabobankTransactions(
				"'0170059286','EUR',20030111,'C',450.00,'P0063925','FIRMA JANSSEN',20030110,'','','REFUND VAN 16-12-2002','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);

		assertEquals(AmountBuilder.build(450), it.getAmount());
		assertEqualDayOfYear(DateUtil.createDate(2003, 1, 11), it.getDate());
		assertEquals("0170059286", it.getToAccount());
		assertNull(it.getToName());
		assertEquals("P0063925", it.getFromAccount());
		assertEquals("FIRMA JANSSEN", it.getFromName());
	}

	@Test
	public void setAndGetFromAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
			"'0170059286','EUR',20030111,'C',450.00,'P0063925','FIRMA JANSSEN',20030110,'','','REFUND VAN 16-12-2002','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);

		assertNull(importBankStatementService.getFromAccount(document, it));
		importBankStatementService.setImportedFromAccount(document, it, account101);
		assertEquals(account101, importBankStatementService.getFromAccount(document, it));

		importBankStatementService.setImportedFromAccount(document, it, account100);
		assertEquals(account100, importBankStatementService.getFromAccount(document, it));
	}

	@Test
	public void setAndGetToAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
			"'0170059286','EUR',20030111,'C',450.00,'P0063925','FIRMA JANSSEN',20030110,'','','REFUND VAN 16-12-2002','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);

		assertNull(importBankStatementService.getToAccount(document, it));
		importBankStatementService.setImportedToAccount(document, it, account101);
		assertEquals(account101, importBankStatementService.getToAccount(document, it));

		importBankStatementService.setImportedToAccount(document, it, account100);
		assertEquals(account100, importBankStatementService.getToAccount(document, it));
	}

	@Test
	public void setAndGetFromAccountWithUnknownAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
			"'0170059308','EUR',20030105,'C',9550.00,'0000000000','STORTING',20030103,'','','','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);
		assertNull(it.getFromAccount());

		assertNull(importBankStatementService.getFromAccount(document, it));
		importBankStatementService.setImportedFromAccount(document, it, account101);
		assertEquals(account101, importBankStatementService.getFromAccount(document, it));

		importBankStatementService.setImportedFromAccount(document, it, account100);
		assertEquals(account100, importBankStatementService.getFromAccount(document, it));
	}

	@Test
	public void setAndGetToAccountWithUnknownAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
			"'0000000000','EUR',20030111,'C',450.00,'P0063925','FIRMA JANSSEN',20030110,'','','REFUND VAN 16-12-2002','','','','','','','',''");
		ImportedTransaction it = transactions.get(0);
		assertNull(it.getToAccount());

		assertNull(importBankStatementService.getToAccount(document, it));
		importBankStatementService.setImportedToAccount(document, it, account101);
		assertEquals(account101, importBankStatementService.getToAccount(document, it));

		importBankStatementService.setImportedToAccount(document, it, account100);
		assertEquals(account100, importBankStatementService.getToAccount(document, it));
	}

	private List<ImportedTransaction> importRabobankTransactions(String... lines) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (String l : lines) {
			sb.append(l.replace('\'', '"')).append('\n');
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes("utf-8"));
		try (Reader reader = new InputStreamReader(bais)) {
			TransactionImporter importer = new RabobankCSVImporter();
			return importer.importTransactions(reader);
		}
	}
}
