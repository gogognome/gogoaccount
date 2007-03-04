/*
 * $Id: InvoiceGeneratorDialog.java,v 1.5 2007-02-10 16:28:46 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;
import cf.ui.components.AccountComboBox;
import cf.ui.components.AmountTextField;

import nl.gogognome.swing.DialogWithButtons;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.Amount;
import nl.gogognome.text.TextResource;

/**
 * This class implements a dialog in which the user can generate invoices
 * for multiple parties.
 *
 * @author Sander Kooijmans
 */
public class InvoiceGeneratorDialog extends DialogWithButtons {

    /** Text field containing the description of the generated invoices. */
    private JTextField tfDescription = new JTextField();
    
    /** Text field containing the date of the generated invoices. */ 
    private JTextField tfDate = new JTextField();

    /** Text field containing the ID of the generated invoices. */ 
    private JTextField tfId = new JTextField();
    
    private class TemplateLine {
        JRadioButton rbParty = new JRadioButton();
        AccountComboBox cbAccount =
            new AccountComboBox(Database.getInstance());
        AmountTextField tfDebet = 
            new AmountTextField(Database.getInstance().getCurrency());
        AmountTextField tfCredit =
            new AmountTextField(Database.getInstance().getCurrency());
        
        public TemplateLine() {
            radioButtonGroup.add(rbParty);
            cbAccount.selectAccount(null);
        }
    }
    
    /** The button group of the radio buttons. */
    private ButtonGroup radioButtonGroup = new ButtonGroup();
    
    /** Contains the lines of the template. */
    private ArrayList templateLines = new ArrayList();
    
    /** The panel that contains the template lines. */
    private JPanel templateLinesPanel;
    
    /** The date format used to format and parse the date. */
    private SimpleDateFormat dateFormat;
    
    /** The parties that are shown in the dialog. */
    private Party[] parties;
    
    /** Check box used to indicate for which parties an invoice is generated. */ 
    private JCheckBox[] cbIncluded;
    
    /** Check box used to indicate for which parties an invoice has been generated. */
    private JCheckBox[] cbDone;
    
    /** The parent of this dialog. */
    private Frame parent;
    
    /**
     * Constructor.
     * @param frame the parent of this dialog
     */
    public InvoiceGeneratorDialog(Frame frame) {
        super(frame, "invoicegenerator.title", new String[] {
           "invoicegenerator.addInvoices", "invoicegenerator.done"
        });
        this.parent = frame;
        Database database = Database.getInstance();
        TextResource tr = TextResource.getInstance();
        
        // Initialize template panel
        JPanel templatePanel = new JPanel(new BorderLayout());
        templatePanel.setBorder(new TitledBorder(
                tr.getString("invoicegenerator.template")));
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel(tr.getString("invoicegenerator.id")),
                SwingUtils.createLabelGBConstraints(0, 0));
        panel.add(tfId,
                SwingUtils.createTextFieldGBConstraints(1, 0));
        tfId.setToolTipText(tr.getString("invoicegenerator.tooltip"));
        dateFormat = new SimpleDateFormat(tr.getString("gen.dateFormat"));
        tfDate.setText(dateFormat.format(new java.util.Date()));
        panel.add(new JLabel(tr.getString("invoicegenerator.date")),
                SwingUtils.createLabelGBConstraints(0, 1));
        panel.add(tfDate,
                SwingUtils.createTextFieldGBConstraints(1, 1));
        panel.add(new JLabel(tr.getString("invoicegenerator.description")),
                SwingUtils.createLabelGBConstraints(0, 2));
        panel.add(tfDescription,
                SwingUtils.createTextFieldGBConstraints(1, 2));
        tfDescription.setToolTipText(tr.getString("invoicegenerator.tooltip"));

        panel.setBorder(new EmptyBorder(0, 0, 12, 0));
        
        templateLinesPanel = new JPanel(new GridBagLayout());
        
        for (int i=0; i<2; i++) {
            templateLines.add(new TemplateLine());
        }
        updateTemplateLinesPanel();

        templatePanel.add(panel, BorderLayout.NORTH);
        templatePanel.add(templateLinesPanel, BorderLayout.CENTER);
        
        // Initialize parties panel
        JPanel partiesTitledBorderdPanel = new JPanel(new BorderLayout());
        partiesTitledBorderdPanel.setBorder(
                new TitledBorder(tr.getString("invoicegenerator.parties")));
        
