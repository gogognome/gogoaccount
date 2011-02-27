/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package cf.ui.components;

import nl.gogognome.swing.JComboBoxWithKeyboardInput;
import cf.engine.Account;
import cf.engine.Database;

/**
 * This class implements a selection bean for <code>Account</code>s.
 *
 * @author Sander Kooijmans
 */
public class AccountSelectionBean extends JComboBoxWithKeyboardInput
{
    /** Contains the accounts that are shown in the combo box. */
    private static Account[] accounts;

    /**
     * Creates an <code>AccountCellEditor</code>.
     * @param database the database to get the accounts from
     * @param account the initial value shown in the editor;
     *        <code>null</code> indicates that no account is shown initially.
     */
    public AccountSelectionBean(Database database, Account account) {
        super();
		Account[] assets = database.getAssets();
		Account[] liabilities = database.getLiabilities();
		Account[] expenses = database.getExpenses();
		Account[] revenues = database.getRevenues();
		accounts = new Account[assets.length + liabilities.length + expenses.length + revenues.length];
		int index = 0;
		for (int i = 0; i < assets.length; i++) {
		    accounts[index] = assets[i];
		    index++;
        }
		for (int i = 0; i < liabilities.length; i++) {
		    accounts[index] = liabilities[i];
		    index++;
        }
		for (int i = 0; i < expenses.length; i++) {
		    accounts[index] = expenses[i];
		    index++;
        }
		for (int i = 0; i < revenues.length; i++) {
		    accounts[index] = revenues[i];
		    index++;
        }

		for (int i=0; i<accounts.length; i++) {
		    addItem(accounts[i].getId() + " " + accounts[i].getName());
		}

		// Select initial account
		index = -1;
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
     * @return the selected account or <code>null</code> if no account is selected
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
