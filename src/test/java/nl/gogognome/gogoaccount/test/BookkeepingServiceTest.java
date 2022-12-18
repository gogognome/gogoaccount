package nl.gogognome.gogoaccount.test;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static nl.gogognome.gogoaccount.component.configuration.AccountType.*;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceSending.Type.*;
import static nl.gogognome.lib.util.DateUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.*;
import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.automaticcollection.*;
import nl.gogognome.gogoaccount.component.configuration.*;
import nl.gogognome.gogoaccount.component.document.*;
import nl.gogognome.gogoaccount.component.invoice.*;
import nl.gogognome.gogoaccount.component.ledger.*;
import nl.gogognome.gogoaccount.component.party.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.gogoaccount.test.builders.*;
import nl.gogognome.lib.text.*;
import nl.gogognome.lib.util.*;

public class BookkeepingServiceTest extends AbstractBookkeepingTest {

    @Test
    public void testStartBalance() throws Exception {
        assertAmountEquals(100, ledgerService.getStartBalance(document, cash));
        assertAmountEquals(300, ledgerService.getStartBalance(document, bankAccount));
        assertAmountEquals(400, ledgerService.getStartBalance(document, equity));
    }

    @Test
    public void deleteUnusedAccountSucceeds() throws Exception {
        configurationService.deleteAccount(document, creditors);
        assertAccountDoesNotExist(document, creditors.getId());
    }

    private void assertAccountDoesNotExist(Document document, String accountId) throws ServiceException {
        assertTrue(configurationService.findAllAccounts(document).stream().noneMatch(account -> account.getId().equals(accountId)));
    }

    @Test
    public void deleteUsedAccountFails() throws Exception {
        createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);

