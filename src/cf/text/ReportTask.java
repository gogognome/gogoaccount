/*
 * $Id: ReportTask.java,v 1.1 2010-01-25 20:22:21 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.task.Task;
import nl.gogognome.task.TaskProgressListener;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;
import cf.engine.Account;
import cf.engine.Balance;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.OperationalResult;
import cf.engine.Party;
import cf.pdf.PdfLatex;

/**
 * This class generates reports in a text-format.
 *
 * A <i>report</i> consists of a balance, operational result, and a list
 * of debtors and creditors.
 *
 * @author Sander Kooijmans
 */
public class ReportTask implements Task {
    public final static int RP_TXT = 0;
    public final static int RP_PDF = 1;
    public final static int RP_HTML = 2;

    /** The locale used to format the report. */
    private Locale locale;

    /** The database used to create the report. */
    private Database database;

    /** The date of the report. */
    private Date date;

    /** The <code>Writer</code> to write the report to. */
    private PrintWriter writer;

    /** Thet <code>TextFormat</code> used to format the report. */
    private TextFormat textFormat;

    private String fileName;

    private int fileType;

    /** A report progress listener that monitors the progress of report creation. */
    private TaskProgressListener progressListener;

    /**
     * Constructor.
     * @param locale the locale used to format the report.
     */
    public ReportTask(Database database, Date date, String fileName, int fileType) {
        this.database = database;
        this.date = date;
        this.fileName = fileName;
        this.fileType = fileType;
        this.locale = TextResource.getInstance().getLocale();
    }

    /**
     * Writes a report to a file in the specified file type.
     *
     * @param fileName the name of the file to be generated
     * @param fileType the type of the file (<code>RP_HTML</code>,
     *         <code>RP_PDF</code> or <code>RP_TXT</code>)
     * @param progressListener the progress listener for this task
     * @throws IOException if an I/O exception occurs
     * @throws InterruptedException if the conversion from LaTeX to PDF fails
     */
    public Object execute(TaskProgressListener progressListener) throws Exception {
    	this.progressListener = progressListener;
    	progressListener.onProgressUpdate(0);
        try {
	        switch(fileType) {
	            case RP_TXT:
	                textFormat = new PlainTextFormat(TextResource.getInstance());
	                break;
	            case RP_PDF:
	                textFormat = new LatexTextFormat(TextResource.getInstance());
	                fileName = PdfLatex.getTexFileName(fileName);
	                break;
	            case RP_HTML:
	                textFormat = new HtmlTextFormat(TextResource.getInstance());
	                break;
	            default:
	                throw new IllegalArgumentException(
	                        "Illegal file type: " + fileType);
	        }
	        writer = new PrintWriter(new FileWriter(fileName));
	        printReport();
	        writer.close();
        } catch (IOException e) {
            if (writer != null) {
                writer.close();
            }
            throw e;
        }

        progressListener.onProgressUpdate(90);
        if (fileType == RP_PDF) {
            PdfLatex.convertTexToPdf(new File(fileName), (new File(fileName)).getParentFile());
        }
        progressListener.onProgressUpdate(100);
        return null;
    }

    /**
     * Writes a report to the specified writer.
     */
    private void printReport() {
        writer.println(textFormat.getStartOfDocument());
        progressListener.onProgressUpdate(10);
        printBalance(database.getBalance(date));
        progressListener.onProgressUpdate(20);
        printOperationalResult(database.getOperationalResult(date));
        progressListener.onProgressUpdate(30);
        printDebtors(database.getDebtors(date), date);
        progressListener.onProgressUpdate(40);
        printCreditors(database.getCreditors(date), date);
        progressListener.onProgressUpdate(50);

        List<Journal> journals = database.getJournals();
        progressListener.onProgressUpdate(65);
        printJournals(journals, database.getStartOfPeriod(), date);
        progressListener.onProgressUpdate(75);
        printLedger(database.getAllAccounts(), journals, database.getStartOfPeriod(), date);

        writer.println(textFormat.getEndOfDocument());
    }