        JPanel partiesPanel = new JPanel(new GridBagLayout());
        parties = database.getParties();
        
        partiesPanel.add(new JLabel(tr.getString("invoicegenerator.include")),
                SwingUtils.createLabelGBConstraints(0, 0));
        partiesPanel.add(new JLabel(tr.getString("invoicegenerator.done")),
                SwingUtils.createLabelGBConstraints(1, 0));
        partiesPanel.add(new JLabel(tr.getString("invoicegenerator.party")),
                SwingUtils.createLabelGBConstraints(2, 0));
        
        cbIncluded = new JCheckBox[parties.length];
        cbDone = new JCheckBox[parties.length];
        for (int i = 0; i < parties.length; i++) {
            cbIncluded[i] = new JCheckBox();
            partiesPanel.add(cbIncluded[i],
                SwingUtils.createGBConstraints(0, i+1, 1, 1, 0.0, 0.0, 
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    0, 0, 0, 0));
            
            cbDone[i] = new JCheckBox();
            cbDone[i].setEnabled(false);
            partiesPanel.add(cbDone[i],
                    SwingUtils.createGBConstraints(1, i+1, 1, 1, 0.0, 0.0, 
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        0, 0, 0, 0));

            partiesPanel.add(new JLabel(parties[i].getId() + " - " + parties[i].getName()),
                    SwingUtils.createGBConstraints(2, i+1, 1, 1, 1.0, 0.0, 
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        0, 0, 0, 0));
        }
        
        JScrollPane scrollPane = new JScrollPane(partiesPanel);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        partiesTitledBorderdPanel.add(scrollPane);
        
        // Add panels to the dialog
        panel = new JPanel(new BorderLayout());
        panel.add(templatePanel, BorderLayout.CENTER);
        panel.add(partiesTitledBorderdPanel, BorderLayout.EAST);
        
