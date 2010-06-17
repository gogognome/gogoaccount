/*
 * $Id: Balance.java,v 1.17 2010-06-17 12:27:30 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import cf.engine.Account.Type;

/**
 * This class represents a balance.
 *
 * @author Sander Kooijmans
 */
public class Balance {
    private Date date;

    private Amount totalAssets;

    private Amount totalLiabilities;

    private Amount resultOfOperations;

    private Account[] assets;

    private Account[] liabilities;

    /** Maps accounts to their amounts at the date of the balance. */
    private Map<Account, Amount> accountsToAmountsMap = new HashMap<Account, Amount>();

    /**
     * Constructor.
     * @param database the database on which this balance is based.
     * @param date the date of the balance.
     */
    protected Balance(Database database, Date date) {
        this.date = date;
        totalAssets = Amount.getZero(database.getCurrency());
        totalLiabilities = totalAssets; // is zero

        assets = database.getAssets();
        for (int i=0; i<assets.length; i++) {
        	Amount a = BookkeepingService.getAccountBalance(database, assets[i], date);
        	accountsToAmountsMap.put(assets[i], a);
            totalAssets = totalAssets.add(a);
        }
        liabilities = database.getLiabilities();
        for (int i=0; i<liabilities.length; i++) {
        	Amount a = BookkeepingService.getAccountBalance(database, liabilities[i], date);
        	accountsToAmountsMap.put(liabilities[i], a);
            totalLiabilities = totalLiabilities.add(a);
        }

        // If this balance has a profit or a loss, then add it to the
        // correct side of the balance as an extra account to bring the balance
        // in balance.
        resultOfOperations = totalAssets.subtract(totalLiabilities);

        if (resultOfOperations.isPositive()) {
            // add a profit
            Account profitAccount = new ResultAccount(false, resultOfOperations, Type.LIABILITY);
            Account[] newLiabilities = new Account[liabilities.length+1];
            System.arraycopy(liabilities, 0, newLiabilities, 0, liabilities.length);
            newLiabilities[liabilities.length] = profitAccount;
            liabilities = newLiabilities;
            totalLiabilities = totalLiabilities.add(resultOfOperations);
            accountsToAmountsMap.put(profitAccount, resultOfOperations);
        } else if (resultOfOperations.isNegative()) {
            // add a loss
            Account lossAccount = new ResultAccount(true,
                    resultOfOperations.negate(), Type.ASSET);
            Account[] newAssets = new Account[assets.length+1];
            System.arraycopy(assets, 0, newAssets, 0, assets.length);
            newAssets[assets.length] = lossAccount;
            assets = newAssets;
            Amount a = resultOfOperations.negate(); // resultOfOperations is negative
            totalAssets = totalAssets.add(a);
            accountsToAmountsMap.put(lossAccount, a);
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
    public Account[] getAssets() {
        return assets;
    }

    /**
     * Gets the Assets of the balance.
     * The operational result is not part of the liabilities. It can be
     * obtained by the method <code>getResult()</code>.
     *
     * @return the liabilities
     */
    public Account[] getLiabilities() {
        return liabilities;
    }

    /**
     * Gets the total of all assets.
     * @return the total of all assets
     */
    public Amount getTotalAssets() {
        return totalAssets;
    }

    /**
     * Gets the total of all liabilities.
     * @return the total of all liabilities
     */
    public Amount getTotalLiabilities() {
        return totalLiabilities;
    }

    /**
     * Gets the result of operations.
     * @return the result of operations
     */
    public Amount getResultOfOperations() {
        return resultOfOperations;
    }

    /**
     * Gets the amount for the specified account at the date of this balance.
     * @param account the account
     * @return the amount
     */
    public Amount getAmount(Account account) {
    	return accountsToAmountsMap.get(account);
    }

    /**
     * Gets a string representation for this balance.
     * Use this method only for testing.
     * @return a string representation for this balance
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        Locale defaultLocale = Locale.getDefault();
        AmountFormat af = new AmountFormat(defaultLocale);
        int columnWidth = 45;
        result.append("balance of ");
        result.append(date);
        result.append('\n');

        StringBuilder sb = null;

        int n = Math.max(assets.length, liabilities.length+1);
        int index = 0;
        for (int i=0; i<n; i++) {
            sb = new StringBuilder();
            if (i < assets.length) {
	            sb.append(assets[i].getId());
	            sb.append(' ');
	            sb.append(assets[i].getName());
	            index = sb.length();
	            sb.append(getAmount(assets[i]));
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

            sb = new StringBuilder();

            if (i < liabilities.length)
            {
	            sb.append(liabilities[i].getId());
	            sb.append(' ');
	            sb.append(liabilities[i].getName());
	            index = sb.length();
	            sb.append(af.formatAmount(getAmount(liabilities[i])));
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
        sb = new StringBuilder();

        sb.append(af.formatAmount(getTotalAssets()));
        while (sb.length() < columnWidth)
        {
            sb.insert(0, ' ');
        }
        result.append(sb);
        result.append(" | ");

        sb = new StringBuilder();
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
        public ResultAccount(boolean debet, Amount amount, Type type) {
            super("", TextResource.getInstance().getString(
                    debet ?"gen.loss" : "gen.profit"),
                    debet, type);
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
