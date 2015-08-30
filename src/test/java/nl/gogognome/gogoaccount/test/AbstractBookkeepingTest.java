/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public Licensen
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.database.AccountDAO;
import nl.gogognome.gogoaccount.database.Database;
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
 * Abstract class that sets up a test account.
 *
 * @author Sander Kooijmans
 */
public abstract class AbstractBookkeepingTest {

	protected Database database;

	protected AmountFormat amountFormat;

	protected Amount zero;

	@Before
	public void initBookkeeping() throws Exception {
		initFactory();

        BookkeepingService bookkeepingService = new BookkeepingService();
        database = bookkeepingService.createNewDatabase();
        database.setCurrency(Currency.getInstance("EUR"));
        database.setStartOfPeriod(DateUtil.createDate(2011, 1, 1));
        database.setParties(createParties());

        for (Account account : createAccounts()) {
            bookkeepingService.createAccount(database, account);
        }

		addStartBalance();
		addJournals();

		database.databaseConsistentWithFile();

		zero = Amount.getZero(database.getCurrency());
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
		Invoice invoice = new Invoice(journal.getIdOfCreatedInvoice(),
				database.getParty("1101"), database.getParty("1101"),
				createAmount(20), journal.getDate(), descriptions, amounts);
		database.addInvoicAndJournal(invoice, journal);

		items = new ArrayList<>();
		items.add(createItem(10, "101", true, "inv1", "pay1"));
		items.add(createItem(10, "190", false));
		journal = new Journal("t2", "Payment", DateUtil.createDate(2011, 5, 10), items, null);
		database.addJournal(journal, true);
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

	private Party[] createParties() {
		return new Party[] {
			new Party("1101", "Pietje Puk", "Eikenlaan 64", "1535 DS", "Den Bosch",
					DateUtil.createDate(1980, 2, 23), null, "Is vaak afwezig"),
			new Party("1102", "Jan Pieterszoon", "Sterrenlaan 532", "5217 FG", "Eindhoven",
					null, null, null)
		};
	}

	private void addStartBalance() throws Exception {
		List<JournalItem> items = new ArrayList<>();
		items.add(createItem(100, "100", true));
		items.add(createItem(300, "101", true));
		items.add(createItem(400, "200", false));
		Journal journal = new Journal("start", "Start balance",
				DateUtil.addDays(database.getStartOfPeriod(), -1),
				items, null);
		database.addJournal(journal, false);
	}

	protected JournalItem createItem(int amountInt, String accountId, boolean debet) throws ServiceException {
		return ServiceTransaction.withResult(() -> {
			Account account = new AccountDAO(database).get(accountId);
			Amount amount = createAmount(amountInt);
			return new JournalItem(amount, account, debet);
		});
	}

	protected JournalItem createItem(int amountInt, String accountId, boolean debet, String invoiceId, String paymentId) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Account account = new AccountDAO(database).get(accountId);
            Amount amount = createAmount(amountInt);
            return new JournalItem(amount, account, debet, invoiceId, paymentId);
        });
	}

	protected Amount createAmount(int value) throws ParseException {
		return amountFormat.parse(Integer.toString(value), database.getCurrency());
	}

	protected void checkAmount(int expectedAmountInt, Amount actualAmount) throws ParseException {
		Amount expectedAmount = createAmount(expectedAmountInt);
		assertEquals(amountFormat.formatAmount(expectedAmount),
				amountFormat.formatAmount(actualAmount));
	}

	protected Journal findJournal(String id) {
		for (Journal j : database.getJournals()) {
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
	public void assertEqualDatabase(Database expected, Database actual) throws ServiceException, SQLException {
        BookkeepingService bookkeepingService = new BookkeepingService();
		assertEquals(bookkeepingService.findAllAccounts(expected).toString(), bookkeepingService.findAllAccounts(actual).toString());
		assertEquals(Arrays.toString(expected.getParties()), Arrays.toString(actual.getParties()));
		assertEquals(expected.getCurrency(), actual.getCurrency());
		assertEquals(expected.getDescription(), actual.getDescription());
		assertEquals(expected.getImportedTransactionAccountToAccountMap().keySet().toString(),
				actual.getImportedTransactionAccountToAccountMap().keySet().toString());
		assertEquals(expected.getImportedTransactionAccountToAccountMap().values().toString(),
				actual.getImportedTransactionAccountToAccountMap().values().toString());
		assertEquals(Arrays.asList(expected.getInvoices()), Arrays.asList(actual.getInvoices()));
		assertEquals(expected.getJournals().toString(), actual.getJournals().toString());
		assertEqualDayOfYear(expected.getStartOfPeriod(), actual.getStartOfPeriod());

		Report expectedReport = bookkeepingService.createReport(expected, DateUtil.addYears(expected.getStartOfPeriod(), 1));
		Report actualReport = bookkeepingService.createReport(actual, DateUtil.addYears(expected.getStartOfPeriod(), 1));

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
