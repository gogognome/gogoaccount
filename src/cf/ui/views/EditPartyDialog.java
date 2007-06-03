/*
 * $Id: EditPartyDialog.java,v 1.2 2007-06-03 11:10:10 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.Frame;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.swing.OkCancelDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import cf.engine.Party;

/**
 * This class implements a dialog to edit a party. Either an existing party or 
 * a new party can be edited.
 *
 * @author Sander Kooijmans
 */
public class EditPartyDialog extends OkCancelDialog {

    private JTextField tfId;
    private JTextField tfName;
    private JTextField tfAddress;
    private JTextField tfZipCode;
    private JTextField tfCity;
    
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
        componentInitialized(getPanel(party));
    }

    /**
     * Constructor.
     * @param the parent of this dialog
     */
    protected EditPartyDialog(Frame parent) {
        super(parent, "editPartyDialog.titleAdd");
        componentInitialized(getPanel(null));
    }
    
    /**
     * Gets the panel for editing a party.
     * @param initialParty the party used to initialize the text fields
     * @return the panel
     */
    private JPanel getPanel(Party initialParty) {
        JPanel panel = new JPanel(new GridBagLayout());
        
        WidgetFactory wf = WidgetFactory.getInstance();
        
        int row = 0;
        panel.add(wf.createLabel("editPartyDialog.id"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfId = wf.createTextField(30);
        panel.add(tfId, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        panel.add(wf.createLabel("editPartyDialog.name"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfName = wf.createTextField(30);
        panel.add(tfName, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        panel.add(wf.createLabel("editPartyDialog.address"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfAddress = wf.createTextField(30);
        panel.add(tfAddress, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        panel.add(wf.createLabel("editPartyDialog.zipCode"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfZipCode = wf.createTextField(30);
        panel.add(tfZipCode, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        panel.add(wf.createLabel("editPartyDialog.city"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfCity = wf.createTextField(30);
        panel.add(tfCity, SwingUtils.createTextFieldGBConstraints(1, row));
        
        // TODO: add text fields to the panel and initialize the text fields with values from initialParty
        return panel;
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
