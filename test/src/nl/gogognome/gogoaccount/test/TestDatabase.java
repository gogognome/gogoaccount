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

    You should have received a copy of the GNU General Public Licensen
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package nl.gogognome.gogoaccount.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import cf.engine.Account;
import cf.engine.Account.Type;
/**
 * Tests the database class.
 *
 * @author Sander Kooijmans
 */
public class TestDatabase extends AbstractBookkeepingTest {

	@Test
	public void testGetAllAccounts() throws Exception {
		List<Account> accounts = database.getAllAccounts();

		assertEquals("[100 Kas, 101 Betaalrekening, 190 Debiteuren, " +
				"200 Eigen vermogen, 290 Crediteuren, " +
				"400 Zaalhuur, 490 Onvoorzien, " +
				"300 Contributie, 390 Onvoorzien]",
			accounts.toString());
	}

	@Test
	public void testGetAssets() throws Exception {
		for (Account a : database.getAssets()) {
			assertTrue(a.isDebet());
			assertEquals(Type.ASSET, a.getType());
		}
	}

	@Test
	public void testGetLiabilities() throws Exception {
		for (Account a : database.getLiabilities()) {
			assertTrue(a.isCredit());
			assertEquals(Type.LIABILITY, a.getType());
		}
	}

	@Test
	public void testGetExpenses() throws Exception {
		for (Account a : database.getExpenses()) {
			assertTrue(a.isDebet());
			assertEquals(Type.EXPENSE, a.getType());
		}
	}

	@Test
	public void testGetRevenues() throws Exception {
		for (Account a : database.getRevenues()) {
			assertTrue(a.isCredit());
			assertEquals(Type.REVENUE, a.getType());
		}
	}
}
