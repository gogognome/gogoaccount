package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * Tests the Database class.
 */
public class TestDatabase extends AbstractBookkeepingTest {

	private final ConfigurationService configurationService = new ConfigurationService();
	private final BookkeepingService bookkeepingService = new BookkeepingService();

	@Test
	public void testGetAllAccounts() throws Exception {
		List<Account> accounts = configurationService.findAllAccounts(document);

		assertEquals("[100 Kas, 101 Betaalrekening, 190 Debiteuren, " +
				"200 Eigen vermogen, 290 Crediteuren, " +
				"300 Contributie, 390 Onvoorzien, " +
				"400 Zaalhuur, 490 Onvoorzien]",
			accounts.toString());
	}

	@Test
	public void testCreatePaymentId() throws Exception {
		int n = 100000;
		Set<String> ids = new HashSet<String>(2 * n);
		for (int i=0; i<n; i++) {
			String id = document.createPaymentId();
			if (!ids.add(id)) {
				fail("The id " + id + " was generated more than once");
			}
		}
	}

	@Test
	public void testGetCreatingInvoice() throws Exception {
		assertEquals("t1", document.getCreatingJournal("inv1").getId());
		assertNull(document.getCreatingJournal("bla"));
	}

	@Test
	public void testAddingNonExistentParty() throws Exception {
		Party party = new Party("1115", "Hendrik Erikszoon", "Willemstraat 5", "6122 CC", "Heerenveen", null, null, null);
		assertNull(document.getParty(party.getId()));
		document.addParty(party);
		assertEqualParty(party, document.getParty(party.getId()));
	}

	@Test
	public void testAddingExistentPartyFails() throws Exception {
		Party party = new Party("1101", "Hendrik Erikszoon");
		assertNotNull(document.getParty(party.getId()));
		try {
			document.addParty(party);
			fail("Expected exception was not thrown");
		} catch (DocumentModificationFailedException e) {
		}
	}

	@Test
	public void updateExistingParty() throws Exception {
		Party party = new Party("1101", "Hendrik Erikszoon");
		assertNotNull(document.getParty(party.getId()));
		Party oldParty = document.getParty(party.getId());
		document.updateParty(oldParty, party);

		Party updatedParty = document.getParty(party.getId());
		assertEqualParty(party, updatedParty);
	}

	@Test
	public void updateNonExistingPartyFails() throws Exception {
		Party party = new Party("1115", "Hendrik Erikszoon");
		assertNull(document.getParty(party.getId()));
		try {
			document.updateParty(party, party);
			fail("Expected exception was not thrown");
		} catch (DocumentModificationFailedException e) {
		}
	}

	@Test
	public void removeExistingParty() throws Exception {
		Party party = document.getParties()[0];
		document.removeParty(party);
		assertNull(document.getParty(party.getId()));
	}

	@Test
	public void removeNonExistingPartyFails() throws Exception {
		Party party = new Party("1115", "Hendrik Erikszoon", "Willemstraat 5", "6122 CC",
				"Heerenveen", null, "Type 1", null);
		assertNull(document.getParty(party.getId()));
		try {
			document.removeParty(party);
			fail("Expected exception was not thrown");
		} catch (DocumentModificationFailedException e) {
		}
	}

	@Test
	public void testPartyTypes() throws Exception {
		Party party = new Party("1115", "Hendrik Erikszoon", "Willemstraat 5", "6122 CC",
				"Heerenveen", null, "Type 1", null);
		document.addParty(party);

		party = new Party("1116", "Erik Hendriksen", "Pieterstraat 23", "6122 CC",
				"Heerenveen", null, "Type 2", null);
		document.addParty(party);

		assertEquals("[Type 1, Type 2]", Arrays.toString(document.getPartyTypes()));
	}

	@Test
	public void testHasAccounts() throws Exception {
		assertTrue(configurationService.hasAccounts(document));

		assertFalse(configurationService.hasAccounts(bookkeepingService.createNewDatabase()));
	}

	@Test
	public void updateExistingJournal() throws Exception {
		Journal oldJournal = findJournal("t1");
		assertNotNull(oldJournal);

		List<JournalItem> items = Arrays.asList(
				new JournalItem(createAmount(20), configurationService.getAccount(document, "100"), true),
				new JournalItem(createAmount(20), configurationService.getAccount(document, "190"), false)
				);
		Journal newJournal = new Journal("t7", "test", DateUtil.createDate(2011, 9, 3),
				items, null);
		document.updateJournal(oldJournal, newJournal);

		assertEqualJournal(newJournal, findJournal(newJournal.getId()));
	}

	@Test
	public void updateNonExistingJournalFails() throws Exception {
		List<JournalItem> items = Arrays.asList(
				new JournalItem(createAmount(20), configurationService.getAccount(document, "100"), true),
				new JournalItem(createAmount(20), configurationService.getAccount(document, "190"), false)
				);
		Journal newJournal = new Journal("t7", "test", DateUtil.createDate(2011, 9, 3),
				items, null);

		assertNull(findJournal(newJournal.getId()));
		try {
			document.updateJournal(newJournal, newJournal);
			fail("Expected exception was not thrown");
		} catch (DocumentModificationFailedException e) {
		}
	}

	@Test
	public void updateExistingInvoice() throws Exception {
		String[] descriptions = new String[] { "Sponsoring 2011", "Sponsoring" };
		Amount[] amounts = new Amount[] { null, createAmount(30) };
		Invoice invoice = new Invoice("inv1",
				document.getParty("1102"), document.getParty("1102"),
				createAmount(30), DateUtil.createDate(2011, 5, 6), descriptions, amounts);

		document.updateInvoice("inv1", invoice);
		assertEqualInvoice(invoice, document.getInvoice("inv1"));
	}

	@Test
	public void updateNonExistingInvoiceFails() throws Exception {
		String[] descriptions = new String[] { "Sponsoring 2011", "Sponsoring" };
		Amount[] amounts = new Amount[] { null, createAmount(30) };
		Invoice invoice = new Invoice("inv421",
				document.getParty("1102"), document.getParty("1102"),
				createAmount(30), DateUtil.createDate(2011, 5, 6), descriptions, amounts);

		try {
			document.updateInvoice("inv421", invoice);
			fail("Expected exception was not thrown");
		} catch (DocumentModificationFailedException e) {
		}
	}

	@Test
	public void testPartySearchCriteria() throws Exception {
		PartySearchCriteria searchCriteria = new PartySearchCriteria();
		searchCriteria.setName("Puk");
		assertEquals("[1101 Pietje Puk]", Arrays.toString(document.getParties(searchCriteria)));

		searchCriteria = new PartySearchCriteria();
		searchCriteria.setAddress("Sterrenlaan");
		assertEquals("[1102 Jan Pieterszoon]", Arrays.toString(document.getParties(searchCriteria)));

		searchCriteria = new PartySearchCriteria();
		searchCriteria.setBirthDate(DateUtil.createDate(1980, 2, 23));
		assertEquals("[1101 Pietje Puk]", Arrays.toString(document.getParties(searchCriteria)));

		searchCriteria = new PartySearchCriteria();
		searchCriteria.setCity("Eind");
		assertEquals("[1102 Jan Pieterszoon]", Arrays.toString(document.getParties(searchCriteria)));

		searchCriteria = new PartySearchCriteria();
		searchCriteria.setZipCode("15");
		assertEquals("[1101 Pietje Puk]", Arrays.toString(document.getParties(searchCriteria)));
	}
}
