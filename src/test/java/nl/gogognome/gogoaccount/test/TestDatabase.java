package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.DocumentService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.*;

public class TestDatabase extends AbstractBookkeepingTest {

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
    public void testGetCreatingInvoice() throws Exception {
        assertEquals("t1", ledgerService.findJournalThatCreatesInvoice(document, "inv1").getId());
        assertNull(ledgerService.findJournalThatCreatesInvoice(document, "bla"));
    }

    @Test
    public void testHasAccounts() throws Exception {
        assertTrue(configurationService.hasAccounts(document));

        assertFalse(configurationService.hasAccounts(ObjectFactory.create(DocumentService.class).createNewDocumentInMemory("New bookkeeping")));
    }

    @Test
    public void updateExistingJournal() throws Exception {
        JournalEntry oldJournalEntry = findJournalEntry("t1");
        assertNotNull(oldJournalEntry);

        List<JournalEntryDetail> journalEntryDetails = new ArrayList<>();
        JournalEntryDetail d1 = new JournalEntryDetail();
        d1.setAmount(createAmount(20));
        d1.setAccountId("100");
        d1.setDebet(true);
        journalEntryDetails.add(d1);

        JournalEntryDetail d2 = new JournalEntryDetail();
        d2.setAmount(createAmount(20));
        d2.setAccountId("150");
        d2.setDebet(false);
        journalEntryDetails.add(d2);

        JournalEntry newJournalEntry = new JournalEntry(oldJournalEntry.getUniqueId());
        newJournalEntry.setId("t7");
        newJournalEntry.setDescription("test");
        newJournalEntry.setDate(DateUtil.createDate(2011, 9, 3));
        ledgerService.updateJournal(document, newJournalEntry, journalEntryDetails);

        assertEqualJournalEntry(newJournalEntry, findJournalEntry(newJournalEntry.getId()));
        assertEqualJournalEntryDetails(journalEntryDetails, findJournalEntryDetails(newJournalEntry.getId()));
    }

    @Test(expected = ServiceException.class)
    public void updateNonExistingJournalFails() throws Exception {
        List<JournalEntryDetail> newJournalEntryDetails = new ArrayList<>();
        JournalEntryDetail d1 = new JournalEntryDetail();
        d1.setAmount(createAmount(20));
        d1.setAccountId("100");
        d1.setDebet(true);
        newJournalEntryDetails.add(d1);

        JournalEntryDetail d2 = new JournalEntryDetail();
        d2.setAmount(createAmount(20));
        d2.setAccountId("190");
        d2.setDebet(false);
        newJournalEntryDetails.add(d2);

        JournalEntry newJournalEntry = new JournalEntry();
        newJournalEntry.setId("t7");
        newJournalEntry.setDescription("test");
        newJournalEntry.setDate(DateUtil.createDate(2011, 9, 3));

        assertNull(findJournalEntry(newJournalEntry.getId()));
        ledgerService.updateJournal(document, newJournalEntry, newJournalEntryDetails);
    }
}
