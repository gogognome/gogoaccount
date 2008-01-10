/*
 * $Id: PartyOverviewTableModel.java,v 1.9 2008-01-10 21:18:13 sanderk Exp $
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
    class LineInfo {
        Date date;
        String description;
        Amount debetAmount;
        Amount creditAmount;
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
        
        ArrayList<LineInfo> lineInfoList = new ArrayList<LineInfo>();
        Invoice[] invoices = database.getInvoices();
        for (int i = 0; i < invoices.length; i++) {
            if (party.equals(invoices[i].getPayingParty())) {
                if (DateUtil.compareDayOfYear(invoices[i].getIssueDate(),date) <= 0) {
                    String[] descriptions = invoices[i].getDescriptions();
                    Amount[] amounts = invoices[i].getAmounts();
                    for (int l=0; l<descriptions.length; l++) {
                        LineInfo lineInfo = new LineInfo();
                        lineInfo.date = invoices[i].getIssueDate();
                        lineInfo.description = descriptions[l];
                        if (amounts[l] != null) {
                            if (!amounts[l].isNegative()) {
                                lineInfo.debetAmount = amounts[l];
                                totalDebet = totalDebet.add(lineInfo.debetAmount);
                            } else {
                                lineInfo.creditAmount = amounts[l].negate();
                                totalCredit = totalCredit.add(lineInfo.creditAmount);
                            }
                        }
                        lineInfoList.add(lineInfo);
                    }
                }
	            Payment[] payments = invoices[i].getPayments();
	            for (int j = 0; j < payments.length; j++) {
	                if (DateUtil.compareDayOfYear(payments[j].date, date) <= 0) {
	                    LineInfo lineInfo = new LineInfo();
	                    lineInfo.date = payments[j].date;
	                    lineInfo.description = payments[j].description;
	                    if (!payments[j].amount.isNegative()) {
	                        lineInfo.creditAmount = payments[j].amount;
	                        totalCredit = totalCredit.add(lineInfo.creditAmount);
	                    } else {
                            lineInfo.debetAmount = payments[j].amount.negate();
                            totalDebet = totalDebet.add(lineInfo.debetAmount);
	                    }
	                }
	            }
            }
        }
        
        lineInfos = new LineInfo[lineInfoList.size()];
        lineInfoList.toArray(lineInfos);
    }
    
    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() {
        return 4;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() {
        return lineInfos.length + 1;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int row, int column) {
        TextResource tr = TextResource.getInstance();
        AmountFormat af = tr.getAmountFormat();
        String result;
        if (row < lineInfos.length) {
	        switch(column)
	        {
	        case 0:
	            result = tr.formatDate(
	                    "gen.dateFormat", lineInfos[row].date);
	            break;
	            
	        case 1:
	            result = lineInfos[row].description;
	            break;
	            
	        case 2:
                if (lineInfos[row].debetAmount != null)
                {
                    result = af.formatAmountWithoutCurrency(lineInfos[row].debetAmount);
                }
                else
                {
                    result = "";
                }
                break;
	            
	        case 3:
                if (lineInfos[row].creditAmount != null)
                {
                    result = af.formatAmountWithoutCurrency(lineInfos[row].creditAmount);
                }
                else
                {
                    result = "";
                }
                break;
	            
	        default:
	            result = null;
	        }
        } else {
	        switch(column)
	        {
	        case 0:
	            result = "";
	            break;
	            
	        case 1:
	            result = tr.getString("gen.total");
	            break;
	            
	        case 2:
                result = af.formatAmountWithoutCurrency(totalDebet);
	            break;
	
	        case 3:
                result = af.formatAmountWithoutCurrency(totalCredit);
	            break;
	            
	        default:
	            result = null;
	        }
        }
        return result;
    }
    
    public String getColumnName(int column) {
        String id = null;
        switch(column) {
        case 0:
            id = "gen.date";
            break;
            
        case 1:
            id = "gen.description";
            break;
            
        case 2:
            id = "gen.debet";
            break;

        case 3:
            id = "gen.credit";
            break;
            
        default:
            id = null;
        }
        return TextResource.getInstance().getString(id);
    }
}
