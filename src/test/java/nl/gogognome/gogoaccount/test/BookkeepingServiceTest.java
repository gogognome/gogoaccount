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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.businessobjects.AccountType;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.database.AccountDAO;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import org.junit.Test;


/**
 * Tests the bookkeeping service.
 *
 * @author Sander Kooijmans
 */
public class BookkeepingServiceTest extends AbstractBookkeepingTest {

	private BookkeepingService bookkeepingService = new BookkeepingService();

	@Test
	public void testStartBalance() throws Exception {
		checkAmount(100,
				bookkeepingService.getStartBalance(database, bookkeepingService.getAccount(database, "100")));
		checkAmount(300,
				bookkeepingService.getStartBalance(database, bookkeepingService.getAccount(database, "101")));
		checkAmount(400,
				bookkeepingService.getStartBalance(database, bookkeepingService.getAccount(database, "200")));
	}

	@Test
	public void deleteUnusedAccountSucceeds() throws Exception {
		bookkeepingService.deleteAccount(database, bookkeepingService.getAccount(database, "290"));
		assertAccountDoesNotExist(database, "290");
	}

    @Test
	public void deleteUsedAccountFails() throws Exception {
		try {
			bookkeepingService.deleteAccount(database, bookkeepingService.getAccount(database, "190"));
			fail("Expected exception was not thrown");
		} catch (ServiceException e) {
			assertAccountExists(database, "190");
		}
	}

    private void assertAccountDoesNotExist(Database database, String accountId) throws ServiceException {
        assertTrue(bookkeepingService.findAllAccounts(database).stream().noneMatch(account -> account.getId().equals(accountId)));
    }

    private void assertAccountExists(Database database, String accountId) throws ServiceException {
        assertTrue(bookkeepingService.findAllAccounts(database).stream().anyMatch(account -> account.getId().equals(accountId)));
    }

