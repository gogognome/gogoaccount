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
package nl.gogognome.gogoaccount.gui.components;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.lib.swing.JComboBoxWithKeyboardInput;

import java.util.List;

/**
 * This class implements a selection bean for <code>Account</code>s.
 *
 * @author Sander Kooijmans
 */
public class AccountSelectionBean extends JComboBoxWithKeyboardInput
{
	private static final long serialVersionUID = 1L;

	/** Contains the accounts that are shown in the combo box. */
    private List<Account> accounts;

    /**
     * Creates an <code>AccountCellEditor</code>.
     * @param accounts all accounts from which one account must be selected
     * @param account the initial value shown in the editor;
     *        <code>null</code> indicates that no account is shown initially.
     */
    public AccountSelectionBean(List<Account> accounts, Account account) {
        super();

        this.accounts = accounts;
		for (Account a : accounts) {
		    addItem(a.getId() + ' ' + a.getName());
		}

		// Select initial account
		int index = -1;
		if (account != null) {
			for (int i=0; i<accounts.size(); i++) {
			    if (accounts.get(i).equals(account)) {
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
            result = accounts.get(index);
        }
        return result;
    }
}
