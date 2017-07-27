package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionSettings;
import nl.gogognome.gogoaccount.component.automaticcollection.PartyAutomaticCollectionSettings;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate;
import nl.gogognome.gogoaccount.component.invoice.InvoiceTemplateLine;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.test.builders.AmountBuilder;
import nl.gogognome.gogoaccount.test.builders.JournalEntryBuilder;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.util.Collections.singletonList;
import static junit.framework.Assert.*;
import static nl.gogognome.lib.util.DateUtil.createDate;


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
        Report report = bookkeepingService.createReport(document, createDate(2011, 12, 31));

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
                createDate(2011, 4, 30));

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
    public void closeBookkeeping_copiesAccountsAndDebtorsAndTotalAmounts() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));
            Bookkeeping newBookkeeping = configurationService.getBookkeeping(newDocument);

            assertEquals("new bookkeeping", newBookkeeping.getDescription());
            assertEquals(configurationService.findAllAccounts(document).toString(), configurationService.findAllAccounts(newDocument).toString());
            assertEquals(0, DateUtil.compareDayOfYear(createDate(2012, 1, 1),
                    newBookkeeping.getStartOfPeriod()));

            Report report = bookkeepingService.createReport(newDocument, createDate(2011, 12, 31));

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
    public void closeBookkeeping_bookkeepingIsOpen_closesTheOldBookkeepingAndOpensTheNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));
            Bookkeeping newBookkeeping = configurationService.getBookkeeping(newDocument);

            assertTrue(configurationService.getBookkeeping(document).isClosed());
            assertFalse(newBookkeeping.isClosed());
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }
   @Test
    public void closeBookkeeping_bookkeepingHasProfit_profitIsAddedToEquityInNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Report report = bookkeepingService.createReport(document, createDate(2011, 12, 31));
            Amount oldEquityAmount = report.getAmount(equity);
            Amount resultOfOperations = report.getResultOfOperations();
            assertTrue(resultOfOperations.isPositive());

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));
            configurationService.getBookkeeping(newDocument);

            Amount newEquityAmount = bookkeepingService.createReport(newDocument, createDate(2012, 1, 1)).getAmount(equity);
            assertEquals(oldEquityAmount.add(resultOfOperations), newEquityAmount);
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    @Test
    public void closeBookkeeping_bookkeepingHasLoss_lossIsSubtractedFromEquityInNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                    JournalEntryBuilder.debet(300, sportsHallRent),
                    JournalEntryBuilder.credit(300, creditors));
            JournalEntry journalEntry = JournalEntryBuilder.build(createDate(2011, 6, 1), "Test");

            ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, false);
            Report report = bookkeepingService.createReport(document, createDate(2011, 12, 31));
            Amount oldEquityAmount = report.getAmount(equity);
            Amount resultOfOperations = report.getResultOfOperations();
            assertTrue(resultOfOperations.isNegative());

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));
            configurationService.getBookkeeping(newDocument);

            Amount newEquityAmount = bookkeepingService.createReport(newDocument, createDate(2012, 1, 1)).getAmount(equity);
            assertEquals(oldEquityAmount.add(resultOfOperations), newEquityAmount);
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    @Test
    public void closeBookkeeping_oldBookkeepingHasJournalEntryAfterClosingDate_journalEntryIsCopiedToNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                    JournalEntryBuilder.buildDetail(20, "100", true),
                    JournalEntryBuilder.buildDetail(20, "101", false));
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setId("ABC");
            journalEntry.setDescription("Test");
            journalEntry.setDate(createDate(2012, 1, 10));

            ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, false);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            assertEquals("[20111231 start start balance, 20120110 ABC Test]", ledgerService.findJournalEntries(newDocument).toString());
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    @Test
    public void closeBookkeeping_invoiceCreatedBeforeClosingDateButHasNotBeenPaid_invoiceIsCopiedToNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            List<InvoiceTemplateLine> someLine = singletonList(new InvoiceTemplateLine(AmountBuilder.build(20), "Zaalhuur", sportsHallRent));
            InvoiceTemplate invoiceTemplate = new InvoiceTemplate(InvoiceTemplate.Type.SALE, "847539", createDate(2011, 8, 20), "Invoice for {name}", someLine);
            ledgerService.createInvoiceAndJournalForParties(document, debtor, invoiceTemplate, singletonList(pietPuk));

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            assertEquals("[20111231 start start balance]", ledgerService.findJournalEntries(newDocument).toString());
            assertEquals("[20110820 auto Invoice for Pietje Puk, 20110305 inv1 Contributie 2011]", invoiceService.findAllInvoices(newDocument).toString());
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    @Test
    public void closeBookkeeping_invoiceCreatedAfterClosingDate_invoiceIsCopiedToNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            List<InvoiceTemplateLine> someLine = singletonList(new InvoiceTemplateLine(AmountBuilder.build(20), "Zaalhuur", sportsHallRent));
            InvoiceTemplate invoiceTemplate = new InvoiceTemplate(InvoiceTemplate.Type.SALE,  null, createDate(2012, 1, 15), "Invoice for {name}", someLine);
            ledgerService.createInvoiceAndJournalForParties(document, debtor, invoiceTemplate, singletonList(pietPuk));

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            assertEquals("[20111231 start start balance, 20120115 auto Invoice for Pietje Puk]", ledgerService.findJournalEntries(newDocument).toString());
            assertEquals("[20120115 auto Invoice for Pietje Puk, 20110305 inv1 Contributie 2011]", invoiceService.findAllInvoices(newDocument).toString());
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    @Test
    public void closeBookkeeping_organizationDetailsFilledIn_organizationDetailsAreCopiedToNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            bookkeeping.setEnableAutomaticCollection(true);
            bookkeeping.setOrganizationAddress("Organization address");
            bookkeeping.setOrganizationCity("Organization city");
            bookkeeping.setOrganizationCountry("NL");
            bookkeeping.setOrganizationName("Organization name");
            bookkeeping.setOrganizationZipCode("Organization zip code");
            configurationService.updateBookkeeping(document, bookkeeping);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            Bookkeeping newBookkeeping = configurationService.getBookkeeping(newDocument);
            assertEquals(bookkeeping.getOrganizationAddress(), newBookkeeping.getOrganizationAddress());
            assertEquals(bookkeeping.getOrganizationCity(), newBookkeeping.getOrganizationCity());
            assertEquals(bookkeeping.getOrganizationCountry(), newBookkeeping.getOrganizationCountry());
            assertEquals(bookkeeping.getOrganizationName(), newBookkeeping.getOrganizationName());
            assertEquals(bookkeeping.getOrganizationZipCode(), newBookkeeping.getOrganizationZipCode());
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    @Test
    public void closeBookkeepingShouldCopyAutomaticCollectionDetails() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            AutomaticCollectionSettings settings = automaticCollectionService.getSettings(document);
            settings.setAutomaticCollectionContractNumber("contract number");
            settings.setBic("bic");
            settings.setIban("iban");
            settings.setSequenceNumber(123456L);
            automaticCollectionService.setSettings(document, settings);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            AutomaticCollectionSettings newSettings = automaticCollectionService.getSettings(newDocument);
            assertEquals(settings.getAutomaticCollectionContractNumber(), newSettings.getAutomaticCollectionContractNumber());
            assertEquals(settings.getBic(), newSettings.getBic());
            assertEquals(settings.getIban(), newSettings.getIban());
            assertEquals(settings.getSequenceNumber(), newSettings.getSequenceNumber());
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    @Test
    public void closeBookkeepingShouldCopyAutomaticCollectionDetailsForParties() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            PartyAutomaticCollectionSettings settings = automaticCollectionService.findSettings(document, pietPuk);
            settings.setAddress("address");
            settings.setCity("city");
            settings.setCountry("NL");
            settings.setIban("iban");
            settings.setMandateDate(createDate(2016, 10, 3));
            settings.setName("name");
            settings.setZipCode("zipCode");
            automaticCollectionService.setAutomaticCollectionSettings(document, settings);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            PartyAutomaticCollectionSettings newSettings = automaticCollectionService.findSettings(newDocument, pietPuk);
            assertEquals(settings.getAddress(), newSettings.getAddress());
            assertEquals(settings.getCity(), newSettings.getCity());
            assertEquals(settings.getCountry(), newSettings.getCountry());
            assertEquals(settings.getIban(), newSettings.getIban());
            assertEquals(settings.getMandateDate(), newSettings.getMandateDate());
            assertEquals(settings.getName(), newSettings.getName());
            assertEquals(settings.getZipCode(), newSettings.getZipCode());
        } finally {
            assertTrue("Failed to delete " + newBookkeepingFile.getAbsolutePath(), newBookkeepingFile.delete());
        }
    }

    private Document closeBookkeeping(File newBookkeepingFile, Date closingDate) throws ServiceException {
        return bookkeepingService.closeBookkeeping(document, newBookkeepingFile, "new bookkeeping", closingDate, equity);
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
