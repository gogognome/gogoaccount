/*
 * $Id: AccountSelectionDialog.java,v 1.3 2007-03-04 21:04:36 sanderk Exp $
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
import cf.ui.components.AccountCellEditor;

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
    
    private AccountCellEditor accountCellEditor;
    
    /**
     * Constructor.
     * @param frame the frame to which this dialog belongs.
     * @param id the identifer of the description shown in this dialog.
     */
    public AccountSelectionDialog(Frame frame, String id) 
    {
        super(frame, "as.selectAccount");
        JComponent component = new JPanel();
        component.add(new JLabel(TextResource.getInstance().getString(id)));
        
        accountCellEditor = new AccountCellEditor(null);
        component.add(accountCellEditor.getComponent());
        
        componentInitialized(component);
    }

    /* (non-Javadoc)
     * @see cf.ui.OkCancelDialog#handleOk()
     */
    protected void handleOk() 
    {
        account = (Account)accountCellEditor.getCellEditorValue();
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
