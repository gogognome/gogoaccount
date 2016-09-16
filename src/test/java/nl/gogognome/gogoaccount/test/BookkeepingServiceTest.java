package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.*;


public class BookkeepingServiceTest extends AbstractBookkeepingTest {

    @Test
    public void testStartBalance() throws Exception {
        checkAmount(100, ledgerService.getStartBalance(document, configurationService.getAccount(document, "100")));
        checkAmount(300, ledgerService.getStartBalance(document, configurationService.getAccount(document, "101")));
        checkAmount(400, ledgerService.getStartBalance(document, configurationService.getAccount(document, "200")));
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

        assertEquals("[200 Eigen vermogen, 290 Crediteuren,  Profit]",
                report.getLiabilitiesInclProfitAccount().toString());

        checkAmount(20, report.getAmount(new Account("", "", AccountType.LIABILITY)));

        assertEquals("[1101 Pietje Puk]", report.getDebtors().toString());
        assertEquals("[]", report.getCreditors().toString());

        Party party = partyService.getParty(document, "1101");
        checkAmount(10, report.getBalanceForDebtor(party));
        checkAmount(0, report.getBalanceForCreditor(party));

        assertEquals("[ null start balance 30000 null, " +
                        "20110510 t2 Payment 1000 null inv1,  " +
                        "null total mutations 1000 0,  " +
                        "null end balance 31000 null]",
                report.getLedgerLinesForAccount(configurationService.getAccount(document, "101")).toString());

        assertEquals("[ null start balance null 0, " +
                "20110305 t1 Payment null 2000 inv1,  " +
                "null total mutations 0 2000,  " +
                "null end balance null 2000]",
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

        assertEquals("[200 Eigen vermogen, 290 Crediteuren,  Profit]",
                report.getLiabilitiesInclProfitAccount().toString());

        checkAmount(20, report.getAmount(new Account("", "", AccountType.LIABILITY)));

        assertEquals("[1101 Pietje Puk]", report.getDebtors().toString());
        assertEquals("[]", report.getCreditors().toString());

        Party party = partyService.getParty(document, "1101");
        checkAmount(20, report.getBalanceForDebtor(party));
        checkAmount(0, report.getBalanceForCreditor(party));

        assertEquals("[ null start balance 30000 null,  " +
                        "null total mutations 0 0,  " +
                        "null end balance 30000 null]",
                report.getLedgerLinesForAccount(configurationService.getAccount(document, "101")).toString());

        checkTotalsOfReport(report);
    }

    @Test
    public void testCloseBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Document newDocument = bookkeepingService.closeBookkeeping(document, newBookkeepingFile, "new bookkeeping",
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
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    @Test
    public void testCloseBookkeepingWithJournalsCopiedToNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                    buildJournalEntryDetail(20, "100", true),
                    buildJournalEntryDetail(20, "101", false));
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setId("ABC");
            journalEntry.setDescription("Test");
            journalEntry.setDate(DateUtil.createDate(2012, 1, 10));

            ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, false);
            document.notifyChange();
            Document newDocument = bookkeepingService.closeBookkeeping(document, newBookkeepingFile, "new bookkeeping",
                    DateUtil.createDate(2012, 1, 1), configurationService.getAccount(document, "200"));

            assertEquals("[20111231 start start balance, 20120110 ABC Test]", ledgerService.findJournalEntries(newDocument).toString());
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    @Test
    public void checkInUseForUsedAccount() throws ServiceException {
        assertTrue(ledgerService.isAccountUsed(document, "190"));
        assertTrue(ledgerService.isAccountUsed(document, "200"));
    }

    @Test
    public void checkInUseForUnusedAccount() throws ServiceException {
        assertFalse(ledgerService.isAccountUsed(document, "400"));
    }

    @Test
    public void removeJournalThatCreatesInvoice() throws Exception {
        assertNotNull(findJournalEntry("t1"));
        assertTrue(invoiceService.existsInvoice(document, "inv1"));

        ledgerService.removeJournal(document, findJournalEntry("t2")); // must remove journal with payment too to prevent foreign key violation
        ledgerService.removeJournal(document, findJournalEntry("t1"));

        assertNull(findJournalEntry("t1"));
        assertFalse(invoiceService.existsInvoice(document, "inv1"));
    }

    @Test
    public void removeJournalWithPayment() throws Exception {
        assertNotNull(findJournalEntry("t2"));
        Invoice invoice = invoiceService.getInvoice(document, "inv1");
        assertFalse(invoiceService.findPayments(document, invoice).isEmpty());

        ledgerService.removeJournal(document, findJournalEntry("t2"));

        assertNull(findJournalEntry("t2"));
        assertTrue(invoiceService.findPayments(document, invoice).isEmpty());
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

    @Test(expected = ServiceException.class)
    public void addAccountWithExistingIdFails() throws Exception {
        configurationService.createAccount(document,
                new Account("101", "Spaarrekening", AccountType.ASSET));
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

    @Test(expected = ServiceException.class)
    public void updateNonExistingAccountFails() throws Exception {
        configurationService.updateAccount(document,
                new Account("103", "Spaarrekening", AccountType.ASSET));
        fail("Expected exception was not thrown");
    }

    private void checkTotalsOfReport(Report report) {
        assertEquals(report.getTotalAssets(), report.getTotalLiabilities());

        Amount a = report.getTotalExpenses();
        a = a.subtract(report.getTotalRevenues());
        a = a.add(report.getResultOfOperations());
        assertEquals(zero, a);
    }
}
