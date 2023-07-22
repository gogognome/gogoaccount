package nl.gogognome.gogoaccount.test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import org.junit.jupiter.api.*;
import nl.gogognome.gogoaccount.component.configuration.*;
import nl.gogognome.gogoaccount.component.importer.*;
import nl.gogognome.gogoaccount.test.builders.*;
import nl.gogognome.lib.util.*;

public class ImportBankStatementServiceTest extends AbstractBookkeepingTest {

	private static final String RABOBANK_HEADER = "'IBAN/BBAN','Munt','BIC','Volgnr','Datum','Rentedatum','Bedrag','Saldo na trn','Tegenrekening IBAN/BBAN','Naam tegenpartij','Naam uiteindelijke partij','Naam initiërende partij','BIC tegenpartij','Code','Batch ID','Transactiereferentie','Machtigingskenmerk','Incassant ID','Betalingskenmerk','Omschrijving-1','Omschrijving-2','Omschrijving-3','Reden retour','Oorspr bedrag','Oorspr munt','Koers'";

	private static final String KNAB_INDICATOR = "KNAB EXPORT;;;;;;;;;;;;;;;;";
	private static final String KNAB_HEADER = "Rekeningnummer;Transactiedatum;Valutacode;CreditDebet;Bedrag;Tegenrekeningnummer;Tegenrekeninghouder;Valutadatum;Betaalwijze;Omschrijving;Type betaling;Machtigingsnummer;Incassant ID;Adres;Referentie;Boekdatum;";

	@Test
	public void rabobank_testAmountIsAlwaysPositiveInTransaction() throws Exception {
		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','-3,50','+616,86','NL98RABO9876543210','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''",
				"'NL01RABO0123456789','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','3,50','+616,86','NL98RABO9876543210','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Returned allowance',' ','','','','',''");

