/*
 * $Id: PartiesOverviewTableModel.java,v 1.9 2007-05-21 15:55:01 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import java.util.Date;

import javax.swing.table.AbstractTableModel;

import nl.gogognome.framework.models.DateModel;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;

import cf.engine.Database;
import cf.engine.Party;

/**
 * This class implements a model for a <code>JTable</code> that shows an overview 
 * of all parties at a specific date.
 *
 * The number of parties can be much bigger than the number of visible rows in the table.
 * Therefore the contents of the table are calculated when they are shown instead of
 * calculating all contents in the constructor.
 * 
 * @author Sander Kooijmans
 */
public class PartiesOverviewTableModel extends AbstractTableModel
{
    /** The date. */
    private Date date;

    /** The parties to be shown in the table. */
    private Party[] parties;
    
    /** The database from which the amounts of the parties are obtained. */
    private Database database;
    
    /**
     * Constructs a new <code>partyOverviewComponent</code>.
     * @param party the party to be shown
     * @param date the date
     */
    public PartiesOverviewTableModel(Database database, DateModel dateModel) {
        super();
        this.date = dateModel.getDate();
        this.database = database;
        parties = database.getParties();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() 
    {
        return 3;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() 
    {
        return parties.length + 1;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int row, int column) 
    {
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        String result;
        if (row < parties.length)
        {
	        switch(column)
	        {
	        case 0:
	            result = parties[row].getId() + " - " + parties[row].getName();
	            break;
	            
	        case 1:
	        {
	            Amount balance = database.getBalanceForParty(parties[row], date);
	            if (balance.isPositive())
	            {
	                result = af.formatAmount(balance);
	            }
	            else
	            {
	                result = "";
	            }
	            break;
	        }   
	        case 2:
	        {
	            Amount balance = database.getBalanceForParty(parties[row], date).negate();
	            if (balance.isPositive())
	            {
	                result = af.formatAmount(balance);
	            }
	            else
	            {
	                result = "";
	            }
	            break;
	        }   
	        default:
	            result = null;
	        }
        }
        else
        {
	        switch(column)
	        {
	        case 0:
	            result = TextResource.getInstance().getString("gen.total");
	            break;
	            
	        case 1:
	            Amount totalDebet = Amount.getZero(Database.getInstance().getCurrency());
	            for (int i = 0; i < parties.length; i++) 
	            {
		            Amount balance = database.getBalanceForParty(parties[i], date);
		            if (!balance.isNegative())
		            {
		                totalDebet = totalDebet.add(balance);
		            }
	            }
	            result = af.formatAmount(totalDebet);
	            break;
	            
	        case 2:
	            Amount totalCredit = Amount.getZero(Database.getInstance().getCurrency());
	            for (int i = 0; i < parties.length; i++) 
	            {
		            Amount balance = database.getBalanceForParty(parties[i], date);
		            if (balance.isNegative())
		            {
		                totalCredit = totalCredit.subtract(balance);
		            }
	            }
	            result = af.formatAmount(totalCredit);
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
            id = "gen.party";
            break;
            
        case 1:
            id = "gen.debet";
            break;
            
        case 2:
            id = "gen.credit";
            break;
            
        default:
            id = null;
        }
        return TextResource.getInstance().getString(id);
    }
}