    @Test
	public void testReportAtEndOf2011() throws Exception {
		Report report = bookkeepingService.createReport(database,
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

		checkAmount(10, report.getBalanceForDebtor(database.getParty("1101")));
		checkAmount(0, report.getBalanceForCreditor(database.getParty("1101")));

		assertEquals("[ null beginsaldo 30000 null, " +
						"20110510 t2 Payment 1000 null inv1,  " +
						"null totaal mutaties 1000 0,  " +
						"null eindsaldo 31000 null]",
				report.getLedgerLinesForAccount(bookkeepingService.getAccount(database, "101")).toString());

		assertEquals("[ null beginsaldo null 0, " +
				"20110305 t1 Payment null 2000 inv1,  " +
				"null totaal mutaties 0 2000,  " +
				"null eindsaldo null 2000]",
				report.getLedgerLinesForAccount(bookkeepingService.getAccount(database, "300")).toString());

		checkTotalsOfReport(report);
	}

	@Test
	public void testReportApril30_2011() throws Exception {
		Report report = bookkeepingService.createReport(database,
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

		checkAmount(20, report.getBalanceForDebtor(database.getParty("1101")));
		checkAmount(0, report.getBalanceForCreditor(database.getParty("1101")));

		assertEquals("[ null beginsaldo 30000 null,  " +
						"null totaal mutaties 0 0,  " +
						"null eindsaldo 30000 null]",
				report.getLedgerLinesForAccount(bookkeepingService.getAccount(database, "101")).toString());

		checkTotalsOfReport(report);
	}

	@Test
	public void testCloseBookkeeping() throws Exception {
		Database newDatabase = bookkeepingService.closeBookkeeping(database, "new bookkeeping",
				DateUtil.createDate(2012, 1, 1), bookkeepingService.getAccount(database, "200"));

		assertEquals("new bookkeeping", newDatabase.getDescription());
		assertEquals(bookkeepingService.findAllAccounts(database).toString(), bookkeepingService.findAllAccounts(newDatabase).toString());
		assertEquals(0, DateUtil.compareDayOfYear(DateUtil.createDate(2012, 1, 1),
				newDatabase.getStartOfPeriod()));

		Report report = bookkeepingService.createReport(newDatabase,
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

		checkAmount(10, report.getBalanceForDebtor(database.getParty("1101")));
		checkAmount(0, report.getBalanceForCreditor(database.getParty("1101")));

		checkAmount(420, report.getAmount(bookkeepingService.getAccount(database, "200")));
		checkAmount(0, report.getAmount(bookkeepingService.getAccount(database, "300")));

		checkTotalsOfReport(report);
	}

	@Test
	public void testCloseBookkeepingWithUnsavedChangesFails() throws Exception {
		List<JournalItem> items = Arrays.asList(
				createItem(20, "100", true),
				createItem(20, "101", false));
		Journal journal = new Journal("ABC", "Test", DateUtil.createDate(2012, 1, 10), items, null);

		database.addJournal(journal, false);
		try {
			bookkeepingService.closeBookkeeping(database, "new bookkeeping",
					DateUtil.createDate(2012, 1, 1), bookkeepingService.getAccount(database, "200"));
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

		database.addJournal(journal, false);
		database.databaseConsistentWithFile();
		Database newDatabase = bookkeepingService.closeBookkeeping(database, "new bookkeeping",
				DateUtil.createDate(2012, 1, 1), bookkeepingService.getAccount(database, "200"));

		assertEquals("[20111231 start start balance, 20120110 ABC Test]",
				newDatabase.getJournals().toString());
	}

	@Test
	public void checkInUseForUsedAccount() throws ServiceException {
		assertTrue(bookkeepingService.inUse(database, bookkeepingService.getAccount(database, "190")));
		assertTrue(bookkeepingService.inUse(database, bookkeepingService.getAccount(database, "200")));
	}

	@Test
	public void checkInUseForUnusedAccount() throws ServiceException {
		assertFalse(bookkeepingService.inUse(database, bookkeepingService.getAccount(database, "400")));
	}

	@Test
	public void removeJournalThatCreatesInvoice() throws Exception {
		assertNotNull(findJournal("t1"));
		assertNotNull(database.getInvoice("inv1"));

		bookkeepingService.removeJournal(database, findJournal("t1"));

		assertNull(findJournal("t1"));
		assertNull(database.getInvoice("inv1"));
	}

	@Test
	public void removeJournalWithPayment() throws Exception {
		assertNotNull(findJournal("t2"));
		assertEquals("[20110510 pay1 Betaalrekening]",
				database.getPayments("inv1").toString());

		bookkeepingService.removeJournal(database, findJournal("t2"));

		assertNull(findJournal("t2"));
		assertEquals("[]", database.getPayments("inv1").toString());
	}

	@Test
	public void addNewAccount() throws Exception {
		bookkeepingService.createAccount(database,
                new Account("103", "Spaarrekening", AccountType.ASSET));
		Account a = bookkeepingService.getAccount(database, "103");
		assertEquals("103", a.getId());
		assertEquals("Spaarrekening", a.getName());
		assertEquals(AccountType.ASSET, a.getType());
	}

	@Test
	public void addAccountWithExistingIdFails() throws Exception {
		try {
			bookkeepingService.createAccount(database,
                    new Account("101", "Spaarrekening", AccountType.ASSET));
			fail("Expected exception was not thrown");
		} catch (ServiceException e) {
		}
	}

	@Test
	public void addUpdateAccount() throws Exception {
		bookkeepingService.updateAccount(database,
				new Account("101", "Spaarrekening", AccountType.ASSET));
		Account a = bookkeepingService.getAccount(database, "101");
		assertEquals("101", a.getId());
		assertEquals("Spaarrekening", a.getName());
		assertEquals(AccountType.ASSET, a.getType());
	}

	@Test
	public void updateNonExistingAccountFails() throws Exception {
		try {
			bookkeepingService.updateAccount(database,
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
