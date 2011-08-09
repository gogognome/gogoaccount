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

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package nl.gogognome.gogoaccount.businessobjects;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.gogognome.gogoaccount.businessobjects.Report.LedgerLine;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;
import cf.engine.Payment;

/**
 * Class to build Report instances.
 *
 * @author Sander Kooijmans
 */
public class ReportBuilder {

	private final Database database;
	private final Report report;

	private Map<Account, Amount> accountToTotalDebet = new HashMap<Account, Amount>();
	private Map<Account, Amount> accountToTotalCredit = new HashMap<Account, Amount>();
	private Map<Account, Amount> accountToStartDebet = new HashMap<Account, Amount>();
	private Map<Account, Amount> accountToStartCredit = new HashMap<Account, Amount>();

	private TextResource textResource = Factory.getInstance(TextResource.class);

	public ReportBuilder(Database database, Date date) {
		this.database = database;
		this.report = new Report(date, database.getCurrency());
	}

	public Report build() {
		report.removeCompletedInvoices();
		report.determineResultOfOperations();
		addFootersToLedgerLines();
		determineBalanceForDebtorsAndCreditors();
		return report;
	}

	private void determineBalanceForDebtorsAndCreditors() {
		for (Invoice invoice : report.getInvoices()) {
			Party p = invoice.getPayingParty();
			Amount amount = report.getRemaingAmountForInvoice(invoice);
			if (amount.isPositive()) {
				Amount balance = report.getBalanceForDebtor(p);
				balance = balance.add(amount);
				report.setBalanceForDebtor(p, balance);
			} else if (amount.isNegative()) {
				Amount balance = report.getBalanceForCreditor(p);
				balance = balance.subtract(amount);
				report.setBalanceForCreditor(p, balance);
			}
		}
	}

	public void setAssets(List<Account> assets) {
		report.setAssets(assets);
	}

	public void setLiabilities(List<Account> liabilities) {
		report.setLiabilities(liabilities);
	}

	public void setExpenses(List<Account> expenses) {
		report.setExpenses(expenses);
	}

	public void setRevenues(List<Account> revenues) {
		report.setRevenues(revenues);
	}

	public void addJournal(Journal journal) {
		for (JournalItem item : journal.getItems()) {
			addJournalItem(journal, item);
		}
	}

	private void addJournalItem(Journal journal, JournalItem item) {
		addAmountToTotalForAccount(item);
		addLedgerLineForAccount(journal, item);
		addAmountToTotalDebetOrCredit(item); // must come after ledger line has been added
		// otherwise the amount for the first line is added to the start line
	}

	private void addAmountToTotalForAccount(JournalItem item) {
		Account account = item.getAccount();
		Amount accountAmount = report.getAmount(account);

		if (account.isDebet() == item.isDebet()) {
			accountAmount = accountAmount.add(item.getAmount());
		} else {
			accountAmount = accountAmount.subtract(item.getAmount());
		}

		report.setAmount(account, accountAmount);
	}

	private void addLedgerLineForAccount(Journal journal, JournalItem item) {
		if (DateUtil.compareDayOfYear(journal.getDate(), database.getStartOfPeriod()) >= 0) {
			Account account = item.getAccount();
			if (!hasStartBalanceLineBeenAdded(account)) {
				addStartLedgerLineForAccount(account, accountToTotalDebet.get(account),
						accountToTotalCredit.get(account));
			}
			Invoice invoice = database.getInvoice(item.getInvoiceId());
			addLedgerLineForAccount(account, journal, item, invoice);
		}
	}

	void addStartLedgerLineForAccount(Account account, Amount debetAmount, Amount creditAmount) {
		LedgerLine line = new LedgerLine();
		line.description = textResource.getString("rep.startBalance");
		setAmountInLedgerLine(line, account, debetAmount, creditAmount);
		report.addLedgerLineForAccount(account, line);

		accountToStartDebet.put(account, nullToZero(debetAmount));
		accountToStartCredit.put(account, nullToZero(creditAmount));
	}

	private void setAmountInLedgerLine(LedgerLine line, Account account,
			Amount debetAmount, Amount creditAmount) {
		Amount balance = nullToZero(debetAmount).subtract(nullToZero(creditAmount));
		line.debetAmount = balance.isPositive() || (balance.isZero() && account.isDebet()) ? balance : null;
		line.creditAmount = balance.isNegative() || (balance.isZero() && account.isCredit()) ? balance.negate() : null;
	}

	void addLedgerLineForAccount(Account account, Journal journal, JournalItem item, Invoice invoice) {
		LedgerLine line = new LedgerLine();
		line.date = journal.getDate();
		line.id = journal.getId();
		line.description = journal.getDescription();
		line.debetAmount = item.isDebet() ? item.getAmount() : null;
		line.creditAmount = item.isCredit() ? item.getAmount() : null;
		line.invoice = invoice;
		report.addLedgerLineForAccount(account, line);
	}

	private void addAmountToTotalDebetOrCredit(JournalItem item) {
		Account account = item.getAccount();
		if (item.isDebet()) {
			Amount totalAmount = nullToZero(accountToTotalDebet.get(account)).add(item.getAmount());
			accountToTotalDebet.put(account, totalAmount);
		} else {
			Amount totalAmount = nullToZero(accountToTotalCredit.get(account)).add(item.getAmount());
			accountToTotalCredit.put(account, totalAmount);
		}
	}

	private boolean hasStartBalanceLineBeenAdded(Account account) {
		return !report.getLedgerLinesForAccount(account).isEmpty();
	}

	public void addInvoice(Invoice invoice) {
		report.addInvoice(invoice);

		for (Payment p : invoice.getPayments()) {
			if (DateUtil.compareDayOfYear(p.getDate(), report.getEndDate()) <= 0) {
				report.addPayment(invoice, p.getAmount());
			}
		}
	}

	private Amount nullToZero(Amount amount) {
		if (amount == null) {
			amount = Amount.getZero(database.getCurrency());
		}
		return amount;
	}

	private void addFootersToLedgerLines() {
		for (Account account : database.getAllAccounts()) {
			if (!hasStartBalanceLineBeenAdded(account)) {
				addStartLedgerLineForAccount(account,
						accountToTotalDebet.get(account), accountToTotalCredit.get(account));
			}
			addLedgerLineWithTotalMutations(account);
			addEndLedgerLineForAccount(account);
		}
	}

	private void addLedgerLineWithTotalMutations(Account a) {
		LedgerLine line = new LedgerLine();
		line.description = textResource.getString("rep.totalMutations");
		line.debetAmount = nullToZero(accountToTotalDebet.get(a)).subtract(accountToStartDebet.get(a));
		line.creditAmount = nullToZero(accountToTotalCredit.get(a)).subtract(accountToStartCredit.get(a));
		report.addLedgerLineForAccount(a, line);
	}

	private void addEndLedgerLineForAccount(Account account) {
		LedgerLine line = new LedgerLine();
		line.description = textResource.getString("rep.endBalance");
		setAmountInLedgerLine(line, account,
				accountToTotalDebet.get(account), accountToTotalCredit.get(account));
		report.addLedgerLineForAccount(account, line);
	}
}