    private void printBalance(Balance balance) {
        TextResource tr = TextResource.getInstance();
        Date date = balance.getDate();

        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(tr.getString("rep.balanceOf", new Object[] {
                tr.formatDate("gen.dateFormatFull", date)
                }));
        result.append(textFormat.getNewLine());

        result.append(textFormat.getStartOfTable(("lr|lr"),
                new int[] { 40, 15, 0, 40, 15 }));

        String[] values = new String[5];
        values[0] = tr.getString("gen.debet");
        values[1] = "";
        values[2] = null; // invisible column separator
        values[3] = "";
        values[4] = tr.getString("gen.credit");
        result.append(textFormat.getHeaderRow(values));

        values[2] = ""; // visible column separator

        result.append(textFormat.getHorizontalSeparator());

        Account[] assets = balance.getAssets();
        Account[] liabilities = balance.getLiabilities();

        int n = Math.max(assets.length, liabilities.length);
        for (int i=0; i<n; i++)
        {
            if (i < assets.length) {
                values[0] = assets[i].getId() + ' ' + assets[i].getName();
	            values[1] = textFormat.formatAmount(balance.getAmount(assets[i]));
            } else {
                values[0] = "";
                values[1] = "";
            }

            if (i < liabilities.length)
            {
                values[3] = liabilities[i].getId() + ' ' + liabilities[i].getName();
                values[4] = textFormat.formatAmount(balance.getAmount(liabilities[i]));
            }
            else
            {
                values[3] = "";
                values[4] = "";
            }
            result.append(textFormat.getRow(values));
        }

        result.append(textFormat.getEmptyRow());

        String total = TextResource.getInstance().getString("gen.total").toUpperCase();

        values[0] = total;
        values[1] = textFormat.formatAmount(balance.getTotalAssets());
        values[3] = total;
        values[4] = textFormat.formatAmount(balance.getTotalLiabilities());
        result.append(textFormat.getRow(values));

        result.append(textFormat.getEndOfTable());

        writer.println(result.toString());
    }

    private void printOperationalResult(OperationalResult operationalResult) {
        TextResource tr = TextResource.getInstance();
        Date date = operationalResult.getDate();

        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(tr.getString("rep.operationalResultOf", new Object[] {
                tr.formatDate("gen.dateFormatFull", date)
                }));
        result.append(textFormat.getNewLine());

        result.append(textFormat.getStartOfTable(("lr|lr"),
                new int[] { 40, 15, 0, 40, 15 }));

        String[] values = new String[5];
        values[0] = tr.getString("gen.expenses");
        values[1] = "";
        values[2] = null; // invisible column separator
        values[3] = "";
        values[4] = tr.getString("gen.revenues");
        result.append(textFormat.getHeaderRow(values));

        values[2] = ""; // visible column separator

        result.append(textFormat.getHorizontalSeparator());

        Account[] expenses = operationalResult.getExpenses();
        Account[] revenues = operationalResult.getRevenues();

        int n = Math.max(expenses.length, revenues.length);
        for (int i=0; i<n; i++)
        {
            if (i < expenses.length)
            {
                values[0] = expenses[i].getId() + ' ' + expenses[i].getName();
                values[1] = textFormat.formatAmount(
                    BookkeepingService.getAccountBalance(database, expenses[i], date));
            }
            else
            {
                values[0] = "";
                values[1] = "";
            }

            if (i < revenues.length)
            {
                values[3] = revenues[i].getId() + ' ' + revenues[i].getName();
                values[4] = textFormat.formatAmount(
                    BookkeepingService.getAccountBalance(database, revenues[i], date));
            }
            else
            {
                values[3] = "";
                values[4] = "";
            }
            result.append(textFormat.getRow(values));
        }

        result.append(textFormat.getEmptyRow());

        String total = TextResource.getInstance().getString("gen.total").toUpperCase();

       values[0] = total;
       values[1] = textFormat.formatAmount(operationalResult.getTotalExpenses());
       values[3] = total;
       values[4] = textFormat.formatAmount(operationalResult.getTotalRevenues());
       result.append(textFormat.getRow(values));

       result.append(textFormat.getEndOfTable());

        writer.println(result.toString());
    }

    private void printDebtors(Party[] debtors, Date date) {
        TextResource tr = TextResource.getInstance();

        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(tr.getString("rep.debtorsOf", new Object[] {
                tr.formatDate("gen.dateFormatFull", date)
                }));
        result.append(textFormat.getNewLine());

        if (debtors.length == 0) {
            result.append(tr.getString("rep.noDebtors"));
            result.append(textFormat.getNewLine());
        } else {
            Amount total = null;
            result.append(textFormat.getStartOfTable(("lr"),
                    new int[] { 40, 15 }));

            String[] values = new String[2];

            for (int i=0; i<debtors.length; i++) {
                values[0] = debtors[i].getId() + " - " + debtors[i].getName();
                Amount amount = database.getTotalDebetForParty(debtors[i], date);
                values[1] = textFormat.formatAmount(amount);
                total = total == null ? amount : total.add(amount);
                result.append(textFormat.getRow(values));
            }
            result.append(textFormat.getEmptyRow());

            values[0] = TextResource.getInstance().getString("gen.total").toUpperCase();
            values[1] = textFormat.formatAmount(total);

            result.append(textFormat.getRow(values));
            result.append(textFormat.getEndOfTable());
        }
        writer.println(result.toString());
    }

