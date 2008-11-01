/*
 * $Id: EditDescriptionAndAmountView.java,v 1.1 2008-11-01 13:26:01 sanderk Exp $
 *
 * Copyright (C) 2005 Sander Kooijmans
 *
 */

package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.Currency;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import nl.gogognome.framework.View;
import nl.gogognome.swing.ButtonPanel;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.Amount;
import nl.gogognome.text.TextResource;

/**
 * This view allows the user to edit a description and amount. It is typically used
 * for creating or updating invoices. 
 */
public class EditDescriptionAndAmountView extends View {

    /** The id of the title. */
    private String titleId;

    private String initialDescription;
    
    private Amount initialAmount;

    private Currency currency;
    
    private JTextField tfDescription;
    
    private JTextField tfAmount;
    
    /** The description as entered by the user or <code>null</code> if the user cancelled the view. */ 
    private String editedDescription;
    
    /**
     * The amount as entered by the user; <code>null</code> if the user cancelled the view or
     * if the user did not enter an amount.
     */
    private Amount editedAmount;
    
    public EditDescriptionAndAmountView(String titleId, Currency currency) {
        this(titleId, null, null, currency);
    }
    
    public EditDescriptionAndAmountView(String titleId, String initialDescription, Amount initialAmount, 
            Currency currency) {
        this.titleId = titleId;
        this.initialDescription = initialDescription;
        this.initialAmount = initialAmount;
        this.currency = currency;
    }
    
    @Override
    public String getTitle() {
        return TextResource.getInstance().getString(titleId);
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onInit() {
        WidgetFactory wf = WidgetFactory.getInstance();

        // Create panel with ID, issue date, concerning party, paying party and amount to be paid.
        GridBagLayout gbl = new GridBagLayout();
        JPanel topPanel = new JPanel(gbl);

        int row = 0;
        tfDescription = new JTextField(initialDescription);
        JLabel label = wf.createLabel("editDescriptionAndAmountView.description", tfDescription);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(tfDescription, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        String amount;
        if (initialAmount != null) {
            amount = TextResource.getInstance().getAmountFormat().formatAmountWithoutCurrency(initialAmount);
        } else {
            amount = "";
        }
        tfAmount = new JTextField(amount);
        label = wf.createLabel("editDescriptionAndAmountView.amount", tfAmount);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(tfAmount, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        JButton button = wf.createButton("gen.ok", new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                onOk();
            }
        });
        buttonPanel.add(button);
        button = wf.createButton("gen.cancel", closeAction);
        buttonPanel.add(button);
        
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * This method is called when the user selects the Ok action. 
     */
    private void onOk() {
        TextResource tr = TextResource.getInstance();
        try {
            editedAmount = tr.getAmountFormat().parse(tfAmount.getText(), currency);
        } catch (ParseException e) {
            MessageDialog.showMessage(this, "gen.warning", tr.getString("ejid.invalidAmount"));
            return;
        } 
        editedDescription = tfDescription.getText();
        closeAction.actionPerformed(null);
    }

    /** 
     * Gets the description as entered by the user.
     * @return the description as entered by the user or <code>null</code> if the user cancelled the view.
     */ 
    public String getEditedDescription() {
        return editedDescription;
    }

    /**
     * Gets the amount as entered by the user.
     * @return the amount as entered by the user; <code>null</code> if the user cancelled the view or
     *         if the user did not enter an amount.
     */
    public Amount getEditedAmount() {
        return editedAmount;
    }
}
