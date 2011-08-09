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
package cf.ui.views;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.businessobjects.Report.LedgerLine;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import cf.engine.Account;
import cf.engine.Invoice;

/**
 * This class implements a model for a <code>JTable</code> that shows an overview
 * of an account at a specific date.
 *
 * @author Sander Kooijmans
 */
public class AccountOverviewTableModel extends AbstractListTableModel<AccountOverviewTableModel.LineInfo> {

    private static final long serialVersionUID = 1L;

    private final static ColumnDefinition DATE =
        new ColumnDefinition("gen.date", Date.class, 75);

    private final static ColumnDefinition ID =
        new ColumnDefinition("gen.id", String.class, 100);

    private final static ColumnDefinition DESCRIPTION =
        new ColumnDefinition("gen.description", String.class, 200);

    private final static ColumnDefinition DEBET =
        new ColumnDefinition.Builder("gen.debet", String.class, 100)
    		.add(new RightAlignedRenderer()).build();

    private final static ColumnDefinition CREDIT =
        new ColumnDefinition.Builder("gen.credit", String.class, 100)
    		.add(new RightAlignedRenderer()).build();

    private final static ColumnDefinition INVOICE =
        new ColumnDefinition("gen.invoice", String.class, 200);

    private final static List<ColumnDefinition> COLUMN_DEFINITIONS = Arrays.asList(
        DATE, ID, DESCRIPTION, DEBET, CREDIT, INVOICE
    );

    private Report report;
    private Account account;

    /** This class contains the information to be shown in a single row of the table. */
    class LineInfo {
    	Date date;
    	String id;
        String description;
        Amount debet;
        Amount credit;
        String invoice;
    }

    /**
     * Constructs a new <code>AccountOverviewComponent</code>.
     * @param database the database
     * @param account the account to be shown
     * @param date the date
     */
    public AccountOverviewTableModel() {
        super(COLUMN_DEFINITIONS, Collections.<LineInfo>emptyList());
        initializeValues();
    }

    public void setAccountAndDate(Report report, Account account) {
    	this.report = report;
        this.account = account;
        clear();
        initializeValues();
    }

    private void initializeValues() {
        if (account != null && report != null) {
        	for (LedgerLine line : report.getLedgerLinesForAccount(account)) {
	            LineInfo lineInfo = new LineInfo();
	            lineInfo.date = line.date;
	            lineInfo.id = line.id;
	            lineInfo.description = line.description;
	            lineInfo.debet = line.debetAmount;
	            lineInfo.credit = line.creditAmount;;
	            lineInfo.invoice = createInvoiceText(line.invoice);
	            addRow(lineInfo);
	        }
        }
    }

	private String createInvoiceText(Invoice invoice) {
    	StringBuilder sb = new StringBuilder(100);
        if (invoice != null) {
            sb.append(invoice.getId());
            sb.append(" (").append(invoice.getPayingParty().getName()).append(")");
        }
		return sb.toString();
	}

    @Override
	public Object getValueAt(int row, int column) {
    	ColumnDefinition col = getColumnDefinition(column);
        LineInfo lineInfo = getRow(row);
        if (DATE.equals(col)) {
        	return lineInfo.date;
        } else if (ID.equals(col)) {
        	return lineInfo.id;
        } else if (ID.equals(col)) {
        	return lineInfo.id;
        } else if (DESCRIPTION.equals(col)) {
        	return lineInfo.description;
        } else if (DEBET.equals(col)) {
        	if (lineInfo.debet != null) {
        		return Factory.getInstance(AmountFormat.class).formatAmount(lineInfo.debet);
        	}
        } else if (CREDIT.equals(col)) {
        	if (lineInfo.credit != null) {
        		return Factory.getInstance(AmountFormat.class).formatAmount(lineInfo.credit);
        	}
        } else if (INVOICE.equals(col)) {
        	return lineInfo.invoice;
        }
        return null;
    }
}
