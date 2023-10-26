package nl.gogognome.gogoaccount.reportgenerators;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.businessobjects.Report.LedgerLine;
import nl.gogognome.gogoaccount.businessobjects.ReportType;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import org.slf4j.*;

/**
 * <p>This class generates reports in a text-format.</p>
 *
 * <p>A <i>report</i> consists of a balance, operational result, and a list
 * of debtors and creditors.</p>
 */
public class ReportTask implements Task {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReportTask.class);

    private final Document document;
    private final AmountFormat amountFormat;
    private final TextResource textResource;
    private final BookkeepingService bookkeepingService;
    private final ConfigurationService configurationService;
    private final InvoiceService invoiceService;
    private final LedgerService ledgerService;
    private final PartyService partyService;

    private Bookkeeping bookkeeping;
    private final Date date;
    private Report report;

    private PrintWriter writer;

    private TextFormat textFormat;
    private final File file;
    private final ReportType fileType;

    private TaskProgressListener progressListener;

    public ReportTask(Document document, AmountFormat amountFormat, TextResource textResource, BookkeepingService bookkeepingService,
                      ConfigurationService configurationService, InvoiceService invoiceService, LedgerService ledgerService,
                      PartyService partyService, Date endDate, File file, ReportType fileType) {
        this.document = document;
        this.amountFormat = amountFormat;
        this.textResource = textResource;
        this.bookkeepingService = bookkeepingService;
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.ledgerService = ledgerService;
        this.partyService = partyService;
        this.date = endDate;
        this.file = file;
        this.fileType = fileType;
    }

    /**
     * Writes a report to a file in the specified file type.
     *
     * @param progressListener the progress listener for this task
     */
    @Override
	public Object execute(TaskProgressListener progressListener) throws Exception {
        bookkeeping = configurationService.getBookkeeping(document);
    	this.progressListener = progressListener;
    	progressListener.onProgressUpdate(0);
        if (Objects.requireNonNull(fileType) == ReportType.PLAIN_TEXT) {
            textFormat = new PlainTextFormat(Factory.getInstance(TextResource.class));
        } else {
            throw new IllegalArgumentException("Illegal file type: " + fileType);
        }

        report = bookkeepingService.createReport(document, date);
        progressListener.onProgressUpdate(10);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))){
            this.writer = writer;
            printReport();
        }

        progressListener.onProgressUpdate(100);
        LOGGER.info("Created report at " + file.getAbsolutePath());
        return null;
    }

    /**
     * Writes a report to the specified writer.
     */
    private void printReport() throws ServiceException {
        writer.println(textFormat.getStartOfDocument());
        progressListener.onProgressUpdate(20);
        printBalance();
        progressListener.onProgressUpdate(30);
        printOperationalResult();
        progressListener.onProgressUpdate(40);
        printDebtors();
        progressListener.onProgressUpdate(50);
        printCreditors();
        progressListener.onProgressUpdate(60);

        List<JournalEntry> journalEntries = ledgerService.findJournalEntries(document);
        progressListener.onProgressUpdate(70);
        printJournals(journalEntries, bookkeeping.getStartOfPeriod(), date);
        progressListener.onProgressUpdate(80);
        printLedger();

        writer.println(textFormat.getEndOfDocument());
    }

    private void printBalance() {
        Date date = report.getEndDate();
        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.balanceOf", textResource.formatDate("gen.dateFormatFull", date)));
        result.append(textFormat.getNewLine());

        result.append(textFormat.getStartOfTable(("lr|lr"),
                new int[] { 40, 15, 0, 40, 15 }));

        String[] values = new String[5];
        values[0] = textResource.getString("gen.debet");
        values[1] = "";
        values[2] = null; // invisible column separator
        values[3] = "";
        values[4] = textResource.getString("gen.credit");
        result.append(textFormat.getHeaderRow(values));

        values[2] = ""; // visible column separator

        result.append(textFormat.getHorizontalSeparator());

        List<Account> assets = report.getAssetsInclLossAccount();
        List<Account> liabilities = report.getLiabilitiesInclProfitAccount();

        int n = Math.max(assets.size(), liabilities.size());
        for (int i=0; i<n; i++)
        {
            if (i < assets.size()) {
                values[0] = formatAccount(assets.get(i));
	            values[1] = formatAmount(assets.get(i));
            } else {
                values[0] = "";
                values[1] = "";
            }

            if (i < liabilities.size())
            {
                values[3] = formatAccount(liabilities.get(i));
                values[4] = formatAmount(liabilities.get(i));
            }
            else
            {
                values[3] = "";
                values[4] = "";
            }
            result.append(textFormat.getRow(values));
        }

        result.append(textFormat.getEmptyRow());

        String total = textResource.getString("gen.total").toUpperCase();

        values[0] = total;
        values[1] = amountFormat.formatAmountWithoutCurrency(report.getTotalAssets().toBigInteger());
        values[3] = total;
        values[4] = amountFormat.formatAmountWithoutCurrency(report.getTotalLiabilities().toBigInteger());
        result.append(textFormat.getRow(values));

        result.append(textFormat.getEndOfTable());

        writer.println(result);
    }

	private String formatAccount(Account account) {
		return account.getId() + ' ' + account.getName();
	}

	private String formatAmount(Account account) {
    	return amountFormat.formatAmountWithoutCurrency(report.getAmount(account).toBigInteger());
	}

	private void printOperationalResult() {
        Date date = report.getEndDate();

        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.operationalResultOf", textResource.formatDate("gen.dateFormatFull", date)));
        result.append(textFormat.getNewLine());

        result.append(textFormat.getStartOfTable(("lr|lr"),
                new int[] { 40, 15, 0, 40, 15 }));

        String[] values = new String[5];
        values[0] = textResource.getString("gen.expenses");
        values[1] = "";
        values[2] = null; // invisible column separator
        values[3] = "";
        values[4] = textResource.getString("gen.revenues");
        result.append(textFormat.getHeaderRow(values));

        values[2] = ""; // visible column separator

        result.append(textFormat.getHorizontalSeparator());

        List<Account> expenses = report.getExpenses();
        List<Account> revenues = report.getRevenues();

        int n = Math.max(expenses.size(), revenues.size());
        for (int i=0; i<n; i++) {
            if (i < expenses.size()) {
                values[0] = formatAccount(expenses.get(i));
                values[1] = formatAmount(expenses.get(i));
            } else {
                values[0] = "";
                values[1] = "";
            }

            if (i < revenues.size()) {
                values[3] = formatAccount(revenues.get(i));
                values[4] = formatAmount(revenues.get(i));
            } else {
                values[3] = "";
                values[4] = "";
            }
            result.append(textFormat.getRow(values));
        }

        result.append(textFormat.getEmptyRow());

        String total = textResource.getString("gen.total").toUpperCase();

       values[0] = total;
       values[1] = amountFormat.formatAmountWithoutCurrency(report.getTotalExpenses().toBigInteger());
       values[3] = total;
       values[4] = amountFormat.formatAmountWithoutCurrency(report.getTotalRevenues().toBigInteger());
       result.append(textFormat.getRow(values));

       result.append(textFormat.getEndOfTable());

        writer.println(result);
    }

    private void printDebtors() {
        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.debtorsOf", textResource.formatDate("gen.dateFormatFull", date)));
        result.append(textFormat.getNewLine());

        List<Party> debtors = report.getDebtors();
        if (debtors.isEmpty()) {
            result.append(textResource.getString("rep.noDebtors"));
            result.append(textFormat.getNewLine());
        } else {
            Amount total = Amount.ZERO;
            result.append(textFormat.getStartOfTable(("lr"),
                    new int[] { 40, 15 }));

            String[] values = new String[2];

            for (Party debtor : debtors) {
                values[0] = formatParty(debtor);
                Amount amount = report.getBalanceForDebtor(debtor);
                values[1] = amountFormat.formatAmountWithoutCurrency(amount.toBigInteger());
                total = total.add(amount);
                result.append(textFormat.getRow(values));
            }
            result.append(textFormat.getEmptyRow());

            values[0] = textResource.getString("gen.total").toUpperCase();
            values[1] = textFormat.formatAmount(total);

            result.append(textFormat.getRow(values));
            result.append(textFormat.getEndOfTable());
        }
        writer.println(result);
    }

    private void printCreditors() {
        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.creditorsOf", textResource.formatDate("gen.dateFormatFull", date)));
        result.append(textFormat.getNewLine());

        List<Party> creditors = report.getCreditors();
        if (creditors.isEmpty()) {
            result.append(textResource.getString("rep.noCreditors"));
            result.append(textFormat.getNewLine());
        } else {
            Amount total = Amount.ZERO;
            result.append(textFormat.getStartOfTable(("lr"),
                    new int[] { 40, 15 }));

            String[] values = new String[2];

            for (Party creditor : creditors) {
                values[0] = formatParty(creditor);
                Amount amount = report.getBalanceForCreditor(creditor);
                values[1] = amountFormat.formatAmountWithoutCurrency(amount.toBigInteger());
                total = total.add(amount);
                result.append(textFormat.getRow(values));
            }
            result.append(textFormat.getEmptyRow());

            values[0] = textResource.getString("gen.total").toUpperCase();
            values[1] = textFormat.formatAmount(total);

            result.append(textFormat.getRow(values));
            result.append(textFormat.getEndOfTable());
        }
        writer.println(result);
    }

    private String formatParty(Party party) {
		return party.getId() + " - " + party.getName();
	}

	private void printJournals(List<JournalEntry> journalEntries, Date startDate, Date endDate) throws ServiceException {
        int startIndex = 0;
        int endIndex = 0;
        for (int i=0; i< journalEntries.size(); i++) {
            if (DateUtil.compareDayOfYear(journalEntries.get(i).getDate(), endDate) <= 0) {
                endIndex = i+1;
            }
            if (DateUtil.compareDayOfYear(journalEntries.get(i).getDate(), startDate) < 0) {
                startIndex = i+1;
            }
        }

        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.journalEntries"));
        result.append(textFormat.getNewLine());

        if (endIndex - startIndex <= 0) {
            result.append(textResource.getString("rep.noJournalEntries"));
            result.append(textFormat.getNewLine());
        } else {
            result.append(textFormat.getStartOfTable(("l|l|r|r|l"),
                    new int[] { 10, 1, 45, 1, 10, 1, 10, 1, 40 }));

            String[] values = new String[9];
            values[0] = textResource.getString("gen.date");
            values[1] = "";
            values[2] = textResource.getString("gen.description");
            values[3] = "";
            values[4] = textResource.getString("gen.debet");
            values[5] = "";
            values[6] = textResource.getString("gen.credit");
            values[7] = "";
            values[8] = textResource.getString("gen.invoice");
            result.append(textFormat.getHeaderRow(values));

            result.append(textFormat.getHorizontalSeparator());

            Map<String, Account> idToAccount = configurationService.findAllAccounts(document).stream().collect(toMap(Account::getId, a -> a));

            for (int i=startIndex; i<endIndex; i++) {
                values[0] = textResource.formatDate("gen.dateFormat", journalEntries.get(i).getDate());
                values[2] = journalEntries.get(i).getId() + " - " + journalEntries.get(i).getDescription();
                values[4] = "";
                values[6] = "";
                String idOfCreatedInvoice = journalEntries.get(i).getIdOfCreatedInvoice();
                if (idOfCreatedInvoice != null) {
                    Invoice invoice = invoiceService.getInvoice(document, idOfCreatedInvoice);
                    values[8] = amountFormat.formatAmountWithoutCurrency(invoice.getAmountToBePaid().toBigInteger())
                        + " " + invoice.getId() + " (" + partyService.getParty(document, invoice.getPartyId()).getName() + ')';
                } else {
                    values[8] = "";
                }
                result.append(textFormat.getRow(values));

                List<JournalEntryDetail> journalEntryDetails = ledgerService.findJournalEntryDetails(document, journalEntries.get(i));
                for (JournalEntryDetail item : journalEntryDetails) {
                    values[0] = "";
                    values[2] = item.getAccountId() + " - " + idToAccount.get(item.getAccountId()).getName();
                    values[4] = "";
                    values[6] = "";
                    values[item.isDebet() ? 4 : 6] =
                            amountFormat.formatAmountWithoutCurrency(item.getAmount().toBigInteger());
                    if (item.getInvoiceId() != null) {
                        Invoice invoice = invoiceService.getInvoice(document, item.getInvoiceId());
                        values[8] = invoice.getId() + " (" + partyService.getParty(document, invoice.getPartyId()).getName() + ")";
                    } else {
                        values[8] = "";
                    }
                    result.append(textFormat.getRow(values));
                }

                result.append(textFormat.getHorizontalSeparator());
            }
        }
        writer.println(result);
    }

    private void printLedger() throws ServiceException {
        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.ledger"));

        for (Account account : report.getAllAccounts()) {
            result.append(textFormat.getNewParagraph());
            result.append(formatAccount(account));
            result.append(textFormat.getNewLine());
            result.append(textFormat.getStartOfTable(("l|l|r|r|l"),
                    new int[] { 10, 1, 45, 1, 10, 1, 10, 1, 40 }));

            String[] values = new String[9];
            values[0] = textResource.getString("gen.date");
            values[1] = "";
            values[2] = textResource.getString("gen.description");
            values[3] = "";
            values[4] = textResource.getString("gen.debet");
            values[5] = "";
            values[6] = textResource.getString("gen.credit");
            values[7] = "";
            values[8] = textResource.getString("gen.invoice");
            result.append(textFormat.getHeaderRow(values));

            result.append(textFormat.getHorizontalSeparator());

            // Append the items of 'account'.
            for (LedgerLine line : report.getLedgerLinesForAccount(account)) {
                values[0] = textResource.formatDate("gen.dateFormat", line.date);
                values[2] = line.description;
                values[4] = amountFormat.formatAmountWithoutCurrency(line.debetAmount != null ? line.debetAmount.toBigInteger() : null);
                values[6] = amountFormat.formatAmountWithoutCurrency(line.creditAmount != null ? line.creditAmount.toBigInteger() : null);
                values[8] = "";

                Invoice invoice = line.invoice;
                values[8] = invoice != null ? invoice.getId() + " (" + partyService.getParty(document, invoice.getPartyId()).getName() + ")" : "";

                result.append(textFormat.getRow(values));
            }

            result.append(textFormat.getHorizontalSeparator());
        }
        writer.println(result);
    }

}
