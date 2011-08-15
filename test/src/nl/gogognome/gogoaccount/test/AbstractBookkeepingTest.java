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

import static junit.framework.Assert.assertEquals;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;

import org.junit.Before;

import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;
import cf.engine.Account.Type;

/**
 * Abstract class that sets up a test account.
 *
 * @author Sander Kooijmans
 */
public class AbstractBookkeepingTest {

	protected Database database;

	protected AmountFormat amountFormat;

	protected Amount zero;

	@Before
	public void initBookkeeping() throws Exception {
		initFactory();

		database = new Database();
		database.setCurrency(Currency.getInstance("EUR"));
		database.setStartOfPeriod(DateUtil.createDate(2011, 1, 1));
		database.setAssets(createAssets());
		database.setLiabilities(createLiabilities());
		database.setExpenses(createExpenses());
		database.setRevenues(createRevenues());
		database.setParties(createParties());

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
		List<JournalItem> items = new ArrayList<JournalItem>();
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

		items = new ArrayList<JournalItem>();
		items.add(createItem(10, "101", true, "inv1", "pay1"));
		items.add(createItem(10, "190", false));
		journal = new Journal("t1", "Payment", DateUtil.createDate(2011, 5, 10), items, null);
		database.addJournal(journal, true);
	}

	private List<Account> createAssets() {
		return Arrays.asList(
				new Account("100", "Kas", Type.ASSET),
				new Account("101", "Betaalrekening", Type.ASSET),
				new Account("190", "Debiteuren", Type.ASSET)
		);
	}

	private List<Account> createLiabilities() {
		return Arrays.asList(
				new Account("200", "Eigen vermogen", Type.LIABILITY),
				new Account("290", "Crediteuren", Type.LIABILITY)
		);
	}

	private List<Account> createExpenses() {
		return Arrays.asList(
				new Account("400", "Zaalhuur", Type.EXPENSE),
				new Account("490", "Onvoorzien", Type.EXPENSE)
		);
	}

	private List<Account> createRevenues() {
		return Arrays.asList(
				new Account("300", "Contributie", Type.REVENUE),
				new Account("390", "Onvoorzien", Type.REVENUE)
		);
	}

	private Party[] createParties() {
		return new Party[] {
			new Party("1101", "Pietje Puk"),
			new Party("1102", "Jan Pieterszoon"),
		};
	}

	private void addStartBalance() throws Exception {
		List<JournalItem> items = new ArrayList<JournalItem>();
		items.add(createItem(100, "100", true));
		items.add(createItem(300, "101", true));
		items.add(createItem(400, "200", false));
		Journal journal = new Journal("start", "Start balance",
				DateUtil.addDays(database.getStartOfPeriod(), -1),
				items, null);
		database.addJournal(journal, false);
	}

	protected JournalItem createItem(int amountInt, String accountId, boolean debet) throws ParseException {
		Account account = database.getAccount(accountId);
		Amount amount = createAmount(amountInt);
		return new JournalItem(amount, account, debet);
	}

	protected JournalItem createItem(int amountInt, String accountId, boolean debet,
			String invoiceId, String paymentId) throws ParseException {
		Account account = database.getAccount(accountId);
		Amount amount = createAmount(amountInt);
		return new JournalItem(amount, account, debet, invoiceId, paymentId);
	}

	protected Amount createAmount(int value) throws ParseException {
		return amountFormat.parse(Integer.toString(value), database.getCurrency());
	}

	protected void checkAmount(int expectedAmountInt, Amount actualAmount) throws ParseException {
		Amount expectedAmount = createAmount(expectedAmountInt);
		assertEquals(amountFormat.formatAmount(expectedAmount),
				amountFormat.formatAmount(actualAmount));
	}
}
