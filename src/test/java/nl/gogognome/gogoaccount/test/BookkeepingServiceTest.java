package nl.gogognome.gogoaccount.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Arrays;
import java.util.List;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.businessobjects.AccountType;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import org.junit.Test;


/**
 * Tests the bookkeeping service.
 */
public class BookkeepingServiceTest extends AbstractBookkeepingTest {

	private final BookkeepingService bookkeepingService = new BookkeepingService();
	private final ConfigurationService configurationService = new ConfigurationService();
	private final PartyService partyService = new PartyService();

	@Test
	public void testStartBalance() throws Exception {
		checkAmount(100,
				bookkeepingService.getStartBalance(document, configurationService.getAccount(document, "100")));
		checkAmount(300,
				bookkeepingService.getStartBalance(document, configurationService.getAccount(document, "101")));
		checkAmount(400,
				bookkeepingService.getStartBalance(document, configurationService.getAccount(document, "200")));
	}

	@Test
	public void deleteUnusedAccountSucceeds() throws Exception {
		configurationService.deleteAccount(document, configurationService.getAccount(document, "290"));
		assertAccountDoesNotExist(document, "290");
	}

    @Test
	public void deleteUsedAccountFails() throws Exception {
		try {
			configurationService.deleteAccount(document, configurationService.getAccount(document, "190"));
			fail("Expected exception was not thrown");
		} catch (ServiceException e) {
			assertAccountExists(document, "190");
		}
	}

    private void assertAccountDoesNotExist(Document document, String accountId) throws ServiceException {
        assertTrue(configurationService.findAllAccounts(document).stream().noneMatch(account -> account.getId().equals(accountId)));
    }

    private void assertAccountExists(Document document, String accountId) throws ServiceException {
        assertTrue(configurationService.findAllAccounts(document).stream().anyMatch(account -> account.getId().equals(accountId)));
    }

    @Test
	public void testReportAtEndOf2011() throws Exception {
		Report report = bookkeepingService.createReport(document,
				DateUtil.createDate(2011, 12, 31));

		assertEquals("[100 Kas, 101 Betaalrekening, 190 Debiteuren, " +
				"200 Eigen vermogen, 290 Crediteuren, " +
				"400 Zaalhuur, 490 Onvoorzien, " +
				"300 Contributie, 390 Onvoorzien]",
			report.getAllAccounts().toString());

		assertEquals("[100 Kas, 101 Betaalrekening, 190 Debiteuren]",
			report.getAssetsInclLossAccount().toString());

		assertEquals("[200 Eigen vermogen, 290 Crediteuren,  Winst]",
                report.getLiabilitiesInclProfitAccount().toString());

		checkAmount(20, report.getAmount(new Account("", "", AccountType.LIABILITY)));

		assertEquals("[1101 Pietje Puk]", report.getDebtors().toString());
		assertEquals("[]", report.getCreditors().toString());

        Party party = partyService.getParty(document, "1101");
        checkAmount(10, report.getBalanceForDebtor(party));
		checkAmount(0, report.getBalanceForCreditor(party));

		assertEquals("[ null beginsaldo 30000 null, " +
						"20110510 t2 Payment 1000 null inv1,  " +
						"null totaal mutaties 1000 0,  " +
						"null eindsaldo 31000 null]",
				report.getLedgerLinesForAccount(configurationService.getAccount(document, "101")).toString());

		assertEquals("[ null beginsaldo null 0, " +
				"20110305 t1 Payment null 2000 inv1,  " +
				"null totaal mutaties 0 2000,  " +
				"null eindsaldo null 2000]",
				report.getLedgerLinesForAccount(configurationService.getAccount(document, "300")).toString());

		checkTotalsOfReport(report);
	}

	@Test
	public void testReportApril30_2011() throws Exception {
		Report report = bookkeepingService.createReport(document,
				DateUtil.createDate(2011, 4, 30));

		assertEquals("[100 Kas, 101 Betaalrekening, 190 Debiteuren, " +
				"200 Eigen vermogen, 290 Crediteuren, " +
				"400 Zaalhuur, 490 Onvoorzien, " +
				"300 Contributie, 390 Onvoorzien]",
			report.getAllAccounts().toString());

		assertEquals("[100 Kas, 101 Betaalrekening, 190 Debiteuren]",
			report.getAssetsInclLossAccount().toString());

		assertEquals("[200 Eigen vermogen, 290 Crediteuren,  Winst]",
				report.getLiabilitiesInclProfitAccount().toString());

		checkAmount(20, report.getAmount(new Account("", "", AccountType.LIABILITY)));

		assertEquals("[1101 Pietje Puk]", report.getDebtors().toString());
		assertEquals("[]", report.getCreditors().toString());

        Party party = partyService.getParty(document, "1101");
        checkAmount(20, report.getBalanceForDebtor(party));
		checkAmount(0, report.getBalanceForCreditor(party));

		assertEquals("[ null beginsaldo 30000 null,  " +
						"null totaal mutaties 0 0,  " +
						"null eindsaldo 30000 null]",
				report.getLedgerLinesForAccount(configurationService.getAccount(document, "101")).toString());

		checkTotalsOfReport(report);
	}

