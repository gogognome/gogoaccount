/*
 * $Id: AccountSelectionDialog.java,v 1.4 2007-11-08 20:18:03 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.Frame;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nl.gogognome.swing.OkCancelDialog;
import nl.gogognome.text.TextResource;

import cf.engine.Account;
import cf.engine.Database;
import cf.ui.components.AccountSelectionBean;

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
        component.add(new JLabel(TextResource.getInstance().getString(id)));
        
        sbAccount = new AccountSelectionBean(database, null);
        component.add(sbAccount);
        
        componentInitialized(component);
    }

    /* (non-Javadoc)
     * @see cf.ui.OkCancelDialog#handleOk()
     */
    protected void handleOk() 
    {
        account = (Account)sbAccount.getSelectedAccount();
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
