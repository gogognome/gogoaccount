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

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;

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
		public String id;
		public String description;
		public Amount debetAmount;
		public Amount creditAmount;
		public Invoice invoice;

		@Override
		public String toString() {
			return DateUtil.formatDateYYYYMMDD(date) + ' ' + id + ' ' + description
				+ ' ' + debetAmount + ' ' + creditAmount
				+ (invoice != null ? ' ' + invoice.getId() : "");
		}
	}

	private final Date endDate;
	private final Currency currency;

	private List<Account> assets;
	private List<Account> liabilities;
	private List<Account> expenses;
	private List<Account> revenues;

	private Account resultOfOperationsAccount;
	private Amount resultOfOperations;

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

	private TextResource textResource = Factory.getInstance(TextResource.class);

	public Report(Date endDate, Currency currency) {
		super();
		this.endDate = endDate;
		this.currency = currency;
	}

	public Date getEndDate() {
		return endDate;
	}

	public List<Account> getAssets() {
		return assets;
	}

	public List<Account> getAssetsInclLossAccount() {
		if (resultOfOperationsAccount != null && resultOfOperationsAccount.isDebet()) {
			List<Account> assets = new ArrayList<Account>(getAssets());
			assets.add(resultOfOperationsAccount);
			return assets;
		} else {
			return getAssets();
		}
	}

	void setAssets(List<Account> assets) {
		this.assets = assets;
	}

	public List<Account> getLiabilities() {
		return liabilities;
	}

	public List<Account> getLiabilitiesInclProfitAccount() {
		if (resultOfOperationsAccount != null && resultOfOperationsAccount.isCredit()) {
			List<Account> liabilities = new ArrayList<Account>(getLiabilities());
			liabilities.add(resultOfOperationsAccount);
			return liabilities;
		} else {
			return getLiabilities();
		}
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

	public List<Account> getAllAccounts() {
		List<Account> accounts = new ArrayList<Account>(getAssets());
		accounts.addAll(getLiabilities());
		accounts.addAll(getExpenses());
		accounts.addAll(getRevenues());
		return accounts;
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

	void addLedgerLineForAccount(Account account, LedgerLine line) {
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

	void addPayment(Invoice invoice, Amount amount) {
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

	void determineResultOfOperations() {
        resultOfOperations = Amount.getZero(currency);
        for (Account a : assets) {
        	resultOfOperations = resultOfOperations.add(getAmount(a));
        }
        for (Account a : liabilities) {
        	resultOfOperations = resultOfOperations.subtract(getAmount(a));
        }

        if (resultOfOperations.isPositive()) {
    		resultOfOperationsAccount =
    			new Account("", textResource.getString("gen.profit"), AccountType.LIABILITY);
    		setAmount(resultOfOperationsAccount, resultOfOperations);
        } else if (resultOfOperations.isNegative()) {
        	resultOfOperationsAccount =
    			new Account("", textResource.getString("gen.loss"), AccountType.ASSET);
    		setAmount(resultOfOperationsAccount, resultOfOperations.negate());
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

	public Amount getResultOfOperations() {
		return resultOfOperations;
	}

	public Amount getTotalAssets() {
		return getTotalOfAccounts(getAssetsInclLossAccount());
	}

	public Amount getTotalLiabilities() {
		return getTotalOfAccounts(getLiabilitiesInclProfitAccount());
	}

	public Amount getTotalExpenses() {
		return getTotalOfAccounts(getExpenses());
	}

	public Amount getTotalRevenues() {
		return getTotalOfAccounts(getRevenues());
	}

	public Amount getTotalOfAccounts(List<Account> accounts) {
		Amount total = Amount.getZero(currency);
		for (Account a : accounts) {
			total = total.add(getAmount(a));
		}
		return total;
	}

    /**
     * Gets the total amount of the debtors.
     * @return the total amount.
     */
    public Amount getTotalDebtors() {
        Amount total = Amount.getZero(currency);
        for (Amount a : debtorToRemainingAmount.values()) {
        	total = total.add(a);
        }
        return total;
    }

    /**
     * Gets the total amount of the creditors.
     * @return the total amount.
     */
    public Amount getTotalCreditors() {
        Amount total = Amount.getZero(currency);
        for (Amount a : creditorToRemainingAmount.values()) {
        	total = total.add(a);
        }
        return total;
    }
}