    private void printCreditors(Party[] creditors, Date date) {
        TextResource tr = TextResource.getInstance();

        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(tr.getString("rep.creditorsOf", new Object[] {
                tr.formatDate("gen.dateFormatFull", date)
                }));
        result.append(textFormat.getNewLine());

        if (creditors.length == 0) {
            result.append(tr.getString("rep.noCreditors"));
            result.append(textFormat.getNewLine());
        } else {
            Amount total = null;
            result.append(textFormat.getStartOfTable(("lr"),
                    new int[] { 40, 15 }));

            String[] values = new String[2];

            for (int i=0; i<creditors.length; i++) {
                values[0] = creditors[i].getId() + " - " + creditors[i].getName();
                Amount amount = database.getTotalCreditForParty(creditors[i], date);
                values[1] = textFormat.formatAmount(amount);
                total = total == null ? amount : total.add(amount);
                result.append(textFormat.getRow(values));
            }
            result.append(textFormat.getEmptyRow());

            values[0] = TextResource.getInstance().getString("gen.total").toUpperCase();
            values[1] = textFormat.formatAmount(total);

            result.append(textFormat.getRow(values));
            result.append(textFormat.getEndOfTable());
        }
        writer.println(result.toString());
    }

    private void printJournals(List<Journal> journals, Date startDate, Date endDate) {
        TextResource tr = TextResource.getInstance();

        int startIndex = 0;
        int endIndex = 0;
        for (int i=0; i<journals.size(); i++) {
            if (DateUtil.compareDayOfYear(journals.get(i).getDate(), endDate) <= 0) {
                endIndex = i+1;
            }
            if (DateUtil.compareDayOfYear(journals.get(i).getDate(), startDate) < 0) {
                startIndex = i+1;
            }
        }

        StringBuilder result = new StringBuilder(10000);
        AmountFormat af = new AmountFormat(locale);

        result.append(textFormat.getNewParagraph());

        result.append(tr.getString("rep.journals"));
        result.append(textFormat.getNewLine());

        if (endIndex - startIndex <= 0) {
            result.append(tr.getString("rep.noJournals"));
            result.append(textFormat.getNewLine());
        } else {
            result.append(textFormat.getStartOfTable(("l|l|r|r|l"),
                    new int[] { 10, 1, 45, 1, 10, 1, 10, 1, 40 }));

            String[] values = new String[9];
            values[0] = tr.getString("gen.date");
            values[1] = "";
            values[2] = tr.getString("gen.description");
            values[3] = "";
            values[4] = tr.getString("gen.debet");
            values[5] = "";
            values[6] = tr.getString("gen.credit");
            values[7] = "";
            values[8] = tr.getString("gen.invoice");
            result.append(textFormat.getHeaderRow(values));

            result.append(textFormat.getHorizontalSeparator());

            for (int i=startIndex; i<endIndex; i++) {
                values[0] = tr.formatDate("gen.dateFormat", journals.get(i).getDate());
                values[2] = journals.get(i).getId() + " - " + journals.get(i).getDescription();
                values[4] = "";
                values[6] = "";
                String idOfCreatedInvoice = journals.get(i).getIdOfCreatedInvoice();
                if (idOfCreatedInvoice != null) {
                    Invoice invoice = database.getInvoice(idOfCreatedInvoice);
                    values[8] = af.formatAmount(invoice.getAmountToBePaid())
                        + " " + invoice.getId() + " (" + invoice.getConcerningParty().getName() + ')';
                } else {
                    values[8] = "";
                }
                result.append(textFormat.getRow(values));

                JournalItem[] items = journals.get(i).getItems();
                for (int j = 0; j < items.length; j++) {
                    values[0] = "";
                    values[2] = items[j].getAccount().getId() + " - "
                    	+ items[j].getAccount().getName();
                    values[4] = "";
                    values[6] = "";
                    values[items[j].isDebet() ? 4 : 6] =
                        af.formatAmountWithoutCurrency(items[j].getAmount());
                    Invoice invoice = database.getInvoice(items[j].getInvoiceId());
                    values[8] = invoice != null ? invoice.getId() + " (" + invoice.getPayingParty().getName() + ")" : "";
                    result.append(textFormat.getRow(values));
                }

                result.append(textFormat.getHorizontalSeparator());
            }
        }
        writer.println(result.toString());
    }

