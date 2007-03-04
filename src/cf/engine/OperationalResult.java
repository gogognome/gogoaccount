/*
 * $Id: OperationalResult.java,v 1.8 2007-03-04 21:04:24 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.Date;
import java.util.Locale;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;

/**
 * This class represents an operational result.
 *
 * @author Sander Kooijmans
 */
public class OperationalResult {

    private Date date;
    
    private Amount totalExpenses;
    
    private Amount totalRevenues;
    
    /**
     * 
     */
    public OperationalResult(Date date) 
    {
        this.date = date;
        totalExpenses = Amount.getZero(Database.getInstance().getCurrency());
        totalRevenues = totalExpenses;
        
        Account[] expenses = getExpenses();
        
        for (int i = 0; i < expenses.length; i++) 
        {
            totalExpenses = totalExpenses.add(expenses[i].getBalance(date));
        }
        
        Account[] revenues = getRevenues();
        for (int i = 0; i < revenues.length; i++) 
        {
            totalRevenues = totalRevenues.add(revenues[i].getBalance(date));
        }
    }

    public Date getDate()
    {
        return date;
    }
    
    public Amount getTotalExpenses()
    {
        return totalExpenses;
    }
    
    public Amount getTotalRevenues()
    {
        return totalRevenues;
    }
    
    /**
     * Gets the result of operations.
     * @return the result of operations (positive indicates profit, negative
     *          indicates loss)
     */
    public Amount getResultOfOperations()
    {
        return totalRevenues.subtract(totalExpenses);
    }
    
    public Account[] getExpenses()
    {
        return Database.getInstance().getExpenses();
    }
    
    public Account[] getRevenues()
    {
        return Database.getInstance().getRevenues();
    }
    
    /**
     * Gets a string representation of the operational result.
     * @return a string representation of the operational result
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        AmountFormat af = new AmountFormat(Locale.getDefault());
        
        result.append("operation result of ");
        result.append(date);
        result.append('\n');
        int columnWidth = 45;
        
        StringBuffer sb = null;
        Account[] expenses = getExpenses();
        Account[] revenues = getRevenues();
        
        int n = Math.max(expenses.length, revenues.length);
        int index = 0;
        for (int i=0; i<n; i++)
        {
            sb = new StringBuffer();
            if (i < expenses.length)
            {
	            sb.append(expenses[i].getId());
	            sb.append(' ');
	            sb.append(expenses[i].getName());
	            index = sb.length();
	            sb.append(af.formatAmount(expenses[i].getBalance(date)));
            }
            else
            {
                index = 0;
            }
            while (sb.length() < columnWidth)
            {
                sb.insert(index, ' ');
            }
            result.append(sb);
            result.append(" | ");
            
            sb = new StringBuffer();
            
            if (i < revenues.length)
            {
	            sb.append(revenues[i].getId());
	            sb.append(' ');
	            sb.append(revenues[i].getName());
	            index = sb.length();
	            sb.append(af.formatAmount(revenues[i].getBalance(date)));
            }
            else
            {
                index = 0;
            }
            while (sb.length() < columnWidth)
            {
                sb.insert(index, ' ');
            }
            result.append(sb);
            result.append('\n');
        }

        result.append('\n');
        sb = new StringBuffer();
        
        sb.append(af.formatAmount(getTotalExpenses()));
        while (sb.length() < columnWidth)
        {
            sb.insert(0, ' ');
        }
        result.append(sb);
        result.append(" | ");
        
        sb = new StringBuffer();
        sb.append(af.formatAmount(getTotalRevenues()));
        while (sb.length() < columnWidth)
        {
            sb.insert(0, ' ');
        }
        result.append(sb);
        result.append('\n');
        
        return result.toString();
    }
}
