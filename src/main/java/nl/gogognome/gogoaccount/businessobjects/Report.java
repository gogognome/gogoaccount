package nl.gogognome.gogoaccount.businessobjects;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;

import java.util.*;

/**
 * <p>This class contains the financial report for a specific date. It contains:</p>
 *
 * <ul>
 *   <li>the balance sheet</li>
 *   <li>the operational result</li>
 *   <li>all mutations of all accounts</li>
 *   <li>the debtors and creditors that have open invoices</li>
 * </ul>
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

    private Date endDate;

    private List<Account> assets;
    private List<Account> liabilities;
    private List<Account> expenses;
    private List<Account> revenues;

    private Account resultOfOperationsAccount;
    private Amount resultOfOperations;

    private final List<Invoice> invoices = new ArrayList<>();

    private final Map<Account, Amount> accountToAmount = new HashMap<>();

    private final Map<Account, List<LedgerLine>> accountToLedgerLines = new HashMap<>();

    private final Map<Invoice, Amount> invoiceToRemainingAmount = new HashMap<>();

    private final Map<Party, Amount> debtorToRemainingAmount = new HashMap<>();

    private final Map<Party, Amount> creditorToRemainingAmount = new HashMap<>();

    private final TextResource textResource = Factory.getInstance(TextResource.class);

    public Report() {
        super();
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public List<Account> getAssets() {
        return assets;
    }

    public List<Account> getAssetsInclLossAccount() {
        if (resultOfOperationsAccount != null && resultOfOperationsAccount.isDebet()) {
            List<Account> assets = new ArrayList<>(getAssets());
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
            List<Account> liabilities = new ArrayList<>(getLiabilities());
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
        List<Account> accounts = new ArrayList<>(getAssets());
        accounts.addAll(getLiabilities());
        accounts.addAll(getExpenses());
        accounts.addAll(getRevenues());
        return accounts;
    }

    public Amount getAmount(Account account) {
        Amount a = accountToAmount.get(account);
        if (a == null) {
            a = Amount.ZERO;
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
        return accountToLedgerLines.computeIfAbsent(account, k -> new ArrayList<>());
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
        List<Invoice> invoicesToBeRemoved = new ArrayList<>();
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
        resultOfOperations = Amount.ZERO;
        for (Account a : assets) {
            resultOfOperations = resultOfOperations.add(getAmount(a));
        }
        for (Account a : liabilities) {
            resultOfOperations = resultOfOperations.subtract(getAmount(a));
        }

        if (resultOfOperations.isPositive()) {
            resultOfOperationsAccount =
                new Account("__profit__", textResource.getString("gen.profit"), AccountType.LIABILITY);
            resultOfOperationsAccount.setResultOfOperations(true);
            setAmount(resultOfOperationsAccount, resultOfOperations);
        } else if (resultOfOperations.isNegative()) {
            resultOfOperationsAccount =
                new Account("__loss__", textResource.getString("gen.loss"), AccountType.ASSET);
            resultOfOperationsAccount.setResultOfOperations(true);
            setAmount(resultOfOperationsAccount, resultOfOperations.negate());
        }
    }

    public Amount getRemainingAmountForInvoice(Invoice invoice) {
        Amount amount = invoiceToRemainingAmount.get(invoice);
        if (amount == null) {
            amount = Amount.ZERO;
        }
        return amount;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public Amount getBalanceForDebtor(Party debtor) {
        Amount amount = debtorToRemainingAmount.get(debtor);
        if (amount == null) {
            amount = Amount.ZERO;
        }
        return amount;
    }

    void setBalanceForDebtor(Party debtor, Amount amount) {
        debtorToRemainingAmount.put(debtor, amount);
    }

    public Amount getBalanceForCreditor(Party creditor) {
        Amount amount = creditorToRemainingAmount.get(creditor);
        if (amount == null) {
            amount = Amount.ZERO;
        }
        return amount;
    }

    void setBalanceForCreditor(Party creditor, Amount amount) {
        creditorToRemainingAmount.put(creditor, amount);
    }

    public List<Party> getDebtors() {
        List<Party> debtors = new ArrayList<>(debtorToRemainingAmount.keySet());
        sortParties(debtors);
        return debtors;
    }

    public List<Party> getCreditors() {
        List<Party> creditors = new ArrayList<>(creditorToRemainingAmount.keySet());
        sortParties(creditors);
        return creditors;
    }

    private void sortParties(List<Party> parties) {
        Collections.sort(parties);
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
        Amount total = Amount.ZERO;
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
        Amount total = Amount.ZERO;
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
        Amount total = Amount.ZERO;
        for (Amount a : creditorToRemainingAmount.values()) {
            total = total.add(a);
        }
        return total;
    }
}
