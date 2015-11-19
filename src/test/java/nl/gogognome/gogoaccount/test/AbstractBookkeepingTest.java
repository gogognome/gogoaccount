package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.DocumentService;
import nl.gogognome.gogoaccount.component.importer.ImportBankStatementService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;
import org.junit.Before;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

import static junit.framework.Assert.*;

/**
 * Abstract class that sets up a bookkeeping in an in-memory database. A new database instance is used per test.
 */
public abstract class AbstractBookkeepingTest {

    private final BookkeepingService bookkeepingService = new BookkeepingService();
    private final ConfigurationService configurationService = new ConfigurationService();
    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final ImportBankStatementService importBankStatementService = ObjectFactory.create(ImportBankStatementService.class);
    private final LedgerService ledgerService = ObjectFactory.create(LedgerService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

    protected Document document;
    protected Bookkeeping bookkeeping;
    protected AmountFormat amountFormat;
    protected Amount zero;

    @Before
    public void initBookkeeping() throws Exception {
        initFactory();

        document = ObjectFactory.create(DocumentService.class).createNewDocument("New bookkeeping");
        bookkeeping = configurationService.getBookkeeping(document);
        bookkeeping.setCurrency(Currency.getInstance("EUR"));
        bookkeeping.setOrganizationName("My Club");
        bookkeeping.setOrganizationAddress("Sesamstraat 134");
        bookkeeping.setOrganizationZipCode("1234 AB");
        bookkeeping.setOrganizationCity("Hilversum");
        bookkeeping.setOrganizationCountry("NL");
        bookkeeping.setStartOfPeriod(DateUtil.createDate(2011, 1, 1));
        configurationService.updateBookkeeping(document, bookkeeping);

        PartyService partyService = new PartyService();
        for (Party party : createParties()) {
            partyService.createParty(document, party);
        }

        for (Account account : createAccounts()) {
            configurationService.createAccount(document, account);
        }

        addStartBalance();
        addJournals();

        document.notifyChange();

        zero = Amount.getZero(bookkeeping.getCurrency());
    }

    private void initFactory() {
        TextResource tr = new TextResource(new Locale("nl"));
        tr.loadResourceBundle("stringresources");
        Factory.bindSingleton(TextResource.class, tr);

        amountFormat = new AmountFormat(tr.getLocale());
        Factory.bindSingleton(AmountFormat.class, amountFormat);
    }

    private void addJournals() throws Exception {
        List<JournalEntryDetail> journalEntryDetails = new ArrayList<>();
        journalEntryDetails.add(buildJournalEntryDetail(20, "190", true));
        journalEntryDetails.add(buildJournalEntryDetail(20, "300", false));
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setId("t1");
        journalEntry.setDescription("Payment");
        journalEntry.setDate(DateUtil.createDate(2011, 3, 5));
        journalEntry.setIdOfCreatedInvoice("inv1");

        List<String> descriptions = Arrays.asList("Contributie 2011", "Contributie");
        List<Amount> amounts = Arrays.asList(null, createAmount(20));
        Party party = new PartyService().getParty(document, "1101");
        Invoice invoice = new Invoice(journalEntry.getIdOfCreatedInvoice());
        invoice.setConcerningPartyId(party.getId());
        invoice.setPayingPartyId(party.getId());
        invoice.setAmountToBePaid(createAmount(20));
        invoice.setIssueDate(journalEntry.getDate());
        invoiceService.createInvoice(document, invoice);
        invoiceService.createDetails(document, invoice, descriptions, amounts);
        ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, false);

        journalEntryDetails = new ArrayList<>();
        journalEntryDetails.add(buildJournalEntryDetail(10, "101", true, "inv1", "pay1"));
        journalEntryDetails.add(buildJournalEntryDetail(10, "190", false));
        journalEntry = new JournalEntry();
        journalEntry.setId("t2");
        journalEntry.setDescription("Payment");
        journalEntry.setDate(DateUtil.createDate(2011, 5, 10));
        ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, true);
    }

    private List<Account> createAccounts() {
        return Arrays.asList(
                new Account("100", "Kas", AccountType.ASSET),
                new Account("101", "Betaalrekening", AccountType.ASSET),
                new Account("190", "Debiteuren", AccountType.ASSET),

                new Account("200", "Eigen vermogen", AccountType.LIABILITY),
                new Account("290", "Crediteuren", AccountType.LIABILITY),

                new Account("400", "Zaalhuur", AccountType.EXPENSE),
                new Account("490", "Onvoorzien", AccountType.EXPENSE),

                new Account("300", "Contributie", AccountType.REVENUE),
                new Account("390", "Onvoorzien", AccountType.REVENUE)
        );
    }

    private List<Party> createParties() {
        List<Party> parties = new ArrayList<>();
        Party party = new Party("1101");
        party.setName("Pietje Puk");
        party.setAddress("Eikenlaan 64");
        party.setZipCode("1535 DS");
        party.setCity("Den Bosch");
        party.setBirthDate(DateUtil.createDate(1980, 2, 23));
        party.setRemarks("Is vaak afwezig");
        parties.add(party);

        party = new Party("1102");
        party.setName("Jan Pieterszoon");
        party.setAddress("Sterrenlaan 532");
        party.setZipCode("5217 FG");
        party.setCity("Eindhoven");
        parties.add(party);

        return parties;
    }

    private void addStartBalance() throws Exception {
        List<JournalEntryDetail> journalEntryDetails = new ArrayList<>();
        journalEntryDetails.add(buildJournalEntryDetail(100, "100", true));
        journalEntryDetails.add(buildJournalEntryDetail(300, "101", true));
        journalEntryDetails.add(buildJournalEntryDetail(400, "200", false));
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setId("start");
        journalEntry.setDescription("Start balance");
        journalEntry.setDate(DateUtil.addDays(bookkeeping.getStartOfPeriod(), -1));
        ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, false);
    }

    protected JournalEntryDetail buildJournalEntryDetail(int amountInt, String accountId, boolean debet) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Amount amount = createAmount(amountInt);
            JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
            journalEntryDetail.setAmount(amount);
            journalEntryDetail.setAccountId(accountId);
            journalEntryDetail.setDebet(debet);
            return journalEntryDetail;
        });
    }

    protected JournalEntryDetail buildJournalEntryDetail(int amountInt, String accountId, boolean debet, String invoiceId, String paymentId) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Amount amount = createAmount(amountInt);
            JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
            journalEntryDetail.setAmount(amount);
            journalEntryDetail.setAccountId(accountId);
            journalEntryDetail.setDebet(debet);
            journalEntryDetail.setInvoiceId(invoiceId);
            journalEntryDetail.setPaymentId(paymentId);
            return journalEntryDetail;
        });
    }

    protected Amount createAmount(int value) {
        try {
            return amountFormat.parse(Integer.toString(value), bookkeeping.getCurrency());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected void checkAmount(int expectedAmountInt, Amount actualAmount) throws ParseException {
        Amount expectedAmount = createAmount(expectedAmountInt);
        assertEquals(amountFormat.formatAmount(expectedAmount),
                amountFormat.formatAmount(actualAmount));
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
            assertNull("Expected null but was " + DateUtil.formatDateYYYYMMDD(actual), actual);
        } else {
            assertNotNull("Expected " + DateUtil.formatDateYYYYMMDD(expected)
                    + " but was null", actual);
        }

        assertEquals("Expected " + DateUtil.formatDateYYYYMMDD(expected)
                + " but was " + DateUtil.formatDateYYYYMMDD(actual),
                0, DateUtil.compareDayOfYear(expected, actual));
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
        assertEquals(expected.getAmountToBePaid(), actual.getAmountToBePaid());
        assertEquals(expected.getConcerningPartyId(), actual.getConcerningPartyId());
        assertEquals(expected.getPayingPartyId(), actual.getPayingPartyId());
        assertEquals(expected.getIssueDate(), actual.getIssueDate());
    }

    private void assertEqualPayment(Payment expected, Payment actual) {
        assertEquals(expected.getAmount(), actual.getAmount());
        assertEquals(expected.getDate(), actual.getDate());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getInvoiceId(), actual.getInvoiceId());
    }

}
