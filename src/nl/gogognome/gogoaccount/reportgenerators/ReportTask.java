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
package nl.gogognome.gogoaccount.reportgenerators;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.businessobjects.ReportType;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.OperationalResult;
import cf.engine.Party;

/**
 * This class generates reports in a text-format.
 *
 * A <i>report</i> consists of a balance, operational result, and a list
 * of debtors and creditors.
 *
 * @author Sander Kooijmans
 */
public class ReportTask implements Task {
    private Database database;
    private Date date;
    private Report report;

    private PrintWriter writer;

    private TextFormat textFormat;
    private File file;
    private ReportType fileType;

    private TaskProgressListener progressListener;

    private TextResource textResource = Factory.getInstance(TextResource.class);
    private AmountFormat amountFormat = Factory.getInstance(AmountFormat.class);

    /**
     * Constructor.
     * @param database
     * @param date
     * @param file
     * @param fileType the type of file to be created. Only PLAIN_TEXT is allowed
     */
    public ReportTask(Database database, Date date, File file, ReportType fileType) {
        this.database = database;
        this.date = date;
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
    	this.progressListener = progressListener;
    	progressListener.onProgressUpdate(0);
        try {
	        switch(fileType) {
	            case PLAIN_TEXT:
	                textFormat = new PlainTextFormat(Factory.getInstance(TextResource.class));
	                break;
	            default:
	                throw new IllegalArgumentException("Illegal file type: " + fileType);
	        }

	        report = BookkeepingService.createReport(database, date);
	        progressListener.onProgressUpdate(10);

	        writer = new PrintWriter(new FileWriter(file));
	        printReport();
	        writer.close();
        } catch (IOException e) {
            if (writer != null) {
                writer.close();
            }
            throw e;
        }

        progressListener.onProgressUpdate(100);
        return null;
    }

    /**
     * Writes a report to the specified writer.
     */
    private void printReport() {
        writer.println(textFormat.getStartOfDocument());
        progressListener.onProgressUpdate(20);
        printBalance();
        progressListener.onProgressUpdate(30);
        printOperationalResult(database.getOperationalResult(date));
        progressListener.onProgressUpdate(40);
        printDebtors(database.getDebtors(date), date);
        progressListener.onProgressUpdate(50);
        printCreditors(database.getCreditors(date), date);
        progressListener.onProgressUpdate(60);

        List<Journal> journals = database.getJournals();
        progressListener.onProgressUpdate(70);
        printJournals(journals, database.getStartOfPeriod(), date);
        progressListener.onProgressUpdate(80);
        printLedger(database.getAllAccounts(), journals, database.getStartOfPeriod(), date);

        writer.println(textFormat.getEndOfDocument());
    }

