/*
 * $Id: EditPartyView.java,v 1.5 2007-11-11 19:51:34 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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
    private JTextField tfType;
    private JTextArea taRemarks;
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
        setDefaultButton(okButton);
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
        JLabel label = wf.createLabel("editPartyView.id");
        textfieldPanel.add(label,
                SwingUtils.createLabelGBConstraints(0, row));
        tfId = wf.createTextField(30);
        textfieldPanel.add(tfId, SwingUtils.createTextFieldGBConstraints(1, row));
        label.setLabelFor(tfId);
        row++;
        
        label = wf.createLabel("editPartyView.name");
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        tfName = wf.createTextField(30);
        textfieldPanel.add(tfName, SwingUtils.createTextFieldGBConstraints(1, row));
        label.setLabelFor(tfName);
        row++;
        
        label = wf.createLabel("editPartyView.address");
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        tfAddress = wf.createTextField(30);
        textfieldPanel.add(tfAddress, SwingUtils.createTextFieldGBConstraints(1, row));
        label.setLabelFor(tfAddress);
        row++;

        label = wf.createLabel("editPartyView.zipCode");
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        tfZipCode = wf.createTextField(30);
        textfieldPanel.add(tfZipCode, SwingUtils.createTextFieldGBConstraints(1, row));
        label.setLabelFor(tfZipCode);
        row++;
        
        label = wf.createLabel("editPartyView.city");
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        tfCity = wf.createTextField(30);
        textfieldPanel.add(tfCity, SwingUtils.createTextFieldGBConstraints(1, row));
        label.setLabelFor(tfCity);
        row++;

        label = wf.createLabel("editPartyView.type");
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        tfType = wf.createTextField(30);
        textfieldPanel.add(tfType, SwingUtils.createTextFieldGBConstraints(1, row));
        label.setLabelFor(tfType);
        row++;

        label = wf.createLabel("editPartyView.remarks");
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        taRemarks = new JTextArea(5, 30);
        JScrollPane remarksPane = new JScrollPane(taRemarks);
        textfieldPanel.add(remarksPane, SwingUtils.createTextFieldGBConstraints(1, row));
        label.setLabelFor(remarksPane);
        row++;

        label = wf.createLabel("editPartyView.birthDate");
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        dateModel = new DateModel();
        DateSelectionBean dsbBirthDate = new DateSelectionBean(dateModel);
        textfieldPanel.add(dsbBirthDate, SwingUtils.createTextFieldGBConstraints(1, row));
        label.setLabelFor(dsbBirthDate);

        if (initialParty != null) {
            tfId.setText(initialParty.getId());
            tfName.setText(initialParty.getName());
            tfAddress.setText(initialParty.getAddress());
            tfZipCode.setText(initialParty.getZipCode());
            tfCity.setText(initialParty.getCity());
            tfType.setText(initialParty.getType());
            taRemarks.setText(initialParty.getRemarks());
            dateModel.setDate(initialParty.getBirthDate(), null);
        }
        
        // Create panel with buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        okButton = wf.createButton("gen.ok", new AbstractAction() { 
            public void actionPerformed(ActionEvent event) {
                resultParty = new Party(tfId.getText(), tfName.getText(),
                    tfAddress.getText(), tfZipCode.getText(), tfCity.getText(), dateModel.getDate(),
                    tfType.getText(), taRemarks.getText());
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
}
