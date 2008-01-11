/*
 * $Id: PartyOverviewTableModel.java,v 1.10 2008-01-11 18:56:56 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Party;
import cf.engine.Payment;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.table.AbstractTableModel;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;

/**
 * This class implements a model for a <code>JTable</code> that shows an overview 
 * of a party at a specific date.
 *
 * @author Sander Kooijmans
 */
public class PartyOverviewTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    /** The party to be shown. */
    private Party party;
    
    /** The date. */
    private Date date;

    /** This class contains the information to be shown in a single row of the table. */
    class LineInfo {
        Date date;
        String id;
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
                        lineInfo.id = invoices[i].getId();
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
	                if (DateUtil.compareDayOfYear(payments[j].getDate(), date) <= 0) {
	                    LineInfo lineInfo = new LineInfo();
                        lineInfo.id = invoices[i].getId();
	                    lineInfo.date = payments[j].getDate();
	                    lineInfo.description = payments[j].getDescription();
	                    if (!payments[j].getAmount().isNegative()) {
	                        lineInfo.creditAmount = payments[j].getAmount();
	                        totalCredit = totalCredit.add(lineInfo.creditAmount);
	                    } else {
                            lineInfo.debetAmount = payments[j].getAmount().negate();
                            totalDebet = totalDebet.add(lineInfo.debetAmount);
	                    }
                        lineInfoList.add(lineInfo);
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
        return 5;
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
                result = lineInfos[row].id;
                break;
	            
	        case 2:
	            result = lineInfos[row].description;
	            break;
	            
	        case 3:
                if (lineInfos[row].debetAmount != null)
                {
                    result = af.formatAmountWithoutCurrency(lineInfos[row].debetAmount);
                }
                else
                {
                    result = "";
                }
                break;
	            
	        case 4:
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
    
    public String getColumnName(int column) {
        String id = null;
        switch(column) {
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
