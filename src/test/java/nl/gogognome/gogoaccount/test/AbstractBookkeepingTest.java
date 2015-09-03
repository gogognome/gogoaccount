package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
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
    private final PartyService partyService = new PartyService();

	protected Document document;
    protected Bookkeeping bookkeeping;

	protected AmountFormat amountFormat;

	protected Amount zero;

	@Before
	public void initBookkeeping() throws Exception {
		initFactory();

        BookkeepingService bookkeepingService = new BookkeepingService();
        document = bookkeepingService.createNewDatabase("New bookkeeping");
		bookkeeping = configurationService.getBookkeeping(document);
		bookkeeping.setCurrency(Currency.getInstance("EUR"));
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

		document.databaseConsistentWithFile();

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
		List<JournalItem> items = new ArrayList<>();
		items.add(createItem(20, "190", true));
		items.add(createItem(20, "300", false));
		Journal journal = new Journal("t1", "Payment", DateUtil.createDate(2011, 3, 5),
				items, "inv1");

		String[] descriptions = new String[] { "Contributie 2011", "Contributie" };
		Amount[] amounts = new Amount[] { null, createAmount(20) };
        Party party = new PartyService().getParty(document, "1101");
		Invoice invoice = new Invoice(journal.getIdOfCreatedInvoice(), party, party,
				createAmount(20), journal.getDate(), descriptions, amounts);
		document.addInvoicAndJournal(invoice, journal);

		items = new ArrayList<>();
		items.add(createItem(10, "101", true, "inv1", "pay1"));
		items.add(createItem(10, "190", false));
		journal = new Journal("t2", "Payment", DateUtil.createDate(2011, 5, 10), items, null);
		document.addJournal(journal, true);
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
		List<JournalItem> items = new ArrayList<>();
		items.add(createItem(100, "100", true));
		items.add(createItem(300, "101", true));
		items.add(createItem(400, "200", false));
		Journal journal = new Journal("start", "Start balance",
				DateUtil.addDays(bookkeeping.getStartOfPeriod(), -1),
				items, null);
		document.addJournal(journal, false);
	}

	protected JournalItem createItem(int amountInt, String accountId, boolean debet) throws ServiceException {
		return ServiceTransaction.withResult(() -> {
			Account account = configurationService.getAccount(document, accountId);
			Amount amount = createAmount(amountInt);
			return new JournalItem(amount, account, debet);
		});
	}

	protected JournalItem createItem(int amountInt, String accountId, boolean debet, String invoiceId, String paymentId) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Account account = configurationService.getAccount(document, accountId);
            Amount amount = createAmount(amountInt);
            return new JournalItem(amount, account, debet, invoiceId, paymentId);
        });
	}

	protected Amount createAmount(int value) throws ParseException {
		return amountFormat.parse(Integer.toString(value), bookkeeping.getCurrency());
	}

	protected void checkAmount(int expectedAmountInt, Amount actualAmount) throws ParseException {
		Amount expectedAmount = createAmount(expectedAmountInt);
		assertEquals(amountFormat.formatAmount(expectedAmount),
				amountFormat.formatAmount(actualAmount));
	}

	protected Journal findJournal(String id) {
		for (Journal j : document.getJournals()) {
			if (j.getId().equals(id)) {
				return j;
			}
		}
		return null;
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

        assertEquals(expected.getImportedTransactionAccountToAccountMap().keySet().toString(),
                actual.getImportedTransactionAccountToAccountMap().keySet().toString());
        assertEquals(expected.getImportedTransactionAccountToAccountMap().values().toString(),
                actual.getImportedTransactionAccountToAccountMap().values().toString());
        assertEquals(Arrays.asList(expected.getInvoices()), Arrays.asList(actual.getInvoices()));
        assertEquals(expected.getJournals().toString(), actual.getJournals().toString());

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

	public void assertEqualJournal(Journal expected, Journal actual) {
		assertEquals(expected.getId(), actual.getId());
		assertEqualDayOfYear(expected.getDate(), actual.getDate());
		assertEquals(expected.getDescription(), actual.getDescription());
		assertEquals(expected.getIdOfCreatedInvoice(), actual.getIdOfCreatedInvoice());
		assertEquals(expected.getItems().length, actual.getItems().length);

		JournalItem[] expectedItems = expected.getItems();
		JournalItem[] actualItems = actual.getItems();
		for (int i=0; i<expectedItems.length; i++) {
			assertEqualItem(expectedItems[i], actualItems[i]);
		}
	}

	public void assertEqualItem(JournalItem expected, JournalItem actual) {
		assertEquals(expected.getAccount(), actual.getAccount());
		assertEquals(expected.getAmount(), actual.getAmount());
		assertEquals(expected.getInvoiceId(), actual.getInvoiceId());
		assertEquals(expected.getPaymentId(), actual.getPaymentId());
		assertEquals(expected.isDebet(), actual.isDebet());
	}

	public void assertEqualInvoice(Invoice expected, Invoice actual) {
		assertEquals(expected.getId(), actual.getId());
		assertEquals(expected.getAmountToBePaid(), actual.getAmountToBePaid());
		assertEquals(expected.getConcerningParty(), actual.getConcerningParty());
		assertEquals(expected.getPayingParty(), actual.getPayingParty());
		assertEquals(Arrays.toString(expected.getAmounts()),
				Arrays.toString(actual.getAmounts()));
		assertEquals(Arrays.toString(expected.getDescriptions()),
				Arrays.toString(actual.getDescriptions()));
	}

}
