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
import java.util.List;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
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
	
	public ReportBuilder(Database database, Date date) {
		this.database = database;
		this.report = new Report(date, database.getCurrency());
	}
	
	public Report build() {
		report.removeCompletedInvoices();
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
		Account account = item.getAccount();
		Amount amount = report.getAmount(account);
		amount = amount.add(item.getAmount());
		report.setAmount(account, amount);
		
		Invoice invoice = database.getInvoice(item.getInvoiceId()); 
		report.addLedgerLineForAccount(account, journal, item, invoice);
	}

	public void addInvoice(Invoice invoice) {
		report.addInvoice(invoice);
		
		for (Payment p : invoice.getPayments()) {
			if (DateUtil.compareDayOfYear(p.getDate(), report.getDate()) <= 0) {
				report.addPayment(invoice, p.getAmount());
			}
		}
	}

}
