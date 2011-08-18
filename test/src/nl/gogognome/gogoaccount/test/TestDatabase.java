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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import org.junit.Test;

import cf.engine.Account;
import cf.engine.Account.Type;
import cf.engine.Database;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;
import cf.engine.PartySearchCriteria;
/**
 * Tests the Database class.
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

	@Test
	public void testCreatePaymentId() throws Exception {
		int n = 1000000;
		Set<String> ids = new HashSet<String>(2 * n);
		for (int i=0; i<n; i++) {
			String id = database.createPaymentId();
			if (!ids.add(id)) {
				fail("The id " + id + " was generated more than once");
			}
		}
	}

	@Test
	public void testGetCreatingInvoice() throws Exception {
		assertEquals("t1", database.getCreatingJournal("inv1").getId());
		assertNull(database.getCreatingJournal("bla"));
	}

	@Test
	public void testAddingNonExistentParty() throws Exception {
		Party party = new Party("1115", "Hendrik Erikszoon", "Willemstraat 5", "6122 CC", "Heerenveen", null, null, null);
		assertNull(database.getParty(party.getId()));
		database.addParty(party);
		assertEqualParty(party, database.getParty(party.getId()));
	}

	@Test
	public void testAddingExistentPartyFails() throws Exception {
		Party party = new Party("1101", "Hendrik Erikszoon");
		assertNotNull(database.getParty(party.getId()));
		try {
			database.addParty(party);
			fail("Expected exception was not thrown");
		} catch (DatabaseModificationFailedException e) {
		}
	}

	@Test
	public void updateExistingParty() throws Exception {
		Party party = new Party("1101", "Hendrik Erikszoon");
		assertNotNull(database.getParty(party.getId()));
		Party oldParty = database.getParty(party.getId());
		database.updateParty(oldParty, party);

		Party updatedParty = database.getParty(party.getId());
		assertEqualParty(party, updatedParty);
	}

	@Test
	public void updateNonExistingPartyFails() throws Exception {
		Party party = new Party("1115", "Hendrik Erikszoon");
		assertNull(database.getParty(party.getId()));
		try {
			database.updateParty(party, party);
			fail("Expected exception was not thrown");
		} catch (DatabaseModificationFailedException e) {
		}
	}

	@Test
	public void removeExistingParty() throws Exception {
		Party party = database.getParties()[0];
		database.removeParty(party);
		assertNull(database.getParty(party.getId()));
	}

	@Test
	public void removeNonExistingPartyFails() throws Exception {
		Party party = new Party("1115", "Hendrik Erikszoon", "Willemstraat 5", "6122 CC",
				"Heerenveen", null, "Type 1", null);
		assertNull(database.getParty(party.getId()));
		try {
			database.removeParty(party);
			fail("Expected exception was not thrown");
		} catch (DatabaseModificationFailedException e) {
		}
	}

	@Test
	public void testPartyTypes() throws Exception {
		Party party = new Party("1115", "Hendrik Erikszoon", "Willemstraat 5", "6122 CC",
				"Heerenveen", null, "Type 1", null);
		database.addParty(party);

		party = new Party("1116", "Erik Hendriksen", "Pieterstraat 23", "6122 CC",
				"Heerenveen", null, "Type 2", null);
		database.addParty(party);

		assertEquals("[Type 1, Type 2]", Arrays.toString(database.getPartyTypes()));
	}

	@Test
	public void testHasAccounts() throws Exception {
		assertTrue(database.hasAccounts());

		assertFalse(new Database().hasAccounts());
	}

	@Test
	public void updateExistingJournal() throws Exception {
		Journal oldJournal = findJournal("t1");
		assertNotNull(oldJournal);

		List<JournalItem> items = Arrays.asList(
				new JournalItem(createAmount(20), database.getAccount("100"), true),
				new JournalItem(createAmount(20), database.getAccount("190"), false)
				);
		Journal newJournal = new Journal("t7", "test", DateUtil.createDate(2011, 9, 3),
				items, null);
		database.updateJournal(oldJournal, newJournal);

		assertEqualJournal(newJournal, findJournal(newJournal.getId()));
	}

	@Test
	public void updateNonExistingJournalFails() throws Exception {
		List<JournalItem> items = Arrays.asList(
				new JournalItem(createAmount(20), database.getAccount("100"), true),
				new JournalItem(createAmount(20), database.getAccount("190"), false)
				);
		Journal newJournal = new Journal("t7", "test", DateUtil.createDate(2011, 9, 3),
				items, null);

		assertNull(findJournal(newJournal.getId()));
		try {
			database.updateJournal(newJournal, newJournal);
			fail("Expected exception was not thrown");
		} catch (DatabaseModificationFailedException e) {
		}
	}

	@Test
	public void updateExistingInvoice() throws Exception {
		String[] descriptions = new String[] { "Sponsoring 2011", "Sponsoring" };
		Amount[] amounts = new Amount[] { null, createAmount(30) };
		Invoice invoice = new Invoice("inv1",
				database.getParty("1102"), database.getParty("1102"),
				createAmount(30), DateUtil.createDate(2011, 5, 6), descriptions, amounts);

		database.updateInvoice("inv1", invoice);
		assertEqualInvoice(invoice, database.getInvoice("inv1"));
	}

	@Test
	public void updateNonExistingInvoiceFails() throws Exception {
		String[] descriptions = new String[] { "Sponsoring 2011", "Sponsoring" };
		Amount[] amounts = new Amount[] { null, createAmount(30) };
		Invoice invoice = new Invoice("inv421",
				database.getParty("1102"), database.getParty("1102"),
				createAmount(30), DateUtil.createDate(2011, 5, 6), descriptions, amounts);

		try {
			database.updateInvoice("inv421", invoice);
			fail("Expected exception was not thrown");
		} catch (DatabaseModificationFailedException e) {
		}
	}

	@Test
	public void testPartySearchCriteria() throws Exception {
		PartySearchCriteria searchCriteria = new PartySearchCriteria();
		searchCriteria.setName("Puk");
		assertEquals("[1101 Pietje Puk]", Arrays.toString(database.getParties(searchCriteria)));

		searchCriteria = new PartySearchCriteria();
		searchCriteria.setAddress("Sterrenlaan");
		assertEquals("[1102 Jan Pieterszoon]", Arrays.toString(database.getParties(searchCriteria)));

		searchCriteria = new PartySearchCriteria();
		searchCriteria.setBirthDate(DateUtil.createDate(1980, 2, 23));
		assertEquals("[1101 Pietje Puk]", Arrays.toString(database.getParties(searchCriteria)));

		searchCriteria = new PartySearchCriteria();
		searchCriteria.setCity("Eind");
		assertEquals("[1102 Jan Pieterszoon]", Arrays.toString(database.getParties(searchCriteria)));

		searchCriteria = new PartySearchCriteria();
		searchCriteria.setZipCode("15");
		assertEquals("[1101 Pietje Puk]", Arrays.toString(database.getParties(searchCriteria)));
	}
}
