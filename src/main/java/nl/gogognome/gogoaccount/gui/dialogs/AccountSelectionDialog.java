package nl.gogognome.gogoaccount.gui.dialogs;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.gui.components.AccountSelectionBean;
import nl.gogognome.lib.swing.OkCancelDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
     * @param accounts accounts to select from.
     * @param id the identifer of the description shown in this dialog.
     */
    public AccountSelectionDialog(Frame frame, List<Account> accounts, String id)
    {
        super(frame, "as.selectAccount");
        JComponent component = new JPanel();
        component.add(new JLabel(Factory.getInstance(TextResource.class).getString(id)));

        sbAccount = new AccountSelectionBean(accounts, null);
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
