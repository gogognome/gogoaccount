package nl.gogognome.gogoaccount.test;

import static org.assertj.core.api.Assertions.*;
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
	private static final String RABOBANK_BUSINESS_HEADER = "'IBAN/BBAN','Valuta','BIC','Volg nr','Datum','Valuta datum','Bedrag','saldo na boeking','IBAN/BBAN tegenpartij','Naam tegenpartij','Naam uiteind begunst','Naam initiërende partij','BIC tegenpartij','Transactie soort','Batch nr','Transactiereferentie','Machtigingskenmerk','Incassant ID','Betalingskenmerk','Omschrijving - 1','Omschrijving - 2','Omschrijving - 3','Oorzaakscode','Oorspr bedrag','Oorspr valuta','Koers','Naam rekeninghouder','Naam adm gebruik','Omschrijving oorzaakscode','Ref.correspondentbank','Transactiereferentie','Trans.Info.Extra.Details','Trans.Info.Extra.Details.Type','Boekingsreferentie','Veld 86'";

	private static final String RABOBANK_CREDITCARD_HEADER = "'Tegenrekening IBAN','Munt','Creditcard Nummer','Productnaam','Creditcard Regel1','Creditcard Regel2','Transactiereferentie','Datum','Bedrag','Omschrijving','Oorspr bedrag','Oorspr munt','Koers'";

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
	public void rabobankBusiness_testAmountIsAlwaysPositiveInTransaction() throws Exception {
		List<ImportedTransaction> transactions = importRabobankBusinessTransactions(
				RABOBANK_BUSINESS_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2UXXX','','02-09-2023','01-09-2023','-3,50','1234.56','NL98RABO9876543210','Piet Puk','','','','db','','','','','','Allowance','','','','','','','My Club','','','','','','','',''",
				"'NL01RABO0123456789','EUR','RABONL2UXXX','','02-09-2023','01-09-2023','3,50','1234.56','NL98RABO9876543210','Piet Puk','','','','db','','','','','','Returned allowance','','','','','','','My Club','','','','','','','',''");

		assertThat(transactions)
				.extracting("fromName", "fromAccount", "amount", "toName", "toAccount")
				.containsExactly(
						tuple("My Club", "NL01RABO0123456789", AmountBuilder.build("3.50"), "Piet Puk", "NL98RABO9876543210"),
						tuple("Piet Puk", "NL98RABO9876543210", AmountBuilder.build("3.50"), "My Club", "NL01RABO0123456789"));
	}

	@Test
	public void rabobankCreditcard_testAmountIsAlwaysPositiveInTransaction() throws Exception {
		List<ImportedTransaction> transactions = importRabobankCreditcardTransactions(
				RABOBANK_CREDITCARD_HEADER,
				"'NL01RABO0123456789','EUR','1234','RaboCard Mastercard','P. Puk','','0021000000001','2023-07-22','-4,46','PAYPAL *PATREON  MEMBE   123-456-7890 USACA','','',''",
				"'NL01RABO0123456789','EUR','1234','RaboCard Mastercard','P. Puk','','0021000000002','2023-07-23','+7,45','Verrekening vorig overzicht','','',''");

		assertThat(transactions)
				.extracting("fromName", "fromAccount", "amount", "toName", "toAccount")
				.containsExactly(
						tuple(null, "RaboCard Mastercard 1234", AmountBuilder.build("4.46"), null, null),
						tuple(null, null, AmountBuilder.build("7.45"), null, "RaboCard Mastercard 1234"));
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
	public void abnAmroBankTsv_testAmountIsAlwaysPositiveInTransaction() throws Exception {
		List<ImportedTransaction> transactions = importAbnAmroBankTsvTransactions(
				"123456789\tEUR\t20220902\t5121,54\t4904,54\t20220902\t-217,00\tSEPA Incasso algemeen doorlopend Incassant: NL12345678901234567  Naam: Piet Puk                   Machtiging: 000123792531        Omschrijving: Allowance                                                                    IBAN: NL12ABNA3456789012                                         ",
				"123456789\tEUR\t20220902\t4904,54\t5121,54\t20220902\t216,35\tSEPA Incasso algemeen doorlopend Incassant: NL12345678901234567   Naam: Piet Puk                   Machtiging: 000123792531        Omschrijving: Returned allowance                                                           IBAN: NL12ABNA3456789012                                         ");

		assertThat(transactions)
				.extracting("fromName", "fromAccount", "amount", "toName", "toAccount")
				.containsExactly(
						tuple(null, "123456789", AmountBuilder.build("217"), "Piet Puk", "NL12ABNA3456789012"),
						tuple("Piet Puk", "NL12ABNA3456789012", AmountBuilder.build("216.35"), null, "123456789"));
	}

	@Test
	public void rabobank_testGettersFromImportedTransaction() throws Exception {
		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','-3,50','+616,86','NL98RABO9876543210','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("3.50"));
		assertEqualDayOfYear(DateUtil.createDate(2023, 7, 8), transaction.date());
		assertThat(transaction.toAccount()).isEqualTo("NL98RABO9876543210");
		assertThat(transaction.toName()).isEqualTo("Piet Puk");
		assertThat(transaction.fromAccount()).isEqualTo("NL01RABO0123456789");
		assertThat(transaction.fromName()).isNull();
	}

	@Test
	public void rabobankBusiness_testGettersFromImportedTransaction() throws Exception {
		List<ImportedTransaction> transactions = importRabobankBusinessTransactions(
				RABOBANK_BUSINESS_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2UXXX','','02-09-2023','01-09-2023','-1.234,56','1234.56','NL98RABO9876543210','Piet Puk','','','','db','','','','','','Allowance','','','','','','','My Club','','','','','','','',''");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("1234.56"));
		assertEqualDayOfYear(DateUtil.createDate(2023, 9, 2), transaction.date());
		assertThat(transaction.toAccount()).isEqualTo("NL98RABO9876543210");
		assertThat(transaction.toName()).isEqualTo("Piet Puk");
		assertThat(transaction.fromAccount()).isEqualTo("NL01RABO0123456789");
		assertThat(transaction.fromName()).isEqualTo("My Club");
	}

	@Test
	public void rabobankCreditcard_testGettersFromImportedTransaction() throws Exception {
		List<ImportedTransaction> transactions = importRabobankCreditcardTransactions(
				RABOBANK_CREDITCARD_HEADER,
				"'NL01RABO0123456789','EUR','1234','RaboCard Mastercard','P. Puk','','0021000000001','2023-07-22','-4,46','Online shop invoice','','',''");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("4.46"));
		assertEqualDayOfYear(DateUtil.createDate(2023, 7, 22), transaction.date());
		assertThat(transaction.toAccount()).isNull();
		assertThat(transaction.toName()).isNull();
		assertThat(transaction.fromAccount()).isEqualTo("RaboCard Mastercard 1234");
		assertThat(transaction.fromName()).isNull();
	}

	@Test
	public void knab_testGettersFromImportedTransaction() throws Exception {
		List<ImportedTransaction> transactions = importKnabTransactions(
				KNAB_INDICATOR,
				KNAB_HEADER,
				"'NL01RABO0123456789';'08-07-2023';'EUR';'D';'3,5';'NL98RABO9876543210';'Piet Puk';'30-12-2022';'Overboeking';'Allowance';'';'';'';'';'CSAFD23XSHJ';'08-07-2023';");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("3.50"));
		assertEqualDayOfYear(DateUtil.createDate(2023, 7, 8), transaction.date());
		assertThat(transaction.toAccount()).isEqualTo("NL98RABO9876543210");
		assertThat(transaction.toName()).isEqualTo("Piet Puk");
		assertThat(transaction.fromAccount()).isEqualTo("NL01RABO0123456789");
		assertThat(transaction.fromName()).isNull();
	}

	@Test
	public void abnAmroBankTsv_testGettersFromImportedTransaction() throws Exception {
		List<ImportedTransaction> transactions = importAbnAmroBankTsvTransactions(
				"123456789\tEUR\t20220324\t7504,55\t7301,27\t20220324\t-203,28\tSEPA Overboeking                 IBAN: NL12ABNA3456789012        BIC: ABNANL2A                    Naam: Piet Puk                                                   Omschrijving: Invoice 1234 03-2022                               Kenmerk: abcd98765432");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("203.28"));
		assertEqualDayOfYear(DateUtil.createDate(2022, 3, 24), transaction.date());
		assertThat(transaction.toAccount()).isEqualTo("NL12ABNA3456789012");
		assertThat(transaction.toName()).isEqualTo("Piet Puk");
		assertThat(transaction.fromAccount()).isEqualTo("123456789");
		assertThat(transaction.fromName()).isNull();
	}

	@Test
	public void abnAmroBankTsv_testGettersFromImportedTransaction_slashesInDescription() throws Exception {
		List<ImportedTransaction> transactions = importAbnAmroBankTsvTransactions(
				"123456789\tEUR\t20220301\t6317,08\t7992,09\t20220301\t1675,01\t/TRTP/SEPA OVERBOEKING/IBAN/NL12ABNA3456789012/BIC/ABNANL2A/NAME/Piet Puk/REMI/5087/5781/EREF/NOTPROVIDED                  ");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("1675.01"));
		assertEqualDayOfYear(DateUtil.createDate(2022, 3, 1), transaction.date());
		assertThat(transaction.toAccount()).isEqualTo("123456789");
		assertThat(transaction.toName()).isNull();
		assertThat(transaction.fromAccount()).isEqualTo("NL12ABNA3456789012");
		assertThat(transaction.fromName()).isEqualTo("Piet Puk");
		assertThat(transaction.description()).isEqualTo("5087");
	}

	@Test
	public void abnAmroBankTsv_testGettersFromImportedTransaction_slashesInDescriptionIncludingReference() throws Exception {
		List<ImportedTransaction> transactions = importAbnAmroBankTsvTransactions(
				"123456789\tEUR\t20230628\t5517,94\t5251,94\t20230628\t-266,00\t/TRTP/SEPA Incasso algemeen doorlopend/CSID/NL12ZZZ345678890000/NAME/Piet Puk/MARF/12345678/REMI/Kenmerk  9821386747562123 Omschrijving  Klantnummer  54234523-3 BTW  46.40 Periode  2023-06 Sesamstraat 32 HILVERSUM/IBAN/NL12ABNA3456789012/BIC/ABNANL2A /EREF/9435843851231251");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("266"));
		assertEqualDayOfYear(DateUtil.createDate(2023, 6, 28), transaction.date());
		assertThat(transaction.toAccount()).isEqualTo("NL12ABNA3456789012");
		assertThat(transaction.toName()).isEqualTo("Piet Puk");
		assertThat(transaction.fromAccount()).isEqualTo("123456789");
		assertThat(transaction.fromName()).isNull();
		assertThat(transaction.description()).isEqualTo("Kenmerk 9821386747562123 Omschrijving Klantnummer 54234523-3 BTW 46.40 Periode 2023-06 Sesamstraat 32 HILVERSUM 9435843851231251");
	}

	@Test
	public void abnAmroBankTsv_testGettersFromImportedTransaction_slashesInDescriptionExcludingActualDescriptionAndReference() throws Exception {
		List<ImportedTransaction> transactions = importAbnAmroBankTsvTransactions(
				"123456789\tEUR\t20230704\t4866,61\t6541,62\t20230704\t1675,01\t/TRTP/SEPA Incasso Batch/PREF/5/NRTX/0000011/PIND/BRUTO/");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("1675.01"));
		assertEqualDayOfYear(DateUtil.createDate(2023, 7, 4), transaction.date());
		assertThat(transaction.toAccount()).isEqualTo("123456789");
		assertThat(transaction.toName()).isNull();
		assertThat(transaction.fromAccount()).isNull();
		assertThat(transaction.fromName()).isNull();
		assertThat(transaction.description()).isEqualTo("/TRTP/SEPA Incasso Batch/PREF/5/NRTX/0000011/PIND/BRUTO/");
	}

	@Test
	public void abnAmroBankTsv_testGettersFromImportedTransaction_noNameInDescription() throws Exception {
		List<ImportedTransaction> transactions = importAbnAmroBankTsvTransactions(
				"123456789\tEUR\t20220301\t6317,08\t7992,09\t20220301\t1675,01\tSEPA Incasso Batch               Referentie: tbob1234567-RCUR-brek987654                          Aantal opdrachten: 23");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("1675.01"));
		assertEqualDayOfYear(DateUtil.createDate(2022, 3, 1), transaction.date());
		assertThat(transaction.toAccount()).isEqualTo("123456789");
		assertThat(transaction.toName()).isNull();
		assertThat(transaction.fromAccount()).isNull();
		assertThat(transaction.fromName()).isNull();
		assertThat(transaction.description()).isEqualTo("SEPA Incasso Batch Referentie: tbob1234567-RCUR-brek987654 Aantal opdrachten: 23");
	}

	@Test
	public void knab_testGettersFromImportedTransactionWithoutToAccount() throws Exception {
		List<ImportedTransaction> transactions = importKnabTransactions(
				KNAB_INDICATOR,
				KNAB_HEADER,
				"'NL01RABO0123456789';'08-07-2023';'EUR';'D';'3,5';' ';'Piet Puk';'30-12-2022';'Overboeking';'allowance';'';'';'';'';'CSAFD23XSHJ';'08-07-2023';");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(transaction.amount()).isEqualTo(AmountBuilder.build("3.50"));
		assertEqualDayOfYear(DateUtil.createDate(2023, 7, 8), transaction.date());
		assertThat(transaction.toAccount()).isNull();
		assertThat(transaction.toName()).isEqualTo("Piet Puk");
		assertThat(transaction.fromAccount()).isEqualTo("NL01RABO0123456789");
		assertThat(transaction.fromName()).isNull();
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

		assertThat(importBankStatementService.getFromAccount(document, transaction)).isNull();
		importBankStatementService.setImportedFromAccount(document, transaction, account101);
		assertThat(importBankStatementService.getFromAccount(document, transaction)).isEqualTo(account101);

		importBankStatementService.setImportedFromAccount(document, transaction, account100);
		assertThat(importBankStatementService.getFromAccount(document, transaction)).isEqualTo(account100);
	}

	@Test
	public void rabobankBusiness_setAndGetFromAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankBusinessTransactions(
				RABOBANK_BUSINESS_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2UXXX','','02-09-2023','01-09-2023','-3,50','1234.56','NL98RABO9876543210','Piet Puk','','','','db','','','','','','Allowance','','','','','','','My Club','','','','','','','',''");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(importBankStatementService.getFromAccount(document, transaction)).isNull();
		importBankStatementService.setImportedFromAccount(document, transaction, account101);
		assertThat(importBankStatementService.getFromAccount(document, transaction)).isEqualTo(account101);

		importBankStatementService.setImportedFromAccount(document, transaction, account100);
		assertThat(importBankStatementService.getFromAccount(document, transaction)).isEqualTo(account100);
	}

	@Test
	public void setAndGetToAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','-3,50','+616,86','NL98RABO9876543210','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''");
		ImportedTransaction transaction = transactions.get(0);

		assertThat(importBankStatementService.getToAccount(document, transaction)).isNull();
		importBankStatementService.setImportedToAccount(document, transaction, account101);
		assertThat(importBankStatementService.getToAccount(document, transaction)).isEqualTo(account101);

		importBankStatementService.setImportedToAccount(document, transaction, account100);
		assertThat(importBankStatementService.getToAccount(document, transaction)).isEqualTo(account100);
	}

	@Test
	public void setAndGetFromAccountWithUnknownAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'NL01RABO0123456789','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','50,00','+616,86','','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''");
		ImportedTransaction transaction = transactions.get(0);
		assertThat(transaction.fromAccount()).isNull();

		assertThat(importBankStatementService.getFromAccount(document, transaction)).isNull();
		importBankStatementService.setImportedFromAccount(document, transaction, account101);
		assertThat(importBankStatementService.getFromAccount(document, transaction)).isEqualTo(account101);

		importBankStatementService.setImportedFromAccount(document, transaction, account100);
		assertThat(importBankStatementService.getFromAccount(document, transaction)).isEqualTo(account100);
	}

	@Test
	public void setAndGetToAccountWithUnknownAccount() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		List<ImportedTransaction> transactions = importRabobankTransactions(
				RABOBANK_HEADER,
				"'','EUR','RABONL2U','000000000000016431','2023-07-08','2023-07-81','50,00','+616,86','','Piet Puk','','Rabobank Nederland APO','RABONL2UXXX','bg','','','','','','Allowance',' ','','','','',''");
		ImportedTransaction it = transactions.get(0);
		assertThat(it.toAccount()).isNull();

		assertThat(importBankStatementService.getToAccount(document, it)).isNull();
		importBankStatementService.setImportedToAccount(document, it, account101);
		assertThat(importBankStatementService.getToAccount(document, it)).isEqualTo(account101);

		importBankStatementService.setImportedToAccount(document, it, account100);
		assertThat(importBankStatementService.getToAccount(document, it)).isEqualTo(account100);
	}

	@Test
	public void setAndGetToAccountWithSameAccountWithDifferentNames() throws Exception {
		Account account100 = configurationService.getAccount(document, "100");
		Account account101 = configurationService.getAccount(document, "101");

		ImportedTransaction it1 = TdbImportedTransaction.aNew()
				.withToAccount("NL98RABO9876543210")
				.withToName("Name 1")
				.build();
		ImportedTransaction it2 = TdbImportedTransaction.aNew()
				.withToAccount("NL98RABO9876543210")
				.withToName("Name 2")
				.build();

		assertThat(importBankStatementService.getToAccount(document, it1)).isNull();
		importBankStatementService.setImportedToAccount(document, it1, account101);
		assertThat(importBankStatementService.getToAccount(document, it1)).isEqualTo(account101);

		assertThat(importBankStatementService.getToAccount(document, it2)).isNull();
		importBankStatementService.setImportedToAccount(document, it2, account100);
		assertThat(importBankStatementService.getToAccount(document, it2)).isEqualTo(account100);
		assertThat(importBankStatementService.getToAccount(document, it1)).isEqualTo(account101);
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

	private List<ImportedTransaction> importRabobankBusinessTransactions(String... lines) throws Exception {
		String fileAsString = buidlFilesAsString(lines);

		return new RabobankBusinessCSVImporter() {
			@Override
			protected Reader getReader(File file, Charset charset) throws IOException {
				ByteArrayInputStream bais = new ByteArrayInputStream(fileAsString.getBytes(charset));
				return new InputStreamReader(bais);
			}
		}.importTransactions(null);
	}

	private List<ImportedTransaction> importRabobankCreditcardTransactions(String... lines) throws Exception {
		String fileAsString = buidlFilesAsString(lines);

		return new RabobankCreditCardCSVImporter() {
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

	private List<ImportedTransaction> importAbnAmroBankTsvTransactions(String... lines) throws Exception {
		String fileAsString = buidlFilesAsString(lines);

		return new AbnAmroBankTSVTransactionImporter() {
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
