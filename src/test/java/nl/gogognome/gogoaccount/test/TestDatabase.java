package nl.gogognome.gogoaccount.test;

import static java.util.Arrays.*;
import static nl.gogognome.lib.util.DateUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.*;
import org.junit.jupiter.api.*;
import nl.gogognome.gogoaccount.component.configuration.*;
import nl.gogognome.gogoaccount.component.invoice.*;
import nl.gogognome.gogoaccount.component.ledger.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.gogoaccount.test.builders.*;
import nl.gogognome.lib.util.*;

public class TestDatabase extends AbstractBookkeepingTest {

    @Test
    public void testGetAllAccounts() throws Exception {
        List<Account> accounts = configurationService.findAllAccounts(document);

        assertEquals(asList(cash, bankAccount, debtors, equity, creditors, subscription, unforeseenRevenues, sportsHallRent, unforeseenExpenses),
                accounts);
    }

    @Test
    public void findJournalThatCreatesInvoice_validInvoiceId_returnsIdOfJournalCreatingInvoice() throws Exception {
        Invoice invoice = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);

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

    @Test
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
        assertThrows(ServiceException.class, () -> ledgerService.updateJournalEntry(document, newJournalEntry, newJournalEntryDetails));
    }
}