        componentInitialized(panel);
    }

    private void updateTemplateLinesPanel() {
        TextResource tr = TextResource.getInstance();
        WidgetFactory wf = WidgetFactory.getInstance();
        
        templateLinesPanel.removeAll();
        
        templateLinesPanel.add(new JLabel(tr.getString("gen.party")),
                SwingUtils.createLabelGBConstraints(0, 0));
        templateLinesPanel.add(new JLabel(tr.getString("gen.account")),
                SwingUtils.createLabelGBConstraints(1, 0));
        templateLinesPanel.add(new JLabel(tr.getString("gen.debet")),
                SwingUtils.createLabelGBConstraints(2, 0));
        templateLinesPanel.add(new JLabel(tr.getString("gen.credit")),
                SwingUtils.createLabelGBConstraints(3, 0));
        
        int row = 1;
        for (int i=0; i<templateLines.size(); i++) {
            TemplateLine line = (TemplateLine)templateLines.get(i);
            templateLinesPanel.add(line.rbParty,
                    SwingUtils.createGBConstraints(0, row, 1, 1, 0.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.NONE,
                            0, 0, 0, 0));
            templateLinesPanel.add(line.cbAccount,
                    SwingUtils.createGBConstraints(1, row, 1, 1, 3.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            0, 0, 0, 0));
            templateLinesPanel.add(line.tfDebet,
                    SwingUtils.createGBConstraints(2, row, 1, 1, 1.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            0, 0, 0, 0));
            templateLinesPanel.add(line.tfCredit,
                    SwingUtils.createGBConstraints(3, row, 1, 1, 1.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            0, 0, 0, 0));
            
            JButton deleteButton = wf.createButton("invoicegenerator.delete");
            deleteButton.addActionListener(new DeleteActionListener(i));
            templateLinesPanel.add(deleteButton, 
                    SwingUtils.createGBConstraints(4, row, 1, 1, 1.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            0, 0, 0, 0));
            row++;
        }
        
        JButton newButton = wf.createButton("invoicegenerator.new");
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                templateLines.add(new TemplateLine());
                updateTemplateLinesPanel();
            }
        });
        templateLinesPanel.add(newButton, 
                SwingUtils.createGBConstraints(4, row, 1, 1, 1.0, 0.0, 
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        0, 0, 0, 0));
        
        pack();
    }
    
	/**
	 * Handles the button-pressed event. This method is called when one of the buttons
	 * has been pressed by the user. 
	 * @param index the index of the button (as specified by the <tt>buttonIds</tt>
	 *        passed to the constructor. 
	 */
	protected void handleButton(int index) {
	    switch(index) {
	    case 0:
	        MessageDialog dialog = new MessageDialog(parent, "gen.titleWarning", 
	                TextResource.getInstance().getString("invoicegenerator.areYouSure"),
	                new String[] { "gen.yes", "gen.no" });
	        if (dialog.getSelectedButton() == 0) {
	            addInvoicesToBookkeeping();
	        }
	        break;
	        
	    case 1:
			hideDialog();
			break;
			
		default:
		    throw new IllegalArgumentException("Invalid index: " +index);
	    }
	}
	
	private class DeleteActionListener implements ActionListener {
	    /** Index of the line to be deleted by this action. */
	    private int index;
	    
	    /**
	     * Constructor.
	     * @param index index of the line to be deleted by this action.
	     */
	    public DeleteActionListener(int index) {
	        this.index = index;
	    }
	    
        public void actionPerformed(ActionEvent event) {
            TemplateLine line = (TemplateLine)templateLines.remove(index);
            radioButtonGroup.remove(line.rbParty);
            updateTemplateLinesPanel();
        }
	}
	
	/**
	 * Adds invoices to the bookkeeping. The invoice and parties are taken
	 * from the dialog.
	 */
	private void addInvoicesToBookkeeping() {
	    // Add a journal for each selected party
	    int nrInvoicesCreated = 0;
	    for (int i=0; i<parties.length; i++) {
	        if (cbIncluded[i].isSelected()) {
	    	    // Create journal items from the template
	    	    JournalItem[] items = new JournalItem[templateLines.size()];
	    	    for (int l=0; l<templateLines.size(); l++) {
	    	        TemplateLine line = (TemplateLine)templateLines.get(l);
	    	        Account account = line.cbAccount.getSelectedAccount();
	    	        if (account == null) {
	                    MessageDialog.showMessage(parent, "gen.titleError", 
	                            TextResource.getInstance().getString("invoicegenerator.emptyAccountFound"));
	                    return;
	    	        }
	    	        Amount amount = line.tfDebet.getAmount();
	    	        boolean debet = amount != null;
	    	        if (amount == null) {
	    	            amount = line.tfCredit.getAmount();
	    	        }
	    	        
	    	        if (amount == null) {
	                    MessageDialog.showMessage(parent, "gen.titleError", 
	                            TextResource.getInstance().getString("invoicegenerator.emptyAmountsFound"));
	    	            return;
	    	        }
	    	        Party party = line.rbParty.isSelected() ? parties[i] : null;
	    	        items[l] = new JournalItem(amount, account, debet, party);
	    	    }
	            
	    	    String id = replaceKeywords(tfId.getText(), parties[i]);
	    	    String description = replaceKeywords(tfDescription.getText(), parties[i]);
	    	    Date date;
                try {
                    date = dateFormat.parse(tfDate.getText());
                } catch (ParseException e) {
                    MessageDialog.showMessage(parent, "gen.titleError", 
                            TextResource.getInstance().getString("gen.invalidDate"));
                    return;
                }
                
                Journal journal;
                try {
                    journal = new Journal(id, description, date, items);
                } catch (IllegalArgumentException e) {
                    MessageDialog.showMessage(parent, "gen.titleError", 
                            TextResource.getInstance().getString("gen.itemsNotInBalance"));
                    return;
                }
	    	    
	    	    Database.getInstance().addJournal(journal);
	    	    
	            cbIncluded[i].setSelected(false);
	            cbDone[i].setSelected(true);
	            nrInvoicesCreated++;
	        }
	    }
	    
	    MessageDialog.showMessage(parent, "gen.titleMessage",
	            TextResource.getInstance().getString("invoicegenerator.messageSuccess",
	            	new Object[] { new Integer(nrInvoicesCreated) }));
	}
	
	private static String replaceKeywords(String s, Party party) {
	    StringBuffer sb = new StringBuffer(s);
	    String[] keywords = new String[] { "{id}", "{name}" };
	    String[] values = new String[] {
	            party.getId(), party.getName()
	    };
	    
	    for (int k=0; k<keywords.length; k++) {
	        String keyword = keywords[k];
	        String value = values[k];
		    for (int index=sb.indexOf(keyword); index != -1; index=sb.indexOf(keyword)) {
		        sb.replace(index, index+keyword.length(), value);
		    }
	    }
	    return sb.toString();
	}
}
