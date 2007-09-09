/*
 * $Id: EditPartyView.java,v 1.2 2007-09-09 19:41:44 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Party;

/**
 * This class implements a view to edit a party. Either an existing party or 
 * a new party can be edited.
 *
 * @author Sander Kooijmans
 */
public class EditPartyView extends View {

    private JTextField tfId;
    private JTextField tfName;
    private JTextField tfAddress;
    private JTextField tfZipCode;
    private JTextField tfCity;
    private DateModel dateModel;
    
    /** 
     * The party that was entered by the user. If the user cancels this dialog,
     * then <code>resultParty</code> is <code>null</code>.
     */
    private Party resultParty;

    /** The party used to initialize the view. */
    private Party initialParty;
    
    /** The ok button. */
    private JButton okButton;
    
    /**
     * Constructor.
     * @param party the party used to initialize the view
     */
    protected EditPartyView(Party party) {
        super();
        initialParty = party;
    }

    /** This method is called when the view is to be shown. */
    public void onInit() {
        add(createPanel());
    }
    
    /** This method is called when the view is to be closed. */
    public void onClose() {
        
    }
    
    /**
     * Gets the panel for editing a party.
     * @return the panel
     */
    private JPanel createPanel() {
        // Create the panel with labels and text fields
        JPanel textfieldPanel = new JPanel(new GridBagLayout());
        
        WidgetFactory wf = WidgetFactory.getInstance();
        
        int row = 0;
        textfieldPanel.add(wf.createLabel("editPartyView.id"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfId = wf.createTextField(30);
        textfieldPanel.add(tfId, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        textfieldPanel.add(wf.createLabel("editPartyView.name"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfName = wf.createTextField(30);
        textfieldPanel.add(tfName, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        textfieldPanel.add(wf.createLabel("editPartyView.address"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfAddress = wf.createTextField(30);
        textfieldPanel.add(tfAddress, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        textfieldPanel.add(wf.createLabel("editPartyView.zipCode"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfZipCode = wf.createTextField(30);
        textfieldPanel.add(tfZipCode, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        textfieldPanel.add(wf.createLabel("editPartyView.city"),
                SwingUtils.createLabelGBConstraints(0, row));
        tfCity = wf.createTextField(30);
        textfieldPanel.add(tfCity, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        textfieldPanel.add(wf.createLabel("editPartyView.birthDate"),
            SwingUtils.createLabelGBConstraints(0, row));
        dateModel = new DateModel();
        DateSelectionBean dsbBirthDate = new DateSelectionBean(dateModel);
        textfieldPanel.add(dsbBirthDate, SwingUtils.createTextFieldGBConstraints(1, row));

        if (initialParty != null) {
            tfId.setText(initialParty.getId());
            tfName.setText(initialParty.getName());
            tfAddress.setText(initialParty.getAddress());
            tfZipCode.setText(initialParty.getZipCode());
            tfCity.setText(initialParty.getCity());
            dateModel.setDate(initialParty.getBirthDate(), null);
        }
        
        // Create panel with buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        okButton = wf.createButton("gen.ok", new AbstractAction() { 
            public void actionPerformed(ActionEvent event) {
                resultParty = new Party(tfId.getText(), tfName.getText(),
                    tfAddress.getText(), tfZipCode.getText(), tfCity.getText(), dateModel.getDate());
                closeAction.actionPerformed(event);
            }  
        }); 
        buttonPanel.add(okButton);
        buttonPanel.add(wf.createButton("gen.cancel", closeAction));
        
        // Create overall panel
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(textfieldPanel, SwingUtils.createPanelGBConstraints(0, 0));
        panel.add(buttonPanel, SwingUtils.createPanelGBConstraints(0, 1));
        
        return panel;
    }
    
    /**
     * Gets the party that was entered by the user.
     * @return the party or <code>null</code> if the user canceled this dialog 
     */
    public Party getEnteredParty() {
        return resultParty;
    }

    /**
     * Gets the title of this view.
     * @return the title of this view
     */
    public String getTitle() {
        return TextResource.getInstance().getString(initialParty != null ? "editPartyView.titleEdit" : "editPartyView.titleAdd");
    }
    
    /**
     * Gets the default button of this view.
     * @return the default button of this view or <code>null</code> if this view
     *         has no default button
     */
    public JButton getDefaultButton() {
        return okButton;
    }
}
