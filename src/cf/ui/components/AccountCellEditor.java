/*
 * $Id: AccountCellEditor.java,v 1.3 2007-01-15 18:32:46 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import javax.swing.DefaultCellEditor;

import nl.gogognome.swing.JComboBoxWithKeyboardInput;

import cf.engine.Account;
import cf.engine.Database;

/**
 * This class implements a <code>TableCellRenderer</code> for <code>Account</code>s.
 *
 * @author Sander Kooijmans
 */
public class AccountCellEditor extends DefaultCellEditor 
{
    private static JComboBoxWithKeyboardInput comboBox = new JComboBoxWithKeyboardInput();
    
    /** Contains the accounts that are shown in the combo box. */ 
    private static Account[] accounts;
    
    /**
     * Creates an <code>AccountCellEditor</code>.
     * @param account the initial value shown in the editor; 
     *        <code>null</code> indicates that no account is shown initially. 
     */
    public AccountCellEditor(Account account)
    {
        super(comboBox);
		Account[] assets = Database.getInstance().getAssets();
		Account[] liabilities = Database.getInstance().getLiabilities();
		Account[] expenses = Database.getInstance().getExpenses();
		Account[] revenues = Database.getInstance().getRevenues();
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
		    comboBox.addItem(accounts[i].getId() + " " + accounts[i].getName());
		}
		
		// Select initial account
		index = -1;
		if (account != null)
		{
			for (int i=0; i<accounts.length; i++)
			{
			    if (accounts[i].equals(account))
			    {
			        index = i;
			    }
			}
		}
		comboBox.setSelectedIndex(index);
    }
    
    public Object getCellEditorValue()
    {
        Object result = null;
        int index = comboBox.getSelectedIndex();
        if (index != -1)
        {
            result = accounts[index];
        }
        return result;
    }
}
