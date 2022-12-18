package nl.gogognome.gogoaccount.test;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate.Type.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.*;
import org.junit.jupiter.api.*;
import nl.gogognome.dataaccess.transaction.*;
import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.automaticcollection.*;
import nl.gogognome.gogoaccount.component.configuration.*;
import nl.gogognome.gogoaccount.component.document.*;
import nl.gogognome.gogoaccount.component.importer.*;
import nl.gogognome.gogoaccount.component.invoice.*;
import nl.gogognome.gogoaccount.component.ledger.*;
import nl.gogognome.gogoaccount.component.party.*;
import nl.gogognome.gogoaccount.component.settings.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.gogoaccount.test.builders.*;
import nl.gogognome.helpers.*;
import nl.gogognome.lib.swing.*;
import nl.gogognome.lib.text.*;
import nl.gogognome.lib.util.*;

/**
 * Abstract class that sets up a bookkeeping in an in-memory database. A new database instance is used per test.
 */
public abstract class AbstractBookkeepingTest {

    protected final AmountFormat amountFormat = new AmountFormat(Locale.US, Currency.getInstance("EUR"));
    protected final SettingsService settingsService = new SettingsService();
    protected final ConfigurationService configurationService = new ConfigurationService();
    protected final PartyService partyService = new PartyService(configurationService, settingsService);
    protected final DocumentService documentService = new DocumentService(configurationService);
    protected final ImportBankStatementService importBankStatementService = new ImportBankStatementService(configurationService);
    protected final TextResource textResource = new TestTextResource();
    protected final InvoiceService invoiceService = new InvoiceService(amountFormat, partyService, settingsService, textResource);
    protected final PaymentAmountAgainstDebtorAndCreditorValidator paymentAmountAgainstDebtorAndCreditorValidator =
            new PaymentAmountAgainstDebtorAndCreditorValidator(configurationService, invoiceService, textResource);
    protected final LedgerService ledgerService = new LedgerService(textResource, configurationService, invoiceService, partyService,
            paymentAmountAgainstDebtorAndCreditorValidator);
    protected final AutomaticCollectionService automaticCollectionService = new AutomaticCollectionService(configurationService, ledgerService, partyService);
    protected final BookkeepingService bookkeepingService = new BookkeepingService(automaticCollectionService, ledgerService, configurationService, documentService, invoiceService, partyService);

    protected Document document;
    protected Bookkeeping bookkeeping;

    protected Account cash = new Account("100", "Cash", AccountType.ASSET);
    protected Account bankAccount = new Account("101", "Bank account", AccountType.ASSET);
    protected Account debtors =  new Account("190", "Debtors", AccountType.DEBTOR);

    protected Account equity = new Account("200", "Equity", AccountType.EQUITY);
    protected Account creditors = new Account("290", "Creditors", AccountType.CREDITOR);

    protected Account sportsHallRent = new Account("400", "Sports hall rent", AccountType.EXPENSE);
    protected Account unforeseenExpenses = new Account("490", "Unforeseen expenses", AccountType.EXPENSE);

    protected Account subscription = new Account("300", "Subscription", AccountType.REVENUE);
    protected Account unforeseenRevenues = new Account("390", "Unforeseen revenues", AccountType.REVENUE);

    protected Party pietPuk;
    protected Party janPieterszoon;

    @BeforeEach
    public void initBookkeeping() throws Exception {
        DateUtil.setDateFactory(() -> DateUtil.createDate(2011, 2, 3, 14, 20, 30));
        CurrentTransaction.transactionCreator = DocumentAwareTransaction::new;
        initFactory();

        document = documentService.createNewDocumentInMemory("New bookkeeping");
        bookkeeping = configurationService.getBookkeeping(document);
        bookkeeping.setCurrency(Currency.getInstance("EUR"));
        bookkeeping.setOrganizationName("My Club");
        bookkeeping.setOrganizationAddress("Sesamstraat 134");
        bookkeeping.setOrganizationZipCode("1234 AB");
        bookkeeping.setOrganizationCity("Hilversum");
        bookkeeping.setOrganizationCountry("NL");
        bookkeeping.setStartOfPeriod(DateUtil.createDate(2011, 1, 1));
        configurationService.updateBookkeeping(document, bookkeeping);

        AutomaticCollectionSettings settings = new AutomaticCollectionSettings();
        settings.setIban("NL37RABO1234567890");
        settings.setSequenceNumber(100L);
        settings.setAutomaticCollectionContractNumber("CONTR12345");
        settings.setBic("RABONL2U");
        automaticCollectionService.setSettings(document, settings);

        createParties();

        createPartyAutomaticCollectionSettingsFor(pietPuk);

        for (Account account : createAccounts()) {
            configurationService.createAccount(document, account);
        }

        addStartBalance();

        document.notifyChange();
    }


