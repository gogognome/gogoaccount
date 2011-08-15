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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.cf.services.DeleteException;
import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import org.junit.Test;

import cf.engine.Account;
import cf.engine.Account.Type;

/**
 * Tests the bookkeeping service.
 *
 * @author Sander Kooijmans
 */
public class TestBookkeepingService extends AbstractBookkeepingTest {

	@Test
	public void testStartBalance() throws Exception {
		checkAmount(100,
				BookkeepingService.getStartBalance(database, database.getAccount("100")));
		checkAmount(300,
				BookkeepingService.getStartBalance(database, database.getAccount("101")));
		checkAmount(400,
				BookkeepingService.getStartBalance(database, database.getAccount("200")));
	}

	@Test
	public void deleteUnusedAccountSucceeds() throws Exception {
		BookkeepingService.deleteAccount(database, database.getAccount("290"));
		assertNull(database.getAccount("290"));
	}

	@Test
	public void deleteAccountFails() throws Exception {
		try {
			BookkeepingService.deleteAccount(database, database.getAccount("190"));
			fail("Expected exception was not thrown");
		} catch (DeleteException e) {
			assertNotNull(database.getAccount("190"));
		}
	}

	@Test
	public void testReportAtEndOf2011() throws Exception {
		Report report = BookkeepingService.createReport(database,
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

		checkAmount(20, report.getAmount(new Account("", "", Type.LIABILITY)));

		assertEquals("[1101 Pietje Puk]", report.getDebtors().toString());
		assertEquals("[]", report.getCreditors().toString());

		checkAmount(10, report.getBalanceForDebtor(database.getParty("1101")));
		checkAmount(0, report.getBalanceForCreditor(database.getParty("1101")));

		assertEquals("[ null beginsaldo 30000 null, " +
				"20110510 t1 Payment 1000 null inv1,  " +
				"null totaal mutaties 1000 0,  " +
				"null eindsaldo 31000 null]",
				report.getLedgerLinesForAccount(database.getAccount("101")).toString());

		assertEquals("[ null beginsaldo null 0, " +
				"20110305 t1 Payment null 2000 inv1,  " +
				"null totaal mutaties 0 2000,  " +
				"null eindsaldo null 2000]",
				report.getLedgerLinesForAccount(database.getAccount("300")).toString());

		checkTotalsOfReport(report);
	}

	@Test
	public void testReportApril30_2011() throws Exception {
		Report report = BookkeepingService.createReport(database,
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

		checkAmount(20, report.getAmount(new Account("", "", Type.LIABILITY)));

		assertEquals("[1101 Pietje Puk]", report.getDebtors().toString());
		assertEquals("[]", report.getCreditors().toString());

		checkAmount(20, report.getBalanceForDebtor(database.getParty("1101")));
		checkAmount(0, report.getBalanceForCreditor(database.getParty("1101")));

		assertEquals("[ null beginsaldo 30000 null,  " +
				"null totaal mutaties 0 0,  " +
				"null eindsaldo 30000 null]",
				report.getLedgerLinesForAccount(database.getAccount("101")).toString());

		checkTotalsOfReport(report);
	}

	private void checkTotalsOfReport(Report report) {
		assertEquals(report.getTotalAssets(), report.getTotalLiabilities());

		Amount a = report.getTotalExpenses();
		a = a.subtract(report.getTotalRevenues());
		a = a.add(report.getResultOfOperations());
		assertEquals(zero, a);
	}
}
