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
package cf.ui.components;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;
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
public class AccountOverviewTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    /** The database used to obtain the invoices for journal items. */
    private Database database;

    /** The account to be shown. */
    private Account account;

    /** The date. */
    private Date date;

    /** This class contains the information to be shown in a single row of the table. */
    class LineInfo
    {
        JournalItem item;
        Journal journal;
    }

    /** The information shown in the table. */
    LineInfo[] lineInfos;

    private Amount totalDebet;

    private Amount totalCredit;

    /**
     * Constructs a new <code>AccountOverviewComponent</code>.
     * @param database the database
     * @param account the account to be shown
     * @param date the date
     */
    public AccountOverviewTableModel(Database database, Account account, Date date) {
        super();
        this.database = database;
        this.account = account;
        this.date = date;
        initializeValues();
    }

    private void initializeValues()
    {
        Database database = Database.getInstance();
        totalDebet = Amount.getZero(database.getCurrency());
        totalCredit = totalDebet;

        ArrayList<LineInfo> lineInfoVector = new ArrayList<LineInfo>();
        List<Journal> journals = database.getJournals();
        for (Journal journal : journals) {
            if (DateUtil.compareDayOfYear(journal.getDate(), date) <= 0) {
	            JournalItem[] items = journal.getItems();
	            for (int j = 0; j < items.length; j++) {
	                if (items[j].getAccount().equals(account)) {
	                    LineInfo lineInfo = new LineInfo();
	                    lineInfo.item = items[j];
	                    lineInfo.journal = journal;
	                    lineInfoVector.add(lineInfo);
	                    if (lineInfo.item.isDebet())
	                    {
	                        totalDebet = totalDebet.add(lineInfo.item.getAmount());
	                    }
	                    else
	                    {
	                        totalCredit = totalCredit.add(lineInfo.item.getAmount());
	                    }
	                }
	            }
            }
        }

        lineInfos = new LineInfo[lineInfoVector.size()];
        lineInfoVector.toArray(lineInfos);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount()
    {
        return 6;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount()
    {
        return lineInfos.length + 1;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int row, int column)
    {
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        String result;
        if (row < lineInfos.length)
        {
	        switch(column)
	        {
	        case 0:
	            result = TextResource.getInstance().formatDate(
	                    "gen.dateFormat", lineInfos[row].journal.getDate());
	            break;

	        case 1:
	            result = lineInfos[row].journal.getId();
	            break;

	        case 2:
	            result = lineInfos[row].journal.getDescription();
	            break;

	        case 3:
	            if (lineInfos[row].item.isDebet())
	            {
	                result = af.formatAmountWithoutCurrency(lineInfos[row].item.getAmount());
	            }
	            else
	            {
	                result = "";
	            }
	            break;

	        case 4:
	            if (lineInfos[row].item.isCredit())
	            {
	                result = af.formatAmountWithoutCurrency(lineInfos[row].item.getAmount());
	            }
	            else
	            {
	                result = "";
	            }
	            break;

	        case 5:
	            Invoice invoice = database.getInvoice(lineInfos[row].item.getInvoiceId());
	            Invoice createdInvoice = database.getInvoice(lineInfos[row].journal.getIdOfCreatedInvoice());
	            if (invoice != null) {
	                result = invoice.getId() + " (" + invoice.getPayingParty().getName() + ")";
	            } else {
	                result = "";
	            }
	            if (createdInvoice != null) {
	            	result += "<" + createdInvoice.getId() + " (" + createdInvoice.getPayingParty().getName() + ")>";
	            }
	            break;

	        default:
	            result = null;
	        }
        }
        else
        {
	        switch(column)
	        {
	        case 0:
	        case 1:
	        case 5:
	            result = "";
	            break;

	        case 2:
	            result = TextResource.getInstance().getString("gen.total");
	            break;

	        case 3:
                result = af.formatAmountWithoutCurrency(totalDebet);
	            break;

	        case 4:
                result = af.formatAmountWithoutCurrency(totalCredit);
	            break;

	        default:
	            result = null;
	        }
        }
        return result;
    }

    @Override
	public String getColumnName(int column)
    {
        String id = null;
        switch(column)
        {
        case 0:
            id = "gen.date";
            break;

        case 1:
            id = "gen.id";
            break;

        case 2:
            id = "gen.description";
            break;

        case 3:
            id = "gen.debet";
            break;

        case 4:
            id = "gen.credit";
            break;

        case 5:
            id = "gen.party";
            break;

        default:
            id = null;
        }
        return TextResource.getInstance().getString(id);
    }
}
