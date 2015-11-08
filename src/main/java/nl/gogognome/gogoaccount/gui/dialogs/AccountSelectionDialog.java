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
package nl.gogognome.gogoaccount.gui.dialogs;

import java.awt.Frame;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.gui.components.AccountSelectionBean;
import nl.gogognome.lib.swing.OkCancelDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

/**
 * This class implements an account selection dialog.
 *
 * @author Sander Kooijmans
 */
public class AccountSelectionDialog extends OkCancelDialog
{
    /**
     * The account selected by the user. <code>null</code> indicates that
     * the user did not select an account (e.g., by pressing the Cancel button).
     */
    private Account account;

    private AccountSelectionBean sbAccount;

    /**
     * Constructor.
     * @param frame the frame to which this dialog belongs.
     * @param database the database
     * @param id the identifer of the description shown in this dialog.
     */
    public AccountSelectionDialog(Frame frame, Database database, String id)
    {
        super(frame, "as.selectAccount");
        JComponent component = new JPanel();
        component.add(new JLabel(Factory.getInstance(TextResource.class).getString(id)));

        sbAccount = new AccountSelectionBean(database, null);
        component.add(sbAccount);

        componentInitialized(component);
    }

    /* (non-Javadoc)
     * @see cf.ui.OkCancelDialog#handleOk()
     */
    @Override
	protected void handleOk()
    {
        account = sbAccount.getSelectedAccount();
        if (account != null)
        {
            hideDialog();
        }
    }

    /**
     * Gets the account selected by the user.
     * @return the account selected by the user. <code>null</code> indicates that
     *         the user did not select an account (e.g., by pressing the Cancel button).
     */
    public Account getAccount()
    {
        return account;
    }
}
