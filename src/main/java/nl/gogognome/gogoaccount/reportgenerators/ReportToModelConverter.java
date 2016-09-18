package nl.gogognome.gogoaccount.reportgenerators;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.businessobjects.Report.LedgerLine;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;

import java.util.*;

/**
 * Converts a Report to a model for ODT generation.
 */
public class ReportToModelConverter {

    private final Document document;
    private final AmountFormat amountFormat;
    private final TextResource textResource;
    private final ConfigurationService configurationService;
    private final PartyService partyService;

    private Report report;
    private Map<String, Object> model;

    public ReportToModelConverter(Document document, AmountFormat amountFormat, TextResource textResource,
                                  ConfigurationService configurationService, PartyService partyService) {
        this.document = document;
        this.amountFormat = amountFormat;
        this.textResource = textResource;
        this.configurationService = configurationService;
        this.partyService = partyService;
    }

    public Map<String, Object> buildModel(Report report) throws ServiceException {
        this.report = report;
        createModel();
        return model;
    }

    private void createModel() throws ServiceException {
        model = new HashMap<>();

        model.put("date", textResource.formatDate("gen.dateFormatFull", report.getEndDate()));
        model.put("balance", createBalanceLines());
        model.put("operationalResult", createOperationalResultLines());
        model.put("debtors", createDebtors());
        model.put("creditors", createCreditors());
        model.put("accounts", createAccounts());
    }

    private Object createBalanceLines() throws ServiceException {
        List<Map<String, Object>> lines = new ArrayList<>();
        addBalanceSheetLines(lines, report.getAssetsInclLossAccount(),
                report.getLiabilitiesInclProfitAccount());
        return lines;
    }

    private Object createOperationalResultLines() throws ServiceException {
        List<Map<String, Object>> lines = new ArrayList<>();
        addBalanceSheetLines(lines, report.getExpenses(), report.getRevenues());
        return lines;
    }

    private void addBalanceSheetLines(List<Map<String, Object>> lines,
            List<Account> leftAccounts, List<Account> rightAccounts) throws ServiceException {

        Amount leftTotal = new Amount("0");
        Amount rightTotal = new Amount("0");

        Iterator<Account> leftIter = leftAccounts.iterator();
        Iterator<Account> rightIter = rightAccounts.iterator();

        while (leftIter.hasNext() || rightIter.hasNext()) {
            Account leftAccount = leftIter.hasNext() ? leftIter.next() : null;
            String leftName = getAccountName(leftAccount);
            String leftAmount = formatAmount(leftAccount);
            leftTotal = leftTotal.add(report.getAmount(leftAccount));

            Account rightAccount = rightIter.hasNext()? rightIter.next() : null;
            String rightName = getAccountName(rightAccount);
            String rightAmount = formatAmount(rightAccount);
            rightTotal = rightTotal.add(report.getAmount(rightAccount));

            lines.add(createLine(leftName, leftAmount, rightName, rightAmount));
        }

        lines.add(createLine("", "", "", ""));
        String total = textResource.getString("gen.total");
        lines.add(createLine(total, amountFormat.formatAmountWithoutCurrency(leftTotal.toBigInteger()),
                total, amountFormat.formatAmountWithoutCurrency(rightTotal.toBigInteger())));
    }

    private String formatAmount(Account account) {
        return account != null ? getAmount(report.getAmount(account)) : "";
    }

    private String getAccountName(Account account) {
        StringBuilder sb = new StringBuilder(30);
        if (account != null) {
            sb.append(account.getId()).append(' ').append(account.getName());
        }
        return sb.toString();
    }

    private String getAmount(Amount amount) {
        StringBuilder sb = new StringBuilder();
        if (amount != null) {
            sb.append(amountFormat.formatAmountWithoutCurrency(amount.toBigInteger()));
        }
        return sb.toString();
    }

    private Map<String, Object> createLine(String name1,
            String amount1, String name2, String amount2) {
        Map<String,Object> line = new HashMap<>();
        line.put("name1", name1);
        line.put("amount1", amount1);
        line.put("name2", name2);
        line.put("amount2", amount2);
        return line;
    }

    private Object createDebtors() throws ServiceException {
        List<Map<String, Object>> lines = new ArrayList<>();
        Amount total = new Amount("0");
        for (Party p : report.getDebtors()) {
            Amount amount = report.getBalanceForDebtor(p);
            total = total.add(amount);
            lines.add(createLine(p, amount));
        }

        lines.add(createLine("", ""));
        lines.add(createLine(textResource.getString("gen.total"),
            amountFormat.formatAmountWithoutCurrency(total.toBigInteger())));
        return lines;
    }

    private Object createCreditors() throws ServiceException {
        List<Map<String, Object>> lines = new ArrayList<>();
        Amount total = new Amount("0");
        for (Party p : report.getCreditors()) {
            Amount amount = report.getBalanceForCreditor(p);
            total = total.add(amount);
            lines.add(createLine(p, amount));
        }

        lines.add(createLine("", ""));
        lines.add(createLine(textResource.getString("gen.total"),
            amountFormat.formatAmountWithoutCurrency(total.toBigInteger())));
        return lines;
    }

    private Map<String, Object> createLine(Party party, Amount amount) {
        return createLine(party.getId() + ' ' + party.getName(),
                amountFormat.formatAmountWithoutCurrency(amount.toBigInteger()));
    }

    private Map<String, Object> createLine(String partyName, String amount) {
        Map<String,Object> line = new HashMap<>();
        line.put("name", partyName);
        line.put("amount", amount);
        return line;
    }

    private Object createAccounts() throws ServiceException {
        List<Map<String, Object>> accounts = new ArrayList<>();
        for (Account account : configurationService.findAllAccounts(document)) {
            accounts.add(createAccount(account));
        }
        return accounts;
    }

    private Map<String, Object> createAccount(Account account) throws ServiceException {
        Map<String,Object> map = new HashMap<>();
        map.put("title", account.getId() + ' ' + account.getName());
        map.put("lines", createAccountLines(account));
        return map;
    }

    private Object createAccountLines(Account account) throws ServiceException {
        List<Map<String, Object>> lines = new ArrayList<>();
        for (LedgerLine line: report.getLedgerLinesForAccount(account)) {
            lines.add(createLine(line));
        }
        return lines;
    }

    private Map<String, Object> createLine(LedgerLine line) throws ServiceException {
        Map<String,Object> map = new HashMap<>();
        map.put("date", line.date != null ? textResource.formatDate("gen.dateFormat", line.date) : "");
        map.put("id", line.id);
        map.put("description", line.description);
        map.put("debet", line.debetAmount != null ?
                amountFormat.formatAmountWithoutCurrency(line.debetAmount.toBigInteger()) : "");
        map.put("credit", line.creditAmount != null ?
                amountFormat.formatAmountWithoutCurrency(line.creditAmount.toBigInteger()) : "");
        map.put("invoice", line.invoice != null ?
                line.invoice.getId() + " (" + partyService.getParty(document, line.invoice.getConcerningPartyId()).getName() + ')' : "");
        return map;
    }
}