    private void createPartyAutomaticCollectionSettingsFor(Party party) throws ServiceException {
        PartyAutomaticCollectionSettings partyAutomaticCollectionSettings = new PartyAutomaticCollectionSettings(party.getId());
        partyAutomaticCollectionSettings.setIban("NL52ABNA0123456789");
        partyAutomaticCollectionSettings.setName("P. Puk");
        partyAutomaticCollectionSettings.setAddress("Sesamstraat 137");
        partyAutomaticCollectionSettings.setZipCode("1234 AC");
        partyAutomaticCollectionSettings.setCity("Hilvserum");
        partyAutomaticCollectionSettings.setCountry("NL");
        partyAutomaticCollectionSettings.setMandateDate(DateUtil.createDate(2015, 3, 17));
        automaticCollectionService.setAutomaticCollectionSettings(document, partyAutomaticCollectionSettings);
    }

    private void initFactory() {
        TextResource tr = new TextResource(new Locale("en"));
        tr.loadResourceBundle("stringresources");
        Factory.bindSingleton(TextResource.class, tr);

        Factory.bindSingleton(AmountFormat.class, amountFormat);
    }

    protected Invoice createSalesInvoiceAndJournalEntry(Date date, Party party, String invoiceDescription, Account invoiceLineAccount, Account debtorOrCreditor, int amount)
            throws ServiceException {
        return createInvoiceAndJournalEntry(SALE, date, party, invoiceDescription, invoiceLineAccount, debtorOrCreditor, amount);
    }

    protected Invoice createPurchaseInvoiceAndJournalEntry(Date date, Party party, String invoiceDescription, Account invoiceLineAccount, Account debtorOrCreditor, int amount)
            throws ServiceException {
        return createInvoiceAndJournalEntry(PURCHASE, date, party, invoiceDescription, invoiceLineAccount, debtorOrCreditor, amount);
    }

    protected Invoice createInvoiceAndJournalEntry(InvoiceTemplate.Type type, Date date, Party party, String invoiceDescription, Account invoiceLineAccount, Account debtorOrCreditor, int amount)
            throws ServiceException {
        List<InvoiceTemplateLine> invoiceTemplateLines = asList(new InvoiceTemplateLine(AmountBuilder.build(amount), invoiceLineAccount.getName(), invoiceLineAccount));
        InvoiceTemplate invoiceTemplate = new InvoiceTemplate(type, null, date, invoiceDescription, invoiceTemplateLines);
        List<Invoice> createdInvoices = ledgerService.createInvoiceAndJournalForParties(document, debtorOrCreditor, invoiceTemplate, asList(party));
        return createdInvoices.get(0);
    }

