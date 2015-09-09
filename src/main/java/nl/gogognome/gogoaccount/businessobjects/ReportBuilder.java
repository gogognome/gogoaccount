package nl.gogognome.gogoaccount.businessobjects;

import nl.gogognome.gogoaccount.businessobjects.Report.LedgerLine;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportBuilder {

	private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

	private final Document document;
	private final Bookkeeping bookkeeping;
	private final Report report;

	private Map<Account, Amount> accountToTotalDebet = new HashMap<>();
	private Map<Account, Amount> accountToTotalCredit = new HashMap<>();
	private Map<Account, Amount> accountToStartDebet = new HashMap<>();
	private Map<Account, Amount> accountToStartCredit = new HashMap<>();

	private TextResource textResource = Factory.getInstance(TextResource.class);

	public ReportBuilder(Document document, Date date) throws ServiceException {
		this.document = document;
        bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
		this.report = new Report(date, bookkeeping.getCurrency());
	}

	public Report build() throws ServiceException {
		report.removeCompletedInvoices();
		report.determineResultOfOperations();
		addFootersToLedgerLines();
		determineBalanceForDebtorsAndCreditors();
		return report;
	}

	private void determineBalanceForDebtorsAndCreditors() throws ServiceException {
		for (Invoice invoice : report.getInvoices()) {
			Party party = partyService.getParty(document, invoice.getPayingPartyId() != null ? invoice.getPayingPartyId() : invoice.getConcerningPartyId());
			Amount amount = report.getRemaingAmountForInvoice(invoice);
			if (amount.isPositive()) {
				Amount balance = report.getBalanceForDebtor(party);
				balance = balance.add(amount);
				report.setBalanceForDebtor(party, balance);
			} else if (amount.isNegative()) {
				Amount balance = report.getBalanceForCreditor(party);
				balance = balance.subtract(amount);
				report.setBalanceForCreditor(party, balance);
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

	public void addJournal(Journal journal) throws ServiceException {
		for (JournalItem item : journal.getItems()) {
			addJournalItem(journal, item);
		}
	}

	private void addJournalItem(Journal journal, JournalItem item) throws ServiceException {
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

	private void addLedgerLineForAccount(Journal journal, JournalItem item) throws ServiceException {
		if (DateUtil.compareDayOfYear(journal.getDate(), bookkeeping.getStartOfPeriod()) >= 0) {
			Account account = item.getAccount();
			if (!hasStartBalanceLineBeenAdded(account)) {
				addStartLedgerLineForAccount(account, accountToTotalDebet.get(account),
						accountToTotalCredit.get(account));
			}
			Invoice invoice = null;
            if (item.getInvoiceId() != null) {
                invoice = invoiceService.getInvoice(document, item.getInvoiceId());
            } else  if (journal.getIdOfCreatedInvoice() == null) {
				invoice = invoiceService.getInvoice(document, journal.getIdOfCreatedInvoice());
			}
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

	public void addInvoice(Invoice invoice) throws ServiceException {
		report.addInvoice(invoice);

		invoiceService.findPayments(document, invoice).stream()
				.filter(p -> DateUtil.compareDayOfYear(p.getDate(), report.getEndDate()) <= 0)
				.forEach(p -> report.addPayment(invoice, p.getAmount()));
	}

	private Amount nullToZero(Amount amount) {
		if (amount == null) {
			amount = Amount.getZero(bookkeeping.getCurrency());
		}
		return amount;
	}

	private void addFootersToLedgerLines() throws ServiceException {
		for (Account account : ObjectFactory.create(ConfigurationService.class).findAllAccounts(document)) {
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
