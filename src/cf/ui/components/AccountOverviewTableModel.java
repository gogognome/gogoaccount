/*
 * $Id: AccountOverviewTableModel.java,v 1.9 2007-03-04 21:04:36 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import java.util.Date;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;

import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;

/**
 * This class implements a model for a <code>JTable</code> that shows an overview 
 * of an account at a specific date.
 *
 * @author Sander Kooijmans
 */
public class AccountOverviewTableModel extends AbstractTableModel
{
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
     * @param account the account to be shown
     * @param date the date
     */
    public AccountOverviewTableModel(Account account, Date date) 
    {
        super();
        this.account = account;
        this.date = date;
        initializeValues();
    }

    private void initializeValues()
    {
        Database database = Database.getInstance(); 
        totalDebet = Amount.getZero(database.getCurrency());
        totalCredit = totalDebet;
        
        Vector lineInfoVector = new Vector();
        Journal[] journals = database.getJournals();
        for (int i = 0; i < journals.length; i++) 
        {
            if (DateUtil.compareDayOfYear(journals[i].getDate(), date) <= 0)
            {
	            JournalItem[] items = journals[i].getItems();
	            for (int j = 0; j < items.length; j++) 
	            {
	                if (items[j].getAccount().equals(account))
	                {
	                    LineInfo lineInfo = new LineInfo();
	                    lineInfo.item = items[j];
	                    lineInfo.journal = journals[i];
	                    lineInfoVector.addElement(lineInfo);
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
        lineInfoVector.copyInto(lineInfos);
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
	            Party party = lineInfos[row].item.getParty();
	            if (party != null) {
	                result = party.getId() + " " + party.getName();
	            } else {
	                result = null;
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
