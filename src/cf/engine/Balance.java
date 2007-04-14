/*
 * $Id: Balance.java,v 1.13 2007-04-14 12:47:18 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.Date;
import java.util.Locale;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;

/**
 * This class represents a balance.
 *
 * @author Sander Kooijmans
 */
public class Balance 
{
    private Date date;
    
    private Amount totalAssets;
    
    private Amount totalLiabilities;
    
    private Amount resultOfOperations;

    private Account[] assets;
    
    private Account[] liabilities;
    
    /**
     * Constructor.
     * @param database the database on which this balance is based.
     * @param date the date of the balance.
     */
    protected Balance(Database database, Date date) 
    {
        this.date = date;
        totalAssets = Amount.getZero(database.getCurrency());
        totalLiabilities = totalAssets; // is zero
        
        assets = database.getAssets();
        for (int i=0; i<assets.length; i++)
        {
            totalAssets = totalAssets.add(assets[i].getBalance(date));
        }
        liabilities = database.getLiabilities();
        for (int i=0; i<liabilities.length; i++)
        {
            totalLiabilities = totalLiabilities.add(liabilities[i].getBalance(date));
        }
        
        // If this balance has a profit or a loss, then add it to the
        // correct side of the balance as an extra account to bring the balance
        // in balance.
        resultOfOperations = totalAssets.subtract(totalLiabilities);
        
        if (resultOfOperations.isPositive()) {
            // add a profit
            Account profitAccount = new ResultAccount(false, resultOfOperations, database);
            Account[] newLiabilities = new Account[liabilities.length+1];
            System.arraycopy(liabilities, 0, newLiabilities, 0, liabilities.length);
            newLiabilities[liabilities.length] = profitAccount;
            liabilities = newLiabilities;
            totalLiabilities = totalLiabilities.add(profitAccount.getBalance(date));
        } else if (resultOfOperations.isNegative()) {
            // add a loss
            Account lossAccount = new ResultAccount(true, 
                    resultOfOperations.negate(), database);
            Account[] newAssets = new Account[assets.length+1];
            System.arraycopy(assets, 0, newAssets, 0, assets.length);
            newAssets[assets.length] = lossAccount;
            assets = newAssets;
            totalAssets = totalAssets.add(lossAccount.getBalance(date));
        }
    }

    
    public Date getDate() 
    {
        return date;
    }

    /**
     * Gets the assets of the balance.
     * @return the assets
     */
    public Account[] getAssets() 
    {
        return assets;
    }
    
    /**
     * Gets the Assets of the balance.
     * The operational result is not part of the liabilities. It can be
     * obtained by the method <code>getResult()</code>.
     * 
     * @return the liabilities
     */
    public Account[] getLiabilities() 
    {
        return liabilities;
    }
    
    /**
     * Gets the total of all assets.
     * @return the total of all assets
     */
    public Amount getTotalAssets()
    {
        return totalAssets;
    }

    /**
     * Gets the total of all liabilities.
     * @return the total of all liabilities
     */
    public Amount getTotalLiabilities()
    {
        return totalLiabilities;
    }
    
    /**
     * Gets the result of operations. 
     * @return the result of operations
     */
    public Amount getResultOfOperations()
    {
        return totalAssets.subtract(totalLiabilities);
    }
    
    /**
     * Gets a string representation for this balance.
     * Use this method only for testing.
     * @return a string representation for this balance
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        Locale defaultLocale = Locale.getDefault();
        AmountFormat af = new AmountFormat(defaultLocale);
        int columnWidth = 45;
        result.append("balance of ");
        result.append(date);
        result.append('\n');
        
        StringBuffer sb = null;
        Database database = Database.getInstance();
        Account[] assets = database.getAssets();
        Account[] liabilities = database.getLiabilities();
        
        int n = Math.max(assets.length, liabilities.length+1);
        int index = 0;
        for (int i=0; i<n; i++)
        {
            sb = new StringBuffer();
            if (i < assets.length)
            {
	            sb.append(assets[i].getId());
	            sb.append(' ');
	            sb.append(assets[i].getName());
	            index = sb.length();
	            sb.append(af.formatAmount(assets[i].getBalance(date)));
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
            
            if (i < liabilities.length)
            {
	            sb.append(liabilities[i].getId());
	            sb.append(' ');
	            sb.append(liabilities[i].getName());
	            index = sb.length();
	            sb.append(af.formatAmount(liabilities[i].getBalance(date)));
            }
            else if (i == liabilities.length)
            {
                sb.append((getResultOfOperations().isNegative()) ? "loss" : "profit");
                index = sb.length();
                sb.append(af.formatAmount(getResultOfOperations()));
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
        
        sb.append(af.formatAmount(getTotalAssets()));
        while (sb.length() < columnWidth)
        {
            sb.insert(0, ' ');
        }
        result.append(sb);
        result.append(" | ");
        
        sb = new StringBuffer();
        sb.append(af.formatAmount(getTotalLiabilities()));
        while (sb.length() < columnWidth)
        {
            sb.insert(0, ' ');
        }
        result.append(sb);
        result.append('\n');
        
        return result.toString();
    }
    
    /**
     * This class represents a profit or a loss on the balance.
     *
     * @author Sander Kooijmans
     */
    private class ResultAccount extends Account {

        private Amount amount;
        
        /**
         * Constructs a result acount.
         * @param debet Indicates whether this account is on the debet of the balance. 
         *         <code>true</code> indicates this account is a loss,
         *         <code>false</code> indicates this account is a profit.
         * @param amount the amount of result
         */
        public ResultAccount(boolean debet, Amount amount, Database database) {
            super("", TextResource.getInstance().getString(
                    debet ?"gen.loss" : "gen.profit"), 
                    debet, database);
            this.amount = amount;
        }

        /**
         * Gets the balance of this account at the specified date.
         * @param date the date
         * @return the balance of this account at the specified date
         */
        public Amount getBalance(Date date) {
            return amount;
        }
    }
}