        assertThrows(ServiceException.class, () -> configurationService.deleteAccount(document, debtors));
        assertAccountExists(document, debtors.getId());
    }

    private void assertAccountExists(Document document, String accountId) throws ServiceException {
        assertTrue(configurationService.findAllAccounts(document).stream().anyMatch(account -> account.getId().equals(accountId)));
    }

    @Test
    public void testReportAtEndOf2011() throws Exception {
        Invoice invoice1 = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        Invoice invoice2 = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), pietPuk, "Subscription 2011 {name}", subscription, debtors, 456);
        createJournalEntry(createDate(2011, 3, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice1, debtors, null);

        Report report = bookkeepingService.createReport(document, createDate(2011, 12, 31));

        assertEquals(asList(cash, bankAccount, debtors, equity, creditors, sportsHallRent, unforeseenExpenses, subscription, unforeseenRevenues),
                report.getAllAccounts());

        assertEquals(asList(cash, bankAccount, debtors),
            report.getAssetsInclLossAccount());

        assertEquals(asList(equity, creditors, new Account("__profit__", "profit", LIABILITY)),
                report.getLiabilitiesInclProfitAccount());

        assertEquals(invoice1.getAmountToBePaid().add(invoice2.getAmountToBePaid()), report.getAmount(new Account("__profit__", "", LIABILITY)));

        assertEquals(asList(pietPuk), report.getDebtors());
        assertEquals(emptyList(), report.getCreditors());

        assertEquals(invoice2.getAmountToBePaid(), report.getBalanceForDebtor(pietPuk));
        assertAmountEquals(0, report.getBalanceForCreditor(janPieterszoon));

        assertEquals("[ " +
                        "null start balance 30000 null, " +
                        "20110325 p1 Payment subscription Jan Pieterszoon 12300 null 201100001,  " +
                        "null total mutations 12300 0,  " +
                        "null end balance 42300 null]",
                report.getLedgerLinesForAccount(configurationService.getAccount(document, bankAccount.getId())).toString());

        assertEquals("[ " +
                        "null start balance null 0, " +
                        "20110315 201100001 Subscription 2011 Jan Pieterszoon null 12300 201100001, " +
                        "20110315 201100002 Subscription 2011 Pietje Puk null 45600 201100002,  " +
                        "null total mutations 0 57900,  " +
                        "null end balance null 57900]",
                report.getLedgerLinesForAccount(configurationService.getAccount(document, "300")).toString());

        checkTotalsOfReport(report);
    }

    @Test
    public void testReportApril30_2011() throws Exception {
        Invoice invoice1 = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        Invoice invoice2 = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), pietPuk, "Subscription 2011 {name}", subscription, debtors, 456);
        createJournalEntry(createDate(2011, 5, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice1, debtors, null);

        Report report = bookkeepingService.createReport(document,
                createDate(2011, 4, 30));

        assertEquals(asList(cash, bankAccount, debtors, equity, creditors, sportsHallRent, unforeseenExpenses, subscription, unforeseenRevenues),
                report.getAllAccounts());

        assertEquals(asList(cash, bankAccount, debtors),
                report.getAssetsInclLossAccount());

        assertEquals(asList(equity, creditors, new Account("__profit__", "profit", LIABILITY)),
                report.getLiabilitiesInclProfitAccount());

        assertEquals(invoice1.getAmountToBePaid().add(invoice2.getAmountToBePaid()), report.getAmount(new Account("__profit__", "", LIABILITY)));

        assertEquals(asList(pietPuk, janPieterszoon), report.getDebtors());
        assertEquals(emptyList(), report.getCreditors());

        assertEquals(invoice2.getAmountToBePaid(), report.getBalanceForDebtor(pietPuk));
        assertAmountEquals(0, report.getBalanceForCreditor(janPieterszoon));

        assertEquals("[ " +
                        "null start balance 30000 null,  " +
                        "null total mutations 0 0,  " +
                        "null end balance 30000 null]",
                report.getLedgerLinesForAccount(configurationService.getAccount(document, bankAccount.getId())).toString());

        assertEquals("[ " +
                        "null start balance null 0, " +
                        "20110315 201100001 Subscription 2011 Jan Pieterszoon null 12300 201100001, " +
                        "20110315 201100002 Subscription 2011 Pietje Puk null 45600 201100002,  " +
                        "null total mutations 0 57900,  " +
                        "null end balance null 57900]",
                report.getLedgerLinesForAccount(configurationService.getAccount(document, "300")).toString());

        checkTotalsOfReport(report);
    }

    @Test
    public void closeBookkeeping_copiesAccountsAndDebtorsAndTotalAmounts() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Amount startEquity = ledgerService.getStartBalance(document, equity);
            Invoice invoice1 = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
            Invoice invoice2 = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), pietPuk, "Subscription 2011 {name}", subscription, debtors, 456);
            createJournalEntry(createDate(2011, 5, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice1, debtors, null);

            Date startOfNoewBookkeeping = createDate(2012, 1, 1);
            Document newDocument = closeBookkeeping(newBookkeepingFile, startOfNoewBookkeeping);
            Bookkeeping newBookkeeping = configurationService.getBookkeeping(newDocument);

            assertEquals("new bookkeeping", newBookkeeping.getDescription());
            assertEquals(configurationService.findAllAccounts(document).toString(), configurationService.findAllAccounts(newDocument).toString());
            assertEquals(0, DateUtil.compareDayOfYear(startOfNoewBookkeeping,
                    newBookkeeping.getStartOfPeriod()));

            Report report = bookkeepingService.createReport(newDocument, startOfNoewBookkeeping);

            assertEquals(asList(cash, bankAccount, debtors, equity, creditors, sportsHallRent, unforeseenExpenses, subscription, unforeseenRevenues),
                    report.getAllAccounts());

            assertEquals(asList(cash, bankAccount, debtors),
                    report.getAssetsInclLossAccount());

            assertEquals(asList(equity, creditors),
                    report.getLiabilitiesInclProfitAccount());

            assertEquals(asList(pietPuk), report.getDebtors());
            assertEquals(emptyList(), report.getCreditors());

            assertEquals(invoice2.getAmountToBePaid(), report.getBalanceForDebtor(pietPuk));
            assertAmountEquals(0, report.getBalanceForCreditor(janPieterszoon));

            assertEquals(startEquity.add(invoice1.getAmountToBePaid().add(invoice2.getAmountToBePaid())), report.getAmount(equity));
            assertAmountEquals(0, report.getAmount(subscription));

            checkTotalsOfReport(report);
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_invoiceCreatedOnDayBeforeStartOfNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Amount startEquity = ledgerService.getStartBalance(document, equity);
            Invoice invoice1 = createSalesInvoiceAndJournalEntry(createDate(2011, 12, 31), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
            Invoice invoice2 = createPurchaseInvoiceAndJournalEntry(createDate(2011, 12, 31), pietPuk, "Rent of sports hall", sportsHallRent, creditors, 456);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            Report report = bookkeepingService.createReport(newDocument, createDate(2012, 1, 1));

            assertEquals(asList(janPieterszoon), report.getDebtors());
            assertEquals(asList(pietPuk), report.getCreditors());

            assertEquals(invoice1.getAmountToBePaid(), report.getBalanceForDebtor(janPieterszoon));
            assertEquals(invoice1.getAmountToBePaid(), report.getAmount(debtors));
            assertEquals(invoice2.getAmountToBePaid(), report.getBalanceForCreditor(pietPuk).negate());
            assertEquals(invoice2.getAmountToBePaid(), report.getAmount(creditors).negate());

            assertEquals(Amount.ZERO, report.getResultOfOperations());
            assertEquals(startEquity.add(invoice1.getAmountToBePaid().add(invoice2.getAmountToBePaid())), report.getAmount(equity));

            assertAmountEquals(0, report.getAmount(subscription));

            checkTotalsOfReport(report);
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_invoiceCreatedDayBeforeStartingDateOfNewBookkeeping_dayBeforeStartingDayAddsToResultOfOldBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Amount startEquity = ledgerService.getStartBalance(document, equity);
            Date startOfNewBookkeeping = createDate(2012, 1, 1);
            Date lastDayOfOldBookkeeping = DateUtil.addDays(startOfNewBookkeeping, -1);
            Invoice invoice1 = createSalesInvoiceAndJournalEntry(lastDayOfOldBookkeeping, janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
            Invoice invoice2 = createPurchaseInvoiceAndJournalEntry(lastDayOfOldBookkeeping, pietPuk, "Rent of sports hall", sportsHallRent, creditors, 456);

            Document newDocument = closeBookkeeping(newBookkeepingFile, startOfNewBookkeeping);

            Report report = bookkeepingService.createReport(newDocument, startOfNewBookkeeping);

            assertEquals(asList(janPieterszoon), report.getDebtors());
            assertEquals(asList(pietPuk), report.getCreditors());

            assertEquals(invoice1.getAmountToBePaid(), report.getBalanceForDebtor(janPieterszoon));
            assertEquals(invoice1.getAmountToBePaid(), report.getAmount(debtors));
            assertEquals(invoice2.getAmountToBePaid(), report.getBalanceForCreditor(pietPuk).negate());
            assertEquals(invoice2.getAmountToBePaid(), report.getAmount(creditors).negate());

            assertEquals(Amount.ZERO, report.getResultOfOperations());
            assertEquals(startEquity.add(invoice1.getAmountToBePaid().add(invoice2.getAmountToBePaid())), report.getAmount(equity));

            assertAmountEquals(0, report.getAmount(subscription));
            assertAmountEquals(0, report.getAmount(sportsHallRent));

            checkTotalsOfReport(report);
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_invoiceCreatedOnStartingDateOfNewBookkeeping_startingDayAddsToResultOfNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Amount startEquity = ledgerService.getStartBalance(document, equity);
            Date startOfNewBookkeeping = createDate(2012, 1, 1);
            Invoice invoice1 = createSalesInvoiceAndJournalEntry(startOfNewBookkeeping, janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
            Invoice invoice2 = createPurchaseInvoiceAndJournalEntry(startOfNewBookkeeping, pietPuk, "Rent of sports hall", sportsHallRent, creditors, 456);

            Document newDocument = closeBookkeeping(newBookkeepingFile, startOfNewBookkeeping);

            Report report = bookkeepingService.createReport(newDocument, startOfNewBookkeeping);

            assertEquals(asList(janPieterszoon), report.getDebtors());
            assertEquals(asList(pietPuk), report.getCreditors());

            assertEquals(invoice1.getAmountToBePaid(), report.getBalanceForDebtor(janPieterszoon));
            assertEquals(invoice1.getAmountToBePaid(), report.getAmount(debtors));
            assertEquals(invoice2.getAmountToBePaid(), report.getBalanceForCreditor(pietPuk).negate());
            assertEquals(invoice2.getAmountToBePaid(), report.getAmount(creditors).negate());

            assertEquals(invoice1.getAmountToBePaid().add(invoice2.getAmountToBePaid()), report.getResultOfOperations());
            assertEquals(startEquity, report.getAmount(equity));

            assertEquals(invoice1.getAmountToBePaid(), report.getAmount(subscription));
            assertEquals(invoice2.getAmountToBePaid(), report.getAmount(sportsHallRent).negate());

            checkTotalsOfReport(report);
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_invoiceSendingsBeforeClosingDateAreCopiedToo() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Invoice invoice = createSalesInvoiceAndJournalEntry(createDate(2011, 10, 51), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
            DateUtil.setDateFactory(() -> DateUtil.createDate(2011, 10, 10));
            invoiceService.createInvoiceSending(document, invoice, EMAIL);
            DateUtil.setDateFactory(() -> DateUtil.createDate(2011, 11, 11));
            invoiceService.createInvoiceSending(document, invoice, PRINT);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            Report report = bookkeepingService.createReport(newDocument, createDate(2012, 1, 1));

            assertEquals(asList(janPieterszoon), report.getDebtors());
            List<InvoiceSending> invoiceSendings = invoiceService.findAllInvoiceSendings(newDocument);
            assertEquals(2, invoiceSendings.size());
            InvoiceSending invoiceSending1 = invoiceSendings.get(0);
            assertEquals(invoice.getId(), invoiceSending1.getInvoiceId());
            assertEqualDayOfYear(DateUtil.createDate(2011, 10, 10), invoiceSending1.getDate());
            assertEquals(EMAIL, invoiceSending1.getType());

            InvoiceSending invoiceSending2 = invoiceSendings.get(1);
            assertEquals(invoice.getId(), invoiceSending2.getInvoiceId());
            assertEqualDayOfYear(DateUtil.createDate(2011, 11, 11), invoiceSending2.getDate());
            assertEquals(PRINT, invoiceSending2.getType());

        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_copiesSettings() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            settingsService.save(document, "foo", "FOO");
            settingsService.save(document, "bar", "BAR");

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            assertEquals("FOO", settingsService.findValueForSetting(newDocument, "foo"));
            assertEquals("BAR", settingsService.findValueForSetting(newDocument, "bar"));
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
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
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_copiesPartiesWithoutChangingTheirIds() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Party luke = new Party();
            luke.setName("Luke");
            List<String> lukesTags = asList("tag1", "tag2");
            luke = partyService.createPartyWithNewId(document, luke, lukesTags);

            partyService.deleteParty(document, pietPuk);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            List<Party> newParties = partyService.findAllParties(newDocument);
            assertEqualParties(asList(janPieterszoon, luke), newParties);

            assertEquals(lukesTags, partyService.findTagsForParty(newDocument, luke));
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_bookkeepingHasProfit_profitIsAddedToEquityInNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);

            Report report = bookkeepingService.createReport(document, createDate(2011, 12, 31));
            Amount oldEquityAmount = report.getAmount(equity);
            Amount resultOfOperations = report.getResultOfOperations();
            assertTrue(resultOfOperations.isPositive());

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));
            configurationService.getBookkeeping(newDocument);

            Amount newEquityAmount = bookkeepingService.createReport(newDocument, createDate(2012, 1, 1)).getAmount(equity);
            assertEquals(oldEquityAmount.add(resultOfOperations), newEquityAmount);
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_bookkeepingHasLoss_lossIsSubtractedFromEquityInNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            createJournalEntry(createDate(2011, 6, 1), "p1", "rent sports hall", 123, sportsHallRent, null, creditors, null);

            Report report = bookkeepingService.createReport(document, createDate(2011, 12, 31));
            Amount oldEquityAmount = report.getAmount(equity);
            Amount resultOfOperations = report.getResultOfOperations();
            assertTrue(resultOfOperations.isNegative());

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));
            configurationService.getBookkeeping(newDocument);

            Amount newEquityAmount = bookkeepingService.createReport(newDocument, createDate(2012, 1, 1)).getAmount(equity);
            assertEquals(oldEquityAmount.add(resultOfOperations), newEquityAmount);
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_oldBookkeepingHasJournalEntryAfterStartDateOfNewBookkeeping_journalEntryIsCopiedToNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            List<JournalEntryDetail> journalEntryDetails = asList(
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
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_invoiceCreatedBeforeStartDateOfNewBookkeeping_ButHasNotBeenPaid_invoiceIsCopiedToNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Invoice invoice1 = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
            Invoice invoice2 = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), pietPuk, "Subscription 2011 {name}", subscription, debtors, 456);
            createJournalEntry(createDate(2011, 3, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice1, debtors, null);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            assertEquals("[20111231 start start balance]",
                    ledgerService.findJournalEntries(newDocument).toString());
            List<Invoice> openInvoices = invoiceService.findAllInvoices(newDocument);
            assertEquals(1, openInvoices.size());
            assertEqualInvoice(invoice2, openInvoices.get(0));
            assertEqualsInvoiceDetails(invoiceService.findDetails(document, invoice2), invoiceService.findDetails(newDocument, openInvoices.get(0)));
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_invoiceCreatedAfterStartDateOfNewBookkeeping_invoiceIsCopiedToNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Invoice invoice1 = createSalesInvoiceAndJournalEntry(createDate(2012, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
            Invoice invoice2 = createSalesInvoiceAndJournalEntry(createDate(2012, 3, 15), pietPuk, "Subscription 2011 {name}", subscription, debtors, 456);
            createJournalEntry(createDate(2012, 3, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice1, debtors, null);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            assertEquals("[20111231 start start balance, " +
                        "20120315 201100001 Subscription 2011 Jan Pieterszoon, " +
                        "20120315 201100002 Subscription 2011 Pietje Puk, " +
                        "20120325 p1 Payment subscription Jan Pieterszoon]",
                    ledgerService.findJournalEntries(newDocument).toString());
            List<Invoice> openInvoices = invoiceService.findAllInvoices(newDocument);
            assertEquals(2, openInvoices.size());
            assertEqualInvoice(invoice1, openInvoices.get(0));
            assertEqualsInvoiceDetails(invoiceService.findDetails(document, invoice1), invoiceService.findDetails(newDocument, openInvoices.get(0)));
            assertEqualInvoice(invoice2, openInvoices.get(1));
            assertEqualsInvoiceDetails(invoiceService.findDetails(document, invoice2), invoiceService.findDetails(newDocument, openInvoices.get(1)));
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_invoiceCreatedAndPayedBeforeStartDateOfNewBookkeeping_newInvoiceGetsNewId() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Invoice invoice1 = createSalesInvoiceAndJournalEntry(createDate(2012, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
            createJournalEntry(createDate(2012, 3, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice1, debtors, null);

            document = closeBookkeeping(newBookkeepingFile, createDate(2012, 4, 1));

            Invoice invoice2 = createSalesInvoiceAndJournalEntry(createDate(2012, 4, 5), pietPuk, "Subscription 2011 {name}", subscription, debtors, 123);

            assertNotEquals(invoice1.getId(), invoice2.getId());
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_partyCreatedAndRemovedBeforeStartDateOfNewBookkeeping_newPartyGetsNewId() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Party party = new Party();
            party.setName("Johan Johansson");
            party = partyService.createPartyWithNewId(document, party, emptyList());

            document = closeBookkeeping(newBookkeepingFile, createDate(2012, 4, 1));

            Party party2 = new Party();
            party2.setName("Klaas Klaassen");
            party2 = partyService.createPartyWithNewId(document, party2, emptyList());

            assertNotEquals(party2.getId(), party.getId());
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    @Test
    public void closeBookkeeping_bookkeepingSettingsFilledIn_bookkeepingSettingsAreCopiedToNewBookkeeping() throws Exception {
        File newBookkeepingFile = File.createTempFile("test", "h2.db");
        try {
            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            bookkeeping.setCurrency(Currency.getInstance("EUR"));
            bookkeeping.setInvoiceIdFormat("I-yyyy.nnn");
            bookkeeping.setPartyIdFormat("P-yyyy.nnn");
            bookkeeping.setEnableAutomaticCollection(true);
            bookkeeping.setOrganizationAddress("Organization address");
            bookkeeping.setOrganizationCity("Organization city");
            bookkeeping.setOrganizationCountry("NL");
            bookkeeping.setOrganizationName("Organization name");
            bookkeeping.setOrganizationZipCode("Organization zip code");
            configurationService.updateBookkeeping(document, bookkeeping);

            Document newDocument = closeBookkeeping(newBookkeepingFile, createDate(2012, 1, 1));

            Bookkeeping newBookkeeping = configurationService.getBookkeeping(newDocument);
            assertEquals(bookkeeping.getCurrency(), newBookkeeping.getCurrency());
            assertEquals(bookkeeping.getInvoiceIdFormat(), newBookkeeping.getInvoiceIdFormat());
            assertEquals(bookkeeping.getPartyIdFormat(), newBookkeeping.getPartyIdFormat());
            assertEquals(bookkeeping.getOrganizationAddress(), newBookkeeping.getOrganizationAddress());
            assertEquals(bookkeeping.getOrganizationCity(), newBookkeeping.getOrganizationCity());
            assertEquals(bookkeeping.getOrganizationCountry(), newBookkeeping.getOrganizationCountry());
            assertEquals(bookkeeping.getOrganizationName(), newBookkeeping.getOrganizationName());
            assertEquals(bookkeeping.getOrganizationZipCode(), newBookkeeping.getOrganizationZipCode());
        } finally {
            deleteNewBookkeeping(newBookkeepingFile);
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
            deleteNewBookkeeping(newBookkeepingFile);
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
            deleteNewBookkeeping(newBookkeepingFile);
        }
    }

    private Document closeBookkeeping(File newBookkeepingFile, Date startOfNewBookkeeping) throws ServiceException {
        return bookkeepingService.closeBookkeeping(document, newBookkeepingFile, "new bookkeeping", startOfNewBookkeeping, equity);
    }

    @Test
    public void checkInUseForUsedAccount() throws ServiceException {
        createSalesInvoiceAndJournalEntry(createDate(2012, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        assertTrue(ledgerService.isAccountUsed(document, subscription.getId()));
        assertTrue(ledgerService.isAccountUsed(document, debtors.getId()));
    }

    @Test
    public void checkInUseForUnusedAccount() throws ServiceException {
        assertFalse(ledgerService.isAccountUsed(document, sportsHallRent.getId()));
    }

    @Test
    public void addNewAccount() throws Exception {
        Account savingsAccount = new Account("103", "Savings account", AccountType.ASSET);
        configurationService.createAccount(document, savingsAccount);
        assertEqualAccount(savingsAccount, configurationService.getAccount(document, savingsAccount.getId()));
    }

    @Test
    public void addAccountWithExistingIdFails() throws Exception {
        Account accountWithExistingId = new Account(bankAccount.getId(), "Savings account", AccountType.ASSET);
        assertThrows(ServiceException.class, () -> configurationService.createAccount(document, accountWithExistingId));
    }

    @Test
    public void updateExistingAccount() throws Exception {
        Account bankAccountChangedToSavingsAccount = new Account(bankAccount.getId(), "Savings account", AccountType.ASSET);
        configurationService.updateAccount(document, bankAccountChangedToSavingsAccount);
        assertEqualAccount(bankAccountChangedToSavingsAccount, configurationService.getAccount(document, bankAccount.getId()));
    }

    @Test
    public void updateNonExistingAccountFails() throws Exception {
        Account accountWithNonExistingId = new Account("103", "Savings account", AccountType.ASSET);
        assertThrows(ServiceException.class, () -> configurationService.updateAccount(document, accountWithNonExistingId));
    }

    private void checkTotalsOfReport(Report report) {
        assertEquals(report.getTotalAssets(), report.getTotalLiabilities());

        Amount a = report.getTotalExpenses();
        a = a.subtract(report.getTotalRevenues());
        a = a.add(report.getResultOfOperations());
        assertEquals(Amount.ZERO, a);
    }

    private void deleteNewBookkeeping(File newBookkeepingFile) {
        assertTrue(newBookkeepingFile.delete(), "Failed to delete " + newBookkeepingFile.getAbsolutePath());
    }

}