	@Test
	public void testCloseBookkeeping() throws Exception {
		Document newDocument = bookkeepingService.closeBookkeeping(document, "new bookkeeping",
				DateUtil.createDate(2012, 1, 1), configurationService.getAccount(document, "200"));
        Bookkeeping newBookkeeping = configurationService.getBookkeeping(newDocument);

		assertEquals("new bookkeeping", newBookkeeping.getDescription());
		assertEquals(configurationService.findAllAccounts(document).toString(), configurationService.findAllAccounts(newDocument).toString());
		assertEquals(0, DateUtil.compareDayOfYear(DateUtil.createDate(2012, 1, 1),
                newBookkeeping.getStartOfPeriod()));

		Report report = bookkeepingService.createReport(newDocument,
				DateUtil.createDate(2011, 12, 31));

		assertEquals("[100 Kas, 101 Betaalrekening, 190 Debiteuren, " +
				"200 Eigen vermogen, 290 Crediteuren, " +
				"400 Zaalhuur, 490 Onvoorzien, " +
				"300 Contributie, 390 Onvoorzien]",
			report.getAllAccounts().toString());

		assertEquals("[100 Kas, 101 Betaalrekening, 190 Debiteuren]",
			report.getAssetsInclLossAccount().toString());

		assertEquals("[200 Eigen vermogen, 290 Crediteuren]",
				report.getLiabilitiesInclProfitAccount().toString());

		assertEquals("[1101 Pietje Puk]", report.getDebtors().toString());
		assertEquals("[]", report.getCreditors().toString());

        Party party = partyService.getParty(document, "1101");
        checkAmount(10, report.getBalanceForDebtor(party));
		checkAmount(0, report.getBalanceForCreditor(party));

		checkAmount(420, report.getAmount(configurationService.getAccount(document, "200")));
		checkAmount(0, report.getAmount(configurationService.getAccount(document, "300")));

		checkTotalsOfReport(report);
	}

	@Test
	public void testCloseBookkeepingWithUnsavedChangesFails() throws Exception {
		List<JournalItem> items = Arrays.asList(
				createItem(20, "100", true),
				createItem(20, "101", false));
		Journal journal = new Journal("ABC", "Test", DateUtil.createDate(2012, 1, 10), items, null);

		document.addJournal(journal, false);
		try {
			bookkeepingService.closeBookkeeping(document, "new bookkeeping",
					DateUtil.createDate(2012, 1, 1), configurationService.getAccount(document, "200"));
			fail("Expected exception was not thrown");
		} catch (ServiceException e) {
		}
	}

	@Test
	public void testCloseBookkeepingWithJournalsCopiedToNewBookkeeping() throws Exception {
		List<JournalItem> items = Arrays.asList(
				createItem(20, "100", true),
				createItem(20, "101", false));
		Journal journal = new Journal("ABC", "Test", DateUtil.createDate(2012, 1, 10), items, null);

		document.addJournal(journal, false);
		document.databaseConsistentWithFile();
		Document newDocument = bookkeepingService.closeBookkeeping(document, "new bookkeeping",
				DateUtil.createDate(2012, 1, 1), configurationService.getAccount(document, "200"));

		assertEquals("[20111231 start start balance, 20120110 ABC Test]",
				newDocument.getJournals().toString());
	}

	@Test
	public void checkInUseForUsedAccount() throws ServiceException {
		assertTrue(bookkeepingService.inUse(document, configurationService.getAccount(document, "190")));
		assertTrue(bookkeepingService.inUse(document, configurationService.getAccount(document, "200")));
	}

	@Test
	public void checkInUseForUnusedAccount() throws ServiceException {
		assertFalse(bookkeepingService.inUse(document, configurationService.getAccount(document, "400")));
	}

	@Test
	public void removeJournalThatCreatesInvoice() throws Exception {
		assertNotNull(findJournal("t1"));
		assertNotNull(document.getInvoice("inv1"));

		bookkeepingService.removeJournal(document, findJournal("t1"));

		assertNull(findJournal("t1"));
		assertNull(document.getInvoice("inv1"));
	}

	@Test
	public void removeJournalWithPayment() throws Exception {
		assertNotNull(findJournal("t2"));
		assertEquals("[20110510 pay1 Betaalrekening]",
				document.getPayments("inv1").toString());

		bookkeepingService.removeJournal(document, findJournal("t2"));

		assertNull(findJournal("t2"));
		assertEquals("[]", document.getPayments("inv1").toString());
	}

	@Test
	public void addNewAccount() throws Exception {
		configurationService.createAccount(document,
                new Account("103", "Spaarrekening", AccountType.ASSET));
		Account a = configurationService.getAccount(document, "103");
		assertEquals("103", a.getId());
		assertEquals("Spaarrekening", a.getName());
		assertEquals(AccountType.ASSET, a.getType());
	}

	@Test
	public void addAccountWithExistingIdFails() throws Exception {
		try {
			configurationService.createAccount(document,
                    new Account("101", "Spaarrekening", AccountType.ASSET));
			fail("Expected exception was not thrown");
		} catch (ServiceException e) {
		}
	}

	@Test
	public void addUpdateAccount() throws Exception {
		configurationService.updateAccount(document,
				new Account("101", "Spaarrekening", AccountType.ASSET));
		Account a = configurationService.getAccount(document, "101");
		assertEquals("101", a.getId());
		assertEquals("Spaarrekening", a.getName());
		assertEquals(AccountType.ASSET, a.getType());
	}

	@Test
	public void updateNonExistingAccountFails() throws Exception {
		try {
			configurationService.updateAccount(document,
					new Account("103", "Spaarrekening", AccountType.ASSET));
			fail("Expected exception was not thrown");
		} catch (ServiceException e) {
		}
	}

	private void checkTotalsOfReport(Report report) {
		assertEquals(report.getTotalAssets(), report.getTotalLiabilities());

		Amount a = report.getTotalExpenses();
		a = a.subtract(report.getTotalRevenues());
		a = a.add(report.getResultOfOperations());
		assertEquals(zero, a);
	}
}
