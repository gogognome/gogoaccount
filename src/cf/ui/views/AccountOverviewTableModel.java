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

import nl.gogognome.lib.swing.AbstractListSortedTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;

/**
 * This class implements a model for a <code>JTable</code> that shows an overview
 * of an account at a specific date.
 *
 * @author Sander Kooijmans
 */
public class AccountOverviewTableModel extends AbstractListSortedTableModel<AccountOverviewTableModel.LineInfo> {

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

    /** The database used to obtain the invoices for journal items. */
    private Database database;

    /** The account to be shown. */
    private Account account;

    /** The date. */
    private Date date;

    /** This class contains the information to be shown in a single row of the table. */
    class LineInfo {
    	Date date;
    	String id;
        String description;
        Amount debet;
        Amount credit;
        String invoice;
    }

    private Amount totalDebet;

    private Amount totalCredit;

    /**
     * Constructs a new <code>AccountOverviewComponent</code>.
     * @param database the database
     * @param account the account to be shown
     * @param date the date
     */
    public AccountOverviewTableModel(Database database, Account account, Date date) {
        super(COLUMN_DEFINITIONS, Collections.<LineInfo>emptyList());
        this.database = database;
        this.account = account;
        this.date = date;
        initializeValues();
    }

    public void setAccountAndDate(Account account, Date date) {
        this.account = account;
        this.date = date;
        clear();
        initializeValues();
    }

    private void initializeValues() {
        if (account != null && date != null) {
	        totalDebet = Amount.getZero(database.getCurrency());
	        totalCredit = totalDebet;

	        List<Journal> journals = database.getJournals();
	        for (Journal journal : journals) {
	            if (DateUtil.compareDayOfYear(journal.getDate(), date) <= 0) {
	            	addLineInfoForJournal(journal);
	            }
	        }

	        addTotalAmount();
        }
    }

    private void addLineInfoForJournal(Journal journal) {
        JournalItem[] items = journal.getItems();
        for (int j = 0; j < items.length; j++) {
            if (items[j].getAccount().equals(account)) {
                LineInfo lineInfo = new LineInfo();
                lineInfo.date = journal.getDate();
                lineInfo.id = journal.getId();
                lineInfo.description = journal.getDescription();
                lineInfo.debet = items[j].isDebet() ? items[j].getAmount() : null;
                lineInfo.credit = items[j].isCredit() ? items[j].getAmount() : null;
                lineInfo.invoice = createInvoiceText(journal, items[j]);
                addRow(lineInfo);
                if (lineInfo.debet != null) {
                    totalDebet = totalDebet.add(lineInfo.debet);
                } else {
                    totalCredit = totalCredit.add(lineInfo.credit);
                }
            }
        }
	}

	private String createInvoiceText(Journal journal, JournalItem item) {
    	StringBuilder sb = new StringBuilder(100);
        Invoice invoice = database.getInvoice(item.getInvoiceId());
        Invoice createdInvoice = database.getInvoice(journal.getIdOfCreatedInvoice());
        if (invoice != null) {
            sb.append(invoice.getId()).append(" (").append(invoice.getPayingParty().getName()).append(")");
        }
        if (createdInvoice != null) {
        	sb.append("<").append(createdInvoice.getId());
        	sb.append(" (").append(createdInvoice.getPayingParty().getName()).append(")>");
        }
		return sb.toString();
	}

	private void addTotalAmount() {
        LineInfo lineInfo = new LineInfo();
        lineInfo.date = date;
        lineInfo.description = TextResource.getInstance().getString("gen.total");
        lineInfo.debet = totalDebet;
        lineInfo.credit = totalCredit;
        addRow(lineInfo);
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
        		return TextResource.getInstance().getAmountFormat().formatAmount(lineInfo.debet);
        	}
        } else if (CREDIT.equals(col)) {
        	if (lineInfo.credit != null) {
        		return TextResource.getInstance().getAmountFormat().formatAmount(lineInfo.credit);
        	}
        } else if (INVOICE.equals(col)) {
        	return lineInfo.invoice;
        }
        return null;
    }
}