    private void printLedger(Account[] accounts, List<Journal> journals, Date startDate,
            Date endDate) {
        TextResource tr = TextResource.getInstance();

        int startIndex = 0;
        int endIndex = 0;
        for (int i=0; i<journals.size(); i++) {
            if (DateUtil.compareDayOfYear(journals.get(i).getDate(), endDate) <= 0) {
                endIndex = i+1;
            }
            if (DateUtil.compareDayOfYear(journals.get(i).getDate(), startDate) < 0) {
                startIndex = i+1;
            }
        }

        StringBuilder result = new StringBuilder(10000);
        AmountFormat af = new AmountFormat(locale);

        result.append(textFormat.getNewParagraph());

        result.append(tr.getString("rep.ledger"));

        if (endIndex - startIndex <= 0) {
            result.append(tr.getString("rep.noJournals"));
            result.append(textFormat.getNewLine());
        } else {
            for (int accountNr=0; accountNr < accounts.length; accountNr++) {
                Account account = accounts[accountNr];
                result.append(textFormat.getNewParagraph());
                result.append(account.getId() + " " + account.getName());
                result.append(textFormat.getNewLine());
	            result.append(textFormat.getStartOfTable(("l|l|r|r|l"),
	                    new int[] { 10, 1, 45, 1, 10, 1, 10, 1, 40 }));

	            String[] values = new String[9];
	            values[0] = tr.getString("gen.date");
	            values[1] = "";
	            values[2] = tr.getString("gen.description");
	            values[3] = "";
	            values[4] = tr.getString("gen.debet");
	            values[5] = "";
	            values[6] = tr.getString("gen.credit");
	            values[7] = "";
	            values[8] = tr.getString("gen.invoice");
	            result.append(textFormat.getHeaderRow(values));

	            result.append(textFormat.getHorizontalSeparator());

	            // Append the start balance
	            values[0] = "";
	            values[2] = tr.getString("rep.startBalance");
	            Amount startAmount = BookkeepingService.getStartBalance(database, account);
                values[4] = "";
                values[6] = "";
                values[account.isDebet() ? 4 : 6] =
                    af.formatAmountWithoutCurrency(startAmount);
                values[8] = "";
                result.append(textFormat.getRow(values));

                // Convert the start amount to a debet value if it is a credit value.
                if (!account.isDebet()) {
                    startAmount = startAmount.negate();
                }

                // Append the items of 'account'.
                Amount totalDebetMutations = Amount.getZero(startAmount.getCurrency());
                Amount totalCreditMutations = Amount.getZero(startAmount.getCurrency());

	            for (int i=startIndex; i<endIndex; i++) {
	                values[0] = tr.formatDate("gen.dateFormat", journals.get(i).getDate());
	                values[2] = journals.get(i).getId() + " - " + journals.get(i).getDescription();
	                values[4] = "";
	                values[6] = "";
	                values[8] = "";

	                JournalItem[] items = journals.get(i).getItems();
	                for (int j = 0; j < items.length; j++) {
	                    if (account.equals(items[j].getAccount())) {
		                    values[4] = "";
		                    values[6] = "";
		                    Amount amount = items[j].getAmount();
		                    if (items[j].isDebet()) {
			                    values[4] = af.formatAmountWithoutCurrency(amount);
			                    totalDebetMutations = totalDebetMutations.add(amount);
		                    } else {
			                    values[6] = af.formatAmountWithoutCurrency(amount);
			                    totalCreditMutations = totalCreditMutations.add(amount);
		                    }
		                    Invoice invoice = database.getInvoice(items[j].getInvoiceId());
		                    values[8] = invoice != null ? invoice.getId() + " (" + invoice.getPayingParty().getName() + ")" : "";
		                    result.append(textFormat.getRow(values));
	                    }
	                }
	            }

	            // Append the mutations
	            values[0] = "";
	            values[2] = tr.getString("rep.totalMutations");
                values[4] = af.formatAmountWithoutCurrency(totalDebetMutations);
                values[6] = af.formatAmountWithoutCurrency(totalCreditMutations);
                values[8] = "";
                result.append(textFormat.getRow(values));

                Amount endBalance = startAmount.add(totalDebetMutations).subtract(totalCreditMutations);
	            values[0] = "";
	            values[2] = tr.getString("rep.endBalance");
                values[4] = "";
                values[6] = "";
                if (endBalance.isPositive()) {
                    values[4] = af.formatAmountWithoutCurrency(endBalance);
                    values[6] = "";
                } else {
                    values[4] = "";
                    values[6] = af.formatAmountWithoutCurrency(endBalance.negate());
                }
                result.append(textFormat.getRow(values));

                result.append(textFormat.getHorizontalSeparator());
            }
        }
        writer.println(result.toString());
    }

}