    protected JournalEntry createJournalEntry(Date date, String journalEntryId, String journalEntryDescription, int amount, Account account1, Invoice invoice1, Account account2, Invoice invoice2)
            throws ServiceException {
        List<JournalEntryDetail> journalEntryDetails = new ArrayList<>();
        journalEntryDetails.add(JournalEntryBuilder.buildDetail(amount, account1.getId(), true, invoice1 != null ? invoice1.getId() : null));
        journalEntryDetails.add(JournalEntryBuilder.buildDetail(amount, account2.getId(), false, invoice2 != null ? invoice2.getId() : null));
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setId(journalEntryId);
        journalEntry.setDescription(journalEntryDescription);
        journalEntry.setDate(date);
        return ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, invoice1 != null || invoice2 != null);
    }

    private List<Account> createAccounts() {
        return asList(
                cash,
                bankAccount,
                debtors,

                equity,
                creditors,

                sportsHallRent,
                unforeseenExpenses,

                subscription,
                unforeseenRevenues
        );
    }

    private void createParties() throws ServiceException {
        pietPuk = new Party();
        pietPuk.setName("Pietje Puk");
        pietPuk.setAddress("Eikenlaan 64");
        pietPuk.setZipCode("1535 DS");
        pietPuk.setCity("Den Bosch");
        pietPuk.setBirthDate(DateUtil.createDate(1980, 2, 23));
        pietPuk.setRemarks("Is vaak afwezig");
        pietPuk = partyService.createPartyWithNewId(document, pietPuk, emptyList());

        janPieterszoon = new Party();
        janPieterszoon.setName("Jan Pieterszoon");
        janPieterszoon.setAddress("Sterrenlaan 532");
        janPieterszoon.setZipCode("5217 FG");
        janPieterszoon.setCity("Eindhoven");
        janPieterszoon = partyService.createPartyWithNewId(document, janPieterszoon, emptyList());
    }

    private void addStartBalance() throws Exception {
        List<JournalEntryDetail> journalEntryDetails = new ArrayList<>();
        journalEntryDetails.add(JournalEntryBuilder.buildDetail(100, "100", true));
        journalEntryDetails.add(JournalEntryBuilder.buildDetail(300, "101", true));
        journalEntryDetails.add(JournalEntryBuilder.buildDetail(400, "200", false));
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setId("start");
        journalEntry.setDescription("Start balance");
        journalEntry.setDate(DateUtil.addDays(bookkeeping.getStartOfPeriod(), -1));
        ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, false);
    }

    protected void assertAmountEquals(int expectedAmountInt, Amount actualAmount) throws ParseException {
        Amount expectedAmount = AmountBuilder.build(expectedAmountInt);
        assertEquals(amountFormat.formatAmount(expectedAmount.toBigInteger()),
                amountFormat.formatAmount(actualAmount.toBigInteger()));
    }

    protected JournalEntry findJournalEntry(String id) throws ServiceException {
        return ledgerService.findJournalEntry(document, id);
    }

    protected List<JournalEntryDetail> findJournalEntryDetails(String journalEntryId) throws ServiceException {
        return ledgerService.findJournalEntryDetails(document, findJournalEntry(journalEntryId));
    }

    /**
     * Checks whether two dates represent the same day.
     * @param expected expected date
     * @param actual acutal date
     */
    public void assertEqualDayOfYear(Date expected, Date actual) {
        if (expected == null && actual == null) {
            return;
        }

        if (expected == null) {
            assertNull(actual, "Expected null but was " + DateUtil.formatDateYYYYMMDD(actual));
        } else {
            assertNotNull(actual, "Expected " + DateUtil.formatDateYYYYMMDD(expected) + " but was null");
        }

        assertEquals(0, DateUtil.compareDayOfYear(expected, actual),
                "Expected " + DateUtil.formatDateYYYYMMDD(expected) + " but was " + DateUtil.formatDateYYYYMMDD(actual));
    }

    /**
     * Checks whether two databases are equal.
     * @param expected expected database
     * @param actual actual database
     * @throws ServiceException
     */
    public void assertEqualDatabase(Document expected, Document actual) throws ServiceException, SQLException {
        assertEquals(configurationService.findAllAccounts(expected).toString(), configurationService.findAllAccounts(actual).toString());

        List<Party> expectedParties = partyService.findAllParties(expected);
        List<Party> actualParties = partyService.findAllParties(actual);
        assertEquals(expectedParties.size(), actualParties.size());
        for (int i=0; i<expectedParties.size(); i++) {
            assertEqualParty(expectedParties.get(i), actualParties.get(i));
        }

        Bookkeeping expectedBookkeeping = configurationService.getBookkeeping(expected);
        Bookkeeping actualBookkeeping = configurationService.getBookkeeping(actual);

        assertEquals(expectedBookkeeping.getDescription(), actualBookkeeping.getDescription());
        assertEquals(expectedBookkeeping.getCurrency(), actualBookkeeping.getCurrency());
        assertEqualDayOfYear(expectedBookkeeping.getStartOfPeriod(), actualBookkeeping.getStartOfPeriod());

        Map<String, String> expectedImportedAccountsMap = importBankStatementService.getImportedTransactionAccountToAccountMap(expected);
        Map<String, String> actualImportedAccountsMap = importBankStatementService.getImportedTransactionAccountToAccountMap(expected);
        assertEquals(expectedImportedAccountsMap.keySet().toString(),
                actualImportedAccountsMap.keySet().toString());
        assertEquals(expectedImportedAccountsMap.values().toString(),
                actualImportedAccountsMap.values().toString());
        assertEquals(invoiceService.findAllInvoices(expected).size(), invoiceService.findAllInvoices(actual).size());
        for (Invoice invoice : invoiceService.findAllInvoices(expected)) {
            assertEqualInvoice(invoice, invoiceService.getInvoice(actual, invoice.getId()));
            assertEquals(invoiceService.findDescriptions(expected, invoice), invoiceService.findDescriptions(actual, invoice));
            assertEquals(invoiceService.findAmounts(expected, invoice), invoiceService.findAmounts(actual, invoice));

            List<Payment> expectedPayments = invoiceService.findPayments(expected, invoice);
            List<Payment> actualPayments = invoiceService.findPayments(actual, invoice);
            assertEquals(expectedPayments.size(), actualPayments.size());
            for (int i=0; i<expectedPayments.size(); i++) {
                assertEqualPayment(expectedPayments.get(i), actualPayments.get(i));
            }
        }

        assertEquals(ledgerService.findJournalEntries(expected).toString(),  ledgerService.findJournalEntries(actual).toString());

        Report expectedReport = bookkeepingService.createReport(expected, DateUtil.addYears(expectedBookkeeping.getStartOfPeriod(), 1));
        Report actualReport = bookkeepingService.createReport(actual, DateUtil.addYears(actualBookkeeping.getStartOfPeriod(), 1));

        assertEquals(expectedReport.getTotalAssets(), actualReport.getTotalAssets());
        assertEquals(expectedReport.getTotalLiabilities(), actualReport.getTotalLiabilities());
        assertEquals(expectedReport.getTotalExpenses(), actualReport.getTotalExpenses());
        assertEquals(expectedReport.getTotalRevenues(), actualReport.getTotalRevenues());
        assertEquals(expectedReport.getResultOfOperations(), actualReport.getResultOfOperations());
        assertEquals(expectedReport.getTotalDebtors(), actualReport.getTotalDebtors());
        assertEquals(expectedReport.getTotalCreditors(), actualReport.getTotalCreditors());
    }

    public void assertEqualAccount(Account expected, Account actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getType(), actual.getType());
    }

    public void assertEqualParties(List<Party> expectedParties, List<Party> actualParties) {
        assertEquals(expectedParties.size(), actualParties.size());
        for (int i=0; i<expectedParties.size(); i++) {
            assertEqualParty(expectedParties.get(i), actualParties.get(i));
        }
    }

    public void assertEqualParty(Party expected, Party actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getZipCode(), actual.getZipCode());
        assertEquals(expected.getCity(), actual.getCity());
        assertEquals(expected.getRemarks(), actual.getRemarks());
        assertEqualDayOfYear(expected.getBirthDate(), actual.getBirthDate());
    }

    public void assertEqualJournalEntry(JournalEntry expected, JournalEntry actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEqualDayOfYear(expected.getDate(), actual.getDate());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getIdOfCreatedInvoice(), actual.getIdOfCreatedInvoice());
    }

    public void assertEqualJournalEntryDetails(List<JournalEntryDetail> expected, List<JournalEntryDetail> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i=0; i<expected.size(); i++) {
            assertEqualItem(expected.get(i), actual.get(i));
        }
    }

    public void assertEqualItem(JournalEntryDetail expected, JournalEntryDetail actual) {
        assertEquals(expected.getAccountId(), actual.getAccountId());
        assertEquals(expected.getAmount(), actual.getAmount());
        assertEquals(expected.getInvoiceId(), actual.getInvoiceId());
        assertEquals(expected.getPaymentId(), actual.getPaymentId());
        assertEquals(expected.isDebet(), actual.isDebet());
    }

    public void assertEqualInvoice(Invoice expected, Invoice actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getAmountToBePaid(), actual.getAmountToBePaid());
        assertEquals(expected.getPartyId(), actual.getPartyId());
        assertEquals(expected.getPartyReference(), actual.getPartyReference());
        assertEqualDayOfYear(expected.getIssueDate(), actual.getIssueDate());
    }

    public void assertEqualsInvoiceDetails(List<InvoiceDetail> expected, List<InvoiceDetail> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i=0; i<expected.size(); i++) {
            assertEqualsInvoiceDetail(expected.get(i), actual.get(i));
        }
    }

    public void assertEqualsInvoiceDetail(InvoiceDetail expected, InvoiceDetail actual) {
        assertEquals(expected.getInvoiceId(), actual.getInvoiceId());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getAmount(), actual.getAmount());
    }

    private void assertEqualPayment(Payment expected, Payment actual) {
        assertEquals(expected.getAmount(), actual.getAmount());
        assertEquals(expected.getDate(), actual.getDate());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getInvoiceId(), actual.getInvoiceId());
    }

    protected <T extends Exception> T assertThrows(Class<T> expectedExceptionClass, RunnableWithException runnable) {
        try {
            runnable.run();
            fail("Expected exception was not thrown!");
            return null; // will never be executed
        } catch (Exception e) {
            assertEquals(e.getClass(), expectedExceptionClass, "Expected exception of type " + expectedExceptionClass + " but actual type was " + e.getClass());
            //noinspection unchecked
            return (T) e;
        }
    }
}
