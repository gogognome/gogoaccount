/*
 * $Id: EditPartyDialog.java,v 1.1 2007-05-21 15:57:07 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.Frame;

import nl.gogognome.swing.OkCancelDialog;
import cf.engine.Party;

/**
 * This class implements a dialog to edit a party. Either an existing party or 
 * a new party can be edited.
 *
 * @author Sander Kooijmans
 */
public class EditPartyDialog extends OkCancelDialog {

    /** 
     * The party that was entered by the user. If the user cancels this dialog,
     * then <code>resultParty</code> is <code>null</code>.
     */
    private Party resultParty;
    
    /**
     * Constructor.
     * @param the parent of this dialog
     */
    protected EditPartyDialog(Frame parent, Party party) {
        super(parent, "editPartyDialog.titleEdit");
        // TODO Auto-generated constructor stub
    }

    /**
     * Constructor.
     * @param the parent of this dialog
     */
    protected EditPartyDialog(Frame parent) {
        super(parent, "editPartyDialog.titleAdd");
        // TODO Auto-generated constructor stub
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see cf.ui.OkCancelDialog#handleOk()
     */
    protected void handleOk() {
        resultParty = null; // TODO: change this
    }

    /**
     * Gets the party that was entered by the user.
     * @return the party or <code>null</code> if the user canceled this dialog 
     */
    public Party getEnteredParty() {
        return resultParty;
    }
}
