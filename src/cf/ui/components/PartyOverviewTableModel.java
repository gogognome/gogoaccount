/*
 * $Id: PartyOverviewTableModel.java,v 1.8 2008-01-10 19:18:08 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import java.util.ArrayList;
import java.util.Date;

import javax.swing.table.AbstractTableModel;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;
import cf.engine.Invoice.Payment;

/**
 * This class implements a model for a <code>JTable</code> that shows an overview 
 * of a party at a specific date.
 *
 * @author Sander Kooijmans
 * 
 * TODO: Reimplement this class!
 */
public class PartyOverviewTableModel extends AbstractTableModel
{
    /** The party to be shown. */
    private Party party;
    
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
     * Constructs a new <code>partyOverviewComponent</code>.
     * @param party the party to be shown
     * @param date the date
     */
    public PartyOverviewTableModel(Party party, Date date) 
    {
        super();
        this.party = party;
        this.date = date;
        initializeValues();
    }

    private void initializeValues() {
        Database database = Database.getInstance(); 
        totalDebet = Amount.getZero(database.getCurrency());
        totalCredit = totalDebet;
        
        ArrayList<LineInfo> lineInfoVector = new ArrayList<LineInfo>();
        Invoice[] invoices = database.getInvoices();
        for (int i = 0; i < invoices.length; i++) {
            if (party.equals(invoices[i].getPayingParty())) {
                if (DateUtil.compareDayOfYear(invoices[i].getIssueDate(),date) <= 0) {
    	            Payment[] payments = invoices[i].getPayments();
    	            for (int j = 0; j < payments.length; j++) {
    	                if (DateUtil.compareDayOfYear(payments[j].date, date) <= 0) {
//    	                    LineInfo lineInfo = new LineInfo();
//    	                    lineInfo.item = items[j];
//    	                    lineInfo.journal = journals[i];
//    	                    lineInfoVector.addElement(lineInfo);
//    	                    if (lineInfo.item.isDebet())
//    	                    {
//    	                        totalDebet = totalDebet.add(lineInfo.item.getAmount());
//    	                    }
//    	                    else
//    	                    {
//    	                        totalCredit = totalCredit.add(lineInfo.item.getAmount());
//    	                    }
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
        return 5;
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
        TextResource tr = TextResource.getInstance();
        AmountFormat af = tr.getAmountFormat();
        String result;
        if (row < lineInfos.length)
        {
	        switch(column)
	        {
	        case 0:
	            result = tr.formatDate(
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
	            result = "";
	            break;
	            
	        case 2:
	            result = tr.getString("gen.total");
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
            
        default:
            id = null;
        }
        return TextResource.getInstance().getString(id);
    }
}
