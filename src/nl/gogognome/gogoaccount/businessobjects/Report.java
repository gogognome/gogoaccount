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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.gogognome.lib.text.Amount;
import cf.engine.Account;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;

/**
 * This class contains the financial report for a specific date. 
 * 
 * It contains:
 * - the balance sheet
 * - the operational result
 * - all mutations of all accounts
 * - the debtors and creditors that have open invoices
 *   
 * @author Sander Kooijmans
 */
public class Report {

	public static class LedgerLine {
		public Date date;
		public String description;
		public Amount debetAmount;
		public Amount creditAmount;
		public Invoice invoice;
	}
	
	private final Date date;
	private final Currency currency;
	
	private List<Account> assets;
	private List<Account> liabilities;
	private List<Account> expenses;
	private List<Account> revenues;
	
	private List<Invoice> invoices = new ArrayList<Invoice>();
	
	private Map<Account, Amount> accountToAmount = new HashMap<Account, Amount>();
	
	private Map<Account, List<LedgerLine>> accountToLedgerLines = 
		new HashMap<Account, List<LedgerLine>>();

	private Map<Invoice, Amount> invoiceToRemainingAmount = 
		new HashMap<Invoice, Amount>();
	
	private Map<Party, Amount> debtorToRemainingAmount =
		new HashMap<Party, Amount>();

	private Map<Party, Amount> creditorToRemainingAmount =
		new HashMap<Party, Amount>();

	public Report(Date date, Currency currency) {
		super();
		this.date = date;
		this.currency = currency;
	}

	public Date getDate() {
		return date;
	}
	
	public List<Account> getAssets() {
		return assets;
	}
	
	void setAssets(List<Account> assets) {
		this.assets = assets;
	}
	
	public List<Account> getLiabilities() {
		return liabilities;
	}
	
	void setLiabilities(List<Account> liabilities) {
		this.liabilities = liabilities;
	}
	
	public List<Account> getExpenses() {
		return expenses;
	}
	
	void setExpenses(List<Account> expenses) {
		this.expenses = expenses;
	}
	
	public List<Account> getRevenues() {
		return revenues;
	}
	
	void setRevenues(List<Account> revenues) {
		this.revenues = revenues;
	}
	
	public Amount getAmount(Account account) {
		Amount a = accountToAmount.get(account);
		if (a == null) {
			a = Amount.getZero(currency);
		}
		return a;
	}
	
	void setAmount(Account account, Amount amount) {
		accountToAmount.put(account, amount);
	}
	
	void addLedgerLineForAccount(Account account, Journal journal, JournalItem item, Invoice invoice) {
		LedgerLine line = new LedgerLine();
		line.date = journal.getDate();
		line.description = journal.getDescription();
		line.debetAmount = item.isDebet() ? item.getAmount() : null;
		line.creditAmount = item.isCredit() ? item.getAmount() : null;
		line.invoice = invoice;
		getLedgerLinesForAccount(account).add(line);
	}
	
	public List<LedgerLine> getLedgerLinesForAccount(Account account) {
		List<LedgerLine> items = accountToLedgerLines.get(account);
		if (items == null) {
			items = new ArrayList<LedgerLine>();
			accountToLedgerLines.put(account, items);
		}
		return items;
	}
	
	void addInvoice(Invoice invoice) {
		invoices.add(invoice);
		invoiceToRemainingAmount.put(invoice, invoice.getAmountToBePaid());
	}
	
	public void addPayment(Invoice invoice, Amount amount) {
		Amount curAmount = invoiceToRemainingAmount.get(invoice);
		Amount newAmount = curAmount.subtract(amount);
		invoiceToRemainingAmount.put(invoice, newAmount);
	}

	void removeCompletedInvoices() {
		List<Invoice> invoicesToBeRemoved = new ArrayList<Invoice>();
		for (Map.Entry<Invoice, Amount> entry : invoiceToRemainingAmount.entrySet()) {
			if (entry.getValue().isZero()) {
				invoicesToBeRemoved.add(entry.getKey());
			}
		}
		
		for (Invoice invoice : invoicesToBeRemoved) {
			invoiceToRemainingAmount.remove(invoice);
		}
	}
	
	public Amount getRemaingAmountForInvoice(Invoice invoice) {
		Amount amount = invoiceToRemainingAmount.get(invoice);
		if (amount == null) {
			amount = Amount.getZero(currency);
		}
		return amount;
	}
	
	public List<Invoice> getInvoices() {
		return invoices;
	}
	
	public Amount getBalanceForDebtor(Party debtor) {
		Amount amount = debtorToRemainingAmount.get(debtor);
		if (amount == null) {
			amount = Amount.getZero(currency);
		}
		return amount;
	}
	
	void setBalanceForDebtor(Party debtor, Amount amount) {
		debtorToRemainingAmount.put(debtor, amount);
	}
	
	public Amount getBalanceForCreditor(Party creditor) {
		Amount amount = creditorToRemainingAmount.get(creditor);
		if (amount == null) {
			amount = Amount.getZero(currency);
		}
		return amount;
	}

	void setBalanceForCreditor(Party creditor, Amount amount) {
		creditorToRemainingAmount.put(creditor, amount);
	}
	
	public List<Party> getDebtors() {
		List<Party> debtors = new ArrayList<Party>(debtorToRemainingAmount.keySet());
		sortParties(debtors);
		return debtors;
	}
	
	public List<Party> getCreditors() {
		List<Party> creditors = new ArrayList<Party>(creditorToRemainingAmount.keySet());
		sortParties(creditors);
		return creditors;
	}

	private void sortParties(List<Party> parties) {
		Collections.<Party>sort(parties);
	}
}