		assertThat(transactions)
				.extracting("fromName", "fromAccount", "amount", "toName", "toAccount")
				.containsExactly(
						tuple(null, "NL01RABO0123456789", AmountBuilder.build("3.50"), "Piet Puk", "NL98RABO9876543210"),
						tuple("Piet Puk", "NL98RABO9876543210", AmountBuilder.build("3.50"), null, "NL01RABO0123456789"));
	}

	@Test
	public void knab_testAmountIsAlwaysPositiveInTransaction() throws Exception {
		List<ImportedTransaction> transactions = importKnabTransactions(
				KNAB_INDICATOR,
				KNAB_HEADER,
				"'NL01RABO0123456789';'08-07-2023';'EUR';'D';'35';'NL98RABO9876543210';'Piet Puk';'30-12-2022';'Overboeking';'Allowance';'';'';'';'';'CSAFD23XSHJ';'08-07-2023';",
				"'NL01RABO0123456789';'08-07-2023';'EUR';'C';'0,35';'NL98RABO9876543210';'Piet Puk';'13-11-2022';'Ontvangen betaling';'Returned allowance';'';'';'';'';'C342798ASDA';'08-07-2023';");

		assertThat(transactions)
				.extracting("fromName", "fromAccount", "amount", "toName", "toAccount")
				.containsExactly(
						tuple(null, "NL01RABO0123456789", AmountBuilder.build("35"), "Piet Puk", "NL98RABO9876543210"),
						tuple("Piet Puk", "NL98RABO9876543210", AmountBuilder.build("0.35"), null, "NL01RABO0123456789"));
	}

	@Test
	public void rabobank_testGettersFromImportedTransaction() throws Exception {
		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','-3,50','+616,86','NL98RABO9876543210','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''");
		ImportedTransaction transaction = transactions.get(0);

		assertEquals(AmountBuilder.build("3.50"), transaction.amount());
		assertEqualDayOfYear(DateUtil.createDate(2023, 7, 8), transaction.date());
		assertEquals("NL98RABO9876543210", transaction.toAccount());
		assertEquals("Piet Puk", transaction.toName());
		assertEquals("NL01RABO0123456789", transaction.fromAccount());
		assertNull(transaction.fromName());
	}

	@Test
	public void knab_testGettersFromImportedTransaction() throws Exception {
		List<ImportedTransaction> transactions = importKnabTransactions(
				KNAB_INDICATOR,
				KNAB_HEADER,
				"'NL01RABO0123456789';'08-07-2023';'EUR';'D';'3,5';'NL98RABO9876543210';'Piet Puk';'30-12-2022';'Overboeking';'Allowance';'';'';'';'';'CSAFD23XSHJ';'08-07-2023';");
		ImportedTransaction transaction = transactions.get(0);

		assertEquals(AmountBuilder.build("3.50"), transaction.amount());
		assertEqualDayOfYear(DateUtil.createDate(2023, 7, 8), transaction.date());
		assertEquals("NL98RABO9876543210", transaction.toAccount());
		assertEquals("Piet Puk", transaction.toName());
		assertEquals("NL01RABO0123456789", transaction.fromAccount());
		assertNull(transaction.fromName());
	}

	@Test
	public void knab_testGettersFromImportedTransactionWithoutToAccount() throws Exception {
		List<ImportedTransaction> transactions = importKnabTransactions(
				KNAB_INDICATOR,
				KNAB_HEADER,
				"'NL01RABO0123456789';'08-07-2023';'EUR';'D';'3,5';' ';'Piet Puk';'30-12-2022';'Overboeking';'allowance';'';'';'';'';'CSAFD23XSHJ';'08-07-2023';");
		ImportedTransaction transaction = transactions.get(0);

		assertEquals(AmountBuilder.build("3.50"), transaction.amount());
		assertEqualDayOfYear(DateUtil.createDate(2023, 7, 8), transaction.date());
		assertNull(transaction.toAccount());
		assertEquals("Piet Puk", transaction.toName());
		assertEquals("NL01RABO0123456789", transaction.fromAccount());
		assertNull(transaction.fromName());
	}

	@Test
	public void knab_testFileWithoutIndicator() throws Exception {
		String line = "'NL01RABO0123456789';'08-07-2023';'EUR';'D';'3,5';'NL98RABO9876543210';'Piet Puk';'30-12-2022';'Overboeking';'Allowance';'';'';'';'';'CSAFD23XSHJ';'08-07-2023';";
		assertThatThrownBy(() -> importKnabTransactions(KNAB_HEADER, line))
				.isInstanceOf(ParseException.class)
				.hasMessage("The file does not start with the indicator 'KNAB EXPORT'");
	}

	@Test
	public void rabobank_setAndGetFromAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','-3,50','+616,86','NL98RABO9876543210','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''");
		ImportedTransaction transaction = transactions.get(0);

		assertNull(importBankStatementService.getFromAccount(document, transaction));
		importBankStatementService.setImportedFromAccount(document, transaction, account101);
		assertEquals(account101, importBankStatementService.getFromAccount(document, transaction));

		importBankStatementService.setImportedFromAccount(document, transaction, account100);
		assertEquals(account100, importBankStatementService.getFromAccount(document, transaction));
	}

	@Test
	public void setAndGetToAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','-3,50','+616,86','NL98RABO9876543210','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''");
		ImportedTransaction transaction = transactions.get(0);

		assertNull(importBankStatementService.getToAccount(document, transaction));
		importBankStatementService.setImportedToAccount(document, transaction, account101);
		assertEquals(account101, importBankStatementService.getToAccount(document, transaction));

		importBankStatementService.setImportedToAccount(document, transaction, account100);
		assertEquals(account100, importBankStatementService.getToAccount(document, transaction));
	}

	@Test
	public void setAndGetFromAccountWithUnknownAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','50,00','+616,86','','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''");
		ImportedTransaction transaction = transactions.get(0);
		assertNull(transaction.fromAccount());

		assertNull(importBankStatementService.getFromAccount(document, transaction));
		importBankStatementService.setImportedFromAccount(document, transaction, account101);
		assertEquals(account101, importBankStatementService.getFromAccount(document, transaction));

		importBankStatementService.setImportedFromAccount(document, transaction, account100);
		assertEquals(account100, importBankStatementService.getFromAccount(document, transaction));
	}

	@Test
	public void setAndGetToAccountWithUnknownAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','50,00','+616,86','','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''");
		ImportedTransaction it = transactions.get(0);
		assertNull(it.toAccount());

		assertNull(importBankStatementService.getToAccount(document, it));
		importBankStatementService.setImportedToAccount(document, it, account101);
		assertEquals(account101, importBankStatementService.getToAccount(document, it));

		importBankStatementService.setImportedToAccount(document, it, account100);
		assertEquals(account100, importBankStatementService.getToAccount(document, it));
	}

	@Test
	public void setMultipleValuesWithSameAccountAndDifferentDescriptions_getAccountForSameAccountAndSimilarDescription_returnsMostSpecificAccount() throws Exception {
		Account account1 = configurationService.getAccount(document, "100");
		Account account2 = configurationService.getAccount(document, "101");

		ImportedTransaction transaction1 = TdbImportedTransaction.aNew()
				.withDescription("Factuur JANUARI 2022")
				.build();
		ImportedTransaction transaction2 = TdbImportedTransaction.aNew()
				.withDescription("ONKOSTENVERGOEDING 09-07-2023")
				.build();

		importBankStatementService.setImportedFromAccount(document, transaction1, account1);
		importBankStatementService.setImportedFromAccount(document, transaction2, account2);

		ImportedTransaction transaction3 = TdbImportedTransaction.aNew()
				.withDescription("Factuur FEBRUARI 2022")
				.build();
		ImportedTransaction transaction4 = TdbImportedTransaction.aNew()
				.withDescription("onkostenvergoeding 10-08-2023")
				.build();

		assertThat(importBankStatementService.getFromAccount(document, transaction3)).isEqualTo(account1);
		assertThat(importBankStatementService.getFromAccount(document, transaction4)).isEqualTo(account2);
	}

	@Test
	public void setValuesWithEmptyDescription_getAccountForSameAccountAndEmptyDescription_returnsMostSpecificAccount() throws Exception {
		Account account1 = configurationService.getAccount(document, "100");
		Account account2 = configurationService.getAccount(document, "101");

		ImportedTransaction transaction1 = TdbImportedTransaction.aNew()
				.withDescription(null)
				.build();
		ImportedTransaction transaction2 = TdbImportedTransaction.aNew()
				.withDescription("ONKOSTENVERGOEDING 09-07-2023")
				.build();

		importBankStatementService.setImportedFromAccount(document, transaction1, account1);
		importBankStatementService.setImportedFromAccount(document, transaction2, account2);

		ImportedTransaction transaction3 = TdbImportedTransaction.aNew()
				.withDescription(null)
				.build();
		ImportedTransaction transaction4 = TdbImportedTransaction.aNew()
				.withDescription("onkostenvergoeding 10-08-2023")
				.build();

		assertThat(importBankStatementService.getFromAccount(document, transaction3)).isEqualTo(account1);
		assertThat(importBankStatementService.getFromAccount(document, transaction4)).isEqualTo(account2);
	}

	@Test
	public void setMultipleValuesWithSameAccountAndDifferentDescriptions_getAccountForSameAccountButDifferentDescription_returnsLastRecordedAccount() throws Exception {
		Account account1 = configurationService.getAccount(document, "100");
		Account account2 = configurationService.getAccount(document, "101");

		ImportedTransaction transaction1 = TdbImportedTransaction.aNew()
				.withDescription("Factuur JANUARI 2022")
				.build();
		ImportedTransaction transaction2 = TdbImportedTransaction.aNew()
				.withDescription("ONKOSTENVERGOEDING 09-07-2023")
				.build();

		importBankStatementService.setImportedFromAccount(document, transaction1, account1);
		importBankStatementService.setImportedFromAccount(document, transaction2, account2);

		ImportedTransaction transaction3 = TdbImportedTransaction.aNew()
				.withDescription("Contributie 2023")
				.build();

		assertThat(importBankStatementService.getFromAccount(document, transaction3)).isEqualTo(account2);
	}

	private List<ImportedTransaction> importRabobankTransactions(String... lines) throws Exception {
		String fileAsString = buidlFilesAsString(lines);

		return new RabobankCSVImporter() {
			@Override
			protected Reader getReader(File file, Charset charset) throws IOException {
				ByteArrayInputStream bais = new ByteArrayInputStream(fileAsString.getBytes(charset));
				return new InputStreamReader(bais);
			}
		}.importTransactions(null);
	}

	private List<ImportedTransaction> importKnabTransactions(String... lines) throws Exception {
		String fileAsString = buidlFilesAsString(lines);

		return new KnabCSVImporter() {
			@Override
			protected Reader getReader(File file, Charset charset) throws IOException {
				ByteArrayInputStream bais = new ByteArrayInputStream(fileAsString.getBytes(charset));
				return new InputStreamReader(bais);
			}
		}.importTransactions(null);
	}

	private static String buidlFilesAsString(String[] lines) {
		StringBuilder sb = new StringBuilder();
		for (String l : lines) {
			sb.append(l.replace('\'', '"')).append('\n');
		}
		String fileAsString = sb.toString();
		return fileAsString;
	}
}
