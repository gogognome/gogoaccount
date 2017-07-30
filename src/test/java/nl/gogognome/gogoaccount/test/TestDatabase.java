package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.test.builders.AmountBuilder;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static nl.gogognome.lib.util.DateUtil.createDate;
import static org.junit.Assert.*;

public class TestDatabase extends AbstractBookkeepingTest {

    @Test
    public void testGetAllAccounts() throws Exception {
        List<Account> accounts = configurationService.findAllAccounts(document);

        assertEquals(asList(cash, bankAccount, debtors, equity, creditors, subscription, unforeseenRevenues, sportsHallRent, unforeseenExpenses),
                accounts);
    }

    @Test
    public void findJournalThatCreatesInvoice_validInvoiceId_returnsIdOfJournalCreatingInvoice() throws Exception {
        Invoice invoice = createJournalEntryCreatingInvoice(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);

        assertEquals(invoice.getId(), ledgerService.findJournalThatCreatesInvoice(document, invoice.getId()).getId());
        JournalEntry journalEntry = ledgerService.findJournalEntry(document, invoice.getId());
        assertNotNull(journalEntry);
        assertEquals(invoice.getId(), journalEntry.getIdOfCreatedInvoice());
    }

    @Test
    public void findJournalThatCreatesInvoice_invalidInvoiceId_returnsNull() throws Exception {
        assertNull(ledgerService.findJournalThatCreatesInvoice(document, "bla"));
    }

    @Test
    public void testHasAccounts() throws Exception {
        assertTrue(configurationService.hasAccounts(document));

        assertFalse(configurationService.hasAccounts(documentService.createNewDocumentInMemory("New bookkeeping")));
    }

    @Test
    public void updateExistingJournal() throws Exception {
        JournalEntry oldJournalEntry = createJournalEntry(createDate(2011, 3, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, null, debtors, null);

        List<JournalEntryDetail> journalEntryDetails = new ArrayList<>();
        JournalEntryDetail d1 = new JournalEntryDetail();
        d1.setAmount(AmountBuilder.build(20));
        d1.setAccountId(cash.getId());
        d1.setDebet(true);
        journalEntryDetails.add(d1);

        JournalEntryDetail d2 = new JournalEntryDetail();
        d2.setAmount(AmountBuilder.build(20));
        d2.setAccountId(bankAccount.getId());
        d2.setDebet(false);
        journalEntryDetails.add(d2);

        JournalEntry newJournalEntry = new JournalEntry(oldJournalEntry.getUniqueId());
        newJournalEntry.setId("t7");
        newJournalEntry.setDescription("test");
        newJournalEntry.setDate(DateUtil.createDate(2011, 9, 3));
        ledgerService.updateJournalEntry(document, newJournalEntry, journalEntryDetails);

        assertEqualJournalEntry(newJournalEntry, findJournalEntry(newJournalEntry.getId()));
        assertEqualJournalEntryDetails(journalEntryDetails, findJournalEntryDetails(newJournalEntry.getId()));
    }

    @Test(expected = ServiceException.class)
    public void updateNonExistingJournalFails() throws Exception {
        List<JournalEntryDetail> newJournalEntryDetails = new ArrayList<>();
        JournalEntryDetail d1 = new JournalEntryDetail();
        d1.setAmount(AmountBuilder.build(20));
        d1.setAccountId("100");
        d1.setDebet(true);
        newJournalEntryDetails.add(d1);

        JournalEntryDetail d2 = new JournalEntryDetail();
        d2.setAmount(AmountBuilder.build(20));
        d2.setAccountId("190");
        d2.setDebet(false);
        newJournalEntryDetails.add(d2);

        JournalEntry newJournalEntry = new JournalEntry();
        newJournalEntry.setId("t7");
        newJournalEntry.setDescription("test");
        newJournalEntry.setDate(DateUtil.createDate(2011, 9, 3));

        assertNull(findJournalEntry(newJournalEntry.getId()));
        ledgerService.updateJournalEntry(document, newJournalEntry, newJournalEntryDetails);
    }
}
