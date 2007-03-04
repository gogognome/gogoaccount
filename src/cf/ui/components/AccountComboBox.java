/*
 * $Id: AccountComboBox.java,v 1.2 2007-01-15 18:32:46 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import nl.gogognome.swing.JComboBoxWithKeyboardInput;
import cf.engine.Account;
import cf.engine.Database;

/**
 * This class implements a combo box for <code>Account</code>s.
 *
 * @author Sander Kooijmans
 */
public class AccountComboBox extends JComboBoxWithKeyboardInput {

    /** Contains the accounts that are shown in the combo box. */ 
    private static Account[] accounts;
    
    /**
     * Constructor.
     */
    public AccountComboBox(Database database) {
        super();
		Account[] assets = database.getAssets();
		Account[] liabilities = database.getLiabilities();
		Account[] expenses = database.getExpenses();
		Account[] revenues = database.getRevenues();
		accounts = new Account[assets.length + liabilities.length + expenses.length + revenues.length];
		int index = 0;
		for (int i = 0; i < assets.length; i++) 
		{
		    accounts[index] = assets[i];
		    index++;
        }
		for (int i = 0; i < liabilities.length; i++) 
		{
		    accounts[index] = liabilities[i];
		    index++;
        }
		for (int i = 0; i < expenses.length; i++) 
		{
		    accounts[index] = expenses[i];
		    index++;
        }
		for (int i = 0; i < revenues.length; i++) 
		{
		    accounts[index] = revenues[i];
		    index++;
        }
		
		for (int i=0; i<accounts.length; i++)
		{
		    addItem(accounts[i].getId() + " " + accounts[i].getName());
		}
    }

    /**
     * Selects the specified account in the combo box.
     * If the account is not found, then no account will be selected.
     * 
     * @param account the account
     */
    public void selectAccount(Account account) {
		int index = -1;
		if (account != null) {
			for (int i=0; i<accounts.length; i++) {
			    if (accounts[i].equals(account)) {
			        index = i;
			    }
			}
		}
		setSelectedIndex(index);
    }

    /**
     * Gets the selected account.
     * @return the selected account or <code>null</code> if no account is selected.
     */
    public Account getSelectedAccount() {
        Account result = null;
        int index = getSelectedIndex();
        if (index != -1) {
            result = accounts[index];
        }
        return result;
    }
}