    private void printBalance() {
        Date date = report.getEndDate();
        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.balanceOf", new Object[] {
                textResource.formatDate("gen.dateFormatFull", date)
                }));
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

        List<Account> assets = report.getAssets();
        List<Account> liabilities = report.getLiabilities();

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
        values[1] = amountFormat.formatAmountWithoutCurrency(report.getTotalAssets());
        values[3] = total;
        values[4] = amountFormat.formatAmountWithoutCurrency(report.getTotalLiabilities());
        result.append(textFormat.getRow(values));

        result.append(textFormat.getEndOfTable());

        writer.println(result.toString());
    }

	private String formatAccount(Account account) {
		return account.getId() + ' ' + account.getName();
	}

	private String formatAmount(Account account) {
    	return amountFormat.formatAmountWithoutCurrency(report.getAmount(account));
	}

	private void printOperationalResult(OperationalResult operationalResult) {
        Date date = operationalResult.getDate();

        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.operationalResultOf", new Object[] {
                textResource.formatDate("gen.dateFormatFull", date)
                }));
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

        String total = textResource.getString("gen.total").toUpperCase();

       values[0] = total;
       values[1] = textFormat.formatAmount(operationalResult.getTotalExpenses());
       values[3] = total;
       values[4] = textFormat.formatAmount(operationalResult.getTotalRevenues());
       result.append(textFormat.getRow(values));

       result.append(textFormat.getEndOfTable());

        writer.println(result.toString());
    }

    private void printDebtors(Party[] debtors, Date date) {
        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.debtorsOf", new Object[] {
                textResource.formatDate("gen.dateFormatFull", date)
                }));
        result.append(textFormat.getNewLine());

        if (debtors.length == 0) {
            result.append(textResource.getString("rep.noDebtors"));
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

            values[0] = textResource.getString("gen.total").toUpperCase();
            values[1] = textFormat.formatAmount(total);

            result.append(textFormat.getRow(values));
            result.append(textFormat.getEndOfTable());
        }
        writer.println(result.toString());
    }

    private void printCreditors(Party[] creditors, Date date) {
        StringBuilder result = new StringBuilder(10000);

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.creditorsOf", new Object[] {
                textResource.formatDate("gen.dateFormatFull", date)
                }));
        result.append(textFormat.getNewLine());

        if (creditors.length == 0) {
            result.append(textResource.getString("rep.noCreditors"));
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

            values[0] = textResource.getString("gen.total").toUpperCase();
            values[1] = textFormat.formatAmount(total);

            result.append(textFormat.getRow(values));
            result.append(textFormat.getEndOfTable());
        }
        writer.println(result.toString());
    }

    private void printJournals(List<Journal> journals, Date startDate, Date endDate) {
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

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.journals"));
        result.append(textFormat.getNewLine());

        if (endIndex - startIndex <= 0) {
            result.append(textResource.getString("rep.noJournals"));
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

            for (int i=startIndex; i<endIndex; i++) {
                values[0] = textResource.formatDate("gen.dateFormat", journals.get(i).getDate());
                values[2] = journals.get(i).getId() + " - " + journals.get(i).getDescription();
                values[4] = "";
                values[6] = "";
                String idOfCreatedInvoice = journals.get(i).getIdOfCreatedInvoice();
                if (idOfCreatedInvoice != null) {
                    Invoice invoice = database.getInvoice(idOfCreatedInvoice);
                    values[8] = amountFormat.formatAmount(invoice.getAmountToBePaid())
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
                        amountFormat.formatAmountWithoutCurrency(items[j].getAmount());
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

        result.append(textFormat.getNewParagraph());

        result.append(textResource.getString("rep.ledger"));

        if (endIndex - startIndex <= 0) {
            result.append(textResource.getString("rep.noJournals"));
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

	            // Append the start balance
	            values[0] = "";
	            values[2] = textResource.getString("rep.startBalance");
	            Amount startAmount = BookkeepingService.getStartBalance(database, account);
                values[4] = "";
                values[6] = "";
                values[account.isDebet() ? 4 : 6] =
                    amountFormat.formatAmountWithoutCurrency(startAmount);
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
	                values[0] = textResource.formatDate("gen.dateFormat", journals.get(i).getDate());
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
			                    values[4] = amountFormat.formatAmountWithoutCurrency(amount);
			                    totalDebetMutations = totalDebetMutations.add(amount);
		                    } else {
			                    values[6] = amountFormat.formatAmountWithoutCurrency(amount);
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
	            values[2] = textResource.getString("rep.totalMutations");
                values[4] = amountFormat.formatAmountWithoutCurrency(totalDebetMutations);
                values[6] = amountFormat.formatAmountWithoutCurrency(totalCreditMutations);
                values[8] = "";
                result.append(textFormat.getRow(values));

                Amount endBalance = startAmount.add(totalDebetMutations).subtract(totalCreditMutations);
	            values[0] = "";
	            values[2] = textResource.getString("rep.endBalance");
                values[4] = "";
                values[6] = "";
                if (endBalance.isPositive()) {
                    values[4] = amountFormat.formatAmountWithoutCurrency(endBalance);
                    values[6] = "";
                } else {
                    values[4] = "";
                    values[6] = amountFormat.formatAmountWithoutCurrency(endBalance.negate());
                }
                result.append(textFormat.getRow(values));

                result.append(textFormat.getHorizontalSeparator());
            }
        }
        writer.println(result.toString());
    }

}
