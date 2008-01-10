/*
 * $Id: InvoiceGeneratorView.java,v 1.5 2008-01-10 21:18:13 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.ViewDialog;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.ButtonPanel;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.Amount;
import nl.gogognome.text.TextResource;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;
import cf.ui.components.AccountComboBox;
import cf.ui.components.AmountTextField;

/**
 * This class implements a view in which the user can generate invoices
 * for multiple parties.
 *
 * @author Sander Kooijmans
 */
public class InvoiceGeneratorView extends View {

    /** The database used to get data from and to which the generated invoices are added. */
    private Database database;
    
    /** Text field containing the description of the generated invoices. */
    private JTextField tfDescription = new JTextField();
    
    /** The date model for the date when the invoices are generated. */ 
    private DateModel invoiceGenerationDateModel;

    /** Text field containing the ID of the generated invoices. */ 
    private JTextField tfId = new JTextField();
    
    /** Instances of this class represent a single line of the invoice template. */
    private class TemplateLine {
        JRadioButton rbParty = new JRadioButton();
        AccountComboBox cbAccount =
            new AccountComboBox(database);
        AmountTextField tfDebet = 
            new AmountTextField(database.getCurrency());
        AmountTextField tfCredit =
            new AmountTextField(database.getCurrency());
        
        public TemplateLine() {
            radioButtonGroup.add(rbParty);
            cbAccount.selectAccount(null);
        }
    }
    
    /** The button group of the radio buttons. */
    private ButtonGroup radioButtonGroup = new ButtonGroup();
    
    /** Contains the lines of the template. */
    private ArrayList<TemplateLine> templateLines = new ArrayList<TemplateLine>();
    
    /** The panel that contains the template lines. */
    private JPanel templateLinesPanel;
    
    /**
     * Constructor.
     * @param database the database used to get data from and to which the generated invoices are added
     */
    public InvoiceGeneratorView(Database database) {
        super();
        this.database = database;
    }

    /** Gets the title of this view. */
    public String getTitle() {
        return TextResource.getInstance().getString("invoiceGeneratorView.title");
    }

    /** This method is called when the view is closed. */
    public void onClose() {
    }

    /**
     * Initializes the view. 
     */
    public void onInit() {
        TextResource tr = TextResource.getInstance();
        
        // Initialize template panel
        JPanel templatePanel = new JPanel(new BorderLayout());
        templatePanel.setBorder(new CompoundBorder(
            new TitledBorder(tr.getString("invoicegenerator.template")),
            new EmptyBorder(10, 10, 10, 10)));
        
        WidgetFactory wf = WidgetFactory.getInstance();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(wf.createLabel("invoicegenerator.id", tfId),
                SwingUtils.createLabelGBConstraints(0, 0));
        panel.add(tfId,
                SwingUtils.createTextFieldGBConstraints(1, 0));
        tfId.setToolTipText(tr.getString("invoicegenerator.tooltip"));
        invoiceGenerationDateModel = new DateModel();
        invoiceGenerationDateModel.setDate(new Date(), null);
        DateSelectionBean dateSelectionBean = new DateSelectionBean(invoiceGenerationDateModel);
        panel.add(WidgetFactory.getInstance().createLabel("invoiceGeneratorView.date", dateSelectionBean),
                SwingUtils.createLabelGBConstraints(0, 1));
        panel.add(dateSelectionBean,
                SwingUtils.createTextFieldGBConstraints(1, 1));
        panel.add(wf.createLabel("invoicegenerator.description", tfDescription),
                SwingUtils.createLabelGBConstraints(0, 2));
        panel.add(tfDescription,
                SwingUtils.createTextFieldGBConstraints(1, 2));
        tfDescription.setToolTipText(tr.getString("invoicegenerator.tooltip"));

        panel.setBorder(new EmptyBorder(0, 0, 12, 0));
        
        templateLinesPanel = new JPanel(new GridBagLayout());
        
        // Add two empty lines so the user can start editing the template.
        for (int i=0; i<2; i++) {
            templateLines.add(new TemplateLine());
        }
        updateTemplateLinesPanel();

        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
        buttonPanel.add(wf.createButton("invoiceGeneratorView.addInvoices", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onAddInvoicesToBookkeeping();
            }
        }));
        buttonPanel.add(wf.createButton("invoiceGeneratorView.done", closeAction));
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        templatePanel.add(panel, BorderLayout.NORTH);
        templatePanel.add(templateLinesPanel, BorderLayout.CENTER);
        templatePanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add panels to the view
        add(templatePanel);
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
            TemplateLine line = templateLines.get(i);
            int top = 3;
            int bottom = 3;
            templateLinesPanel.add(line.rbParty,
                    SwingUtils.createGBConstraints(0, row, 1, 1, 0.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.NONE,
                            top, 0, bottom, 5));
            templateLinesPanel.add(line.cbAccount,
                    SwingUtils.createGBConstraints(1, row, 1, 1, 3.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            top, 0, bottom, 5));
            templateLinesPanel.add(line.tfDebet,
                    SwingUtils.createGBConstraints(2, row, 1, 1, 1.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            top, 0, bottom, 5));
            templateLinesPanel.add(line.tfCredit,
                    SwingUtils.createGBConstraints(3, row, 1, 1, 1.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            top, 0, bottom, 5));
            
            JButton deleteButton = wf.createButton("invoicegenerator.delete", null);
            deleteButton.addActionListener(new DeleteActionListener(i));
            templateLinesPanel.add(deleteButton, 
                    SwingUtils.createGBConstraints(4, row, 1, 1, 1.0, 0.0, 
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            top, 5, bottom, 0));
            row++;
        }
        
        JButton newButton = wf.createButton("invoicegenerator.new", new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                templateLines.add(new TemplateLine());
                updateTemplateLinesPanel();
            }
        });
        templateLinesPanel.add(newButton, 
                SwingUtils.createGBConstraints(4, row, 1, 1, 1.0, 0.0, 
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        0, 0, 0, 0));
        
        revalidate();
        repaint();
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
            TemplateLine line = templateLines.remove(index);
            radioButtonGroup.remove(line.rbParty);
            updateTemplateLinesPanel();
        }
	}
	
	/**
     * This method is called when the "add invoices" button has been pressed.
	 * The user will be asked to select the parties for which invoices are generated.
     * After that, a dialog asks whether the user is sure to continue. Only if the user explicitly
     * states "yes" the invoices will be added to the bookkeeping.
	 */
	private void onAddInvoicesToBookkeeping() {
	    // Add a journal for each selected party
        PartiesView partiesView = new PartiesView(database, true, true);
        ViewDialog dialog = new ViewDialog(getParentWindow(), partiesView);
        dialog.showDialog();
        Party[] parties = partiesView.getSelectedParties();
        if (parties == null) {
            // No parties have been selected. Abort this method.
            return;
        }

        MessageDialog messageDialog = MessageDialog.showMessage(this, "gen.titleWarning", 
            TextResource.getInstance().getString("invoicegenerator.areYouSure"),
            new String[] { "gen.yes", "gen.no" });
        if (messageDialog.getSelectedButton() != 0) {
            // The user cancelled the operation.
            return;
        }

        // TODO: Move most of this code to the engine package.
	    int nrInvoicesCreated = 0;
        
	    for (int i=0; i<parties.length; i++) {
            String id = replaceKeywords(tfId.getText(), parties[i]);
            String description = replaceKeywords(tfDescription.getText(), parties[i]);
            Date date = invoiceGenerationDateModel.getDate();
            if (date == null) {
                MessageDialog.showMessage(this, "gen.titleError", 
                        TextResource.getInstance().getString("gen.invalidDate"));
                return;
            }
            
    	    // Create journal items from the template
            Amount amountToBePaid = null;
            String[] descriptions = new String[templateLines.size() - 1];
            Amount[] amounts = new Amount[templateLines.size() - 1];
            int descriptionIndex = 0;
            // First create the invoice instance. It is needed when the journal is created.
    	    for (int l=0; l<templateLines.size(); l++) {
    	        TemplateLine line = templateLines.get(l);
    	        Account account = line.cbAccount.getSelectedAccount();
    	        if (account == null) {
                    MessageDialog.showMessage(this, "gen.titleError", 
                            TextResource.getInstance().getString("invoicegenerator.emptyAccountFound"));
                    return;
    	        }
    	        Amount amount = line.tfDebet.getAmount();
    	        boolean debet = amount != null;
                if (line.rbParty.isSelected()) {
                    amountToBePaid = amount;
                }
    	        if (amount == null) {
    	            amount = line.tfCredit.getAmount();
                    if (line.rbParty.isSelected()) {
                        amountToBePaid = amount.negate();
                    }
    	        }
    	        
    	        if (amount == null) {
                    MessageDialog.showMessage(this, "gen.titleError", 
                            TextResource.getInstance().getString("invoicegenerator.emptyAmountsFound"));
    	            return;
    	        }
                
                if (!line.rbParty.isSelected()) {
                    descriptions[descriptionIndex] = account.getId() + " - " + account.getName();
                    amounts[descriptionIndex] = debet ? amount.negate() : amount;
                    descriptionIndex++;
                }
    	    }
            
            Invoice invoice = new Invoice(id, parties[i], parties[i], amountToBePaid, date,
                descriptions, amounts);
            try {
                database.addInvoice(invoice);
            } catch (DatabaseModificationFailedException e) {
                MessageDialog.showMessage(this, "gen.titleError", e.getMessage());
            }

    	    // Create the journal.
            JournalItem[] items = new JournalItem[templateLines.size()];
            for (int l=0; l<templateLines.size(); l++) {
                TemplateLine line = templateLines.get(l);
                Account account = line.cbAccount.getSelectedAccount();
                if (account == null) {
                    MessageDialog.showMessage(this, "gen.titleError", 
                            TextResource.getInstance().getString("invoicegenerator.emptyAccountFound"));
                    return;
                }
                Amount amount = line.tfDebet.getAmount();
                boolean debet = amount != null;
                if (line.rbParty.isSelected()) {
                    amountToBePaid = amount;
                }
                if (amount == null) {
                    amount = line.tfCredit.getAmount();
                    if (line.rbParty.isSelected()) {
                        amountToBePaid = amount.negate();
                    }
                }
                
                if (amount == null) {
                    MessageDialog.showMessage(this, "gen.titleError", 
                            TextResource.getInstance().getString("invoicegenerator.emptyAmountsFound"));
                    return;
                }
                
                items[l] = new JournalItem(amount, account, debet, 
                    line.rbParty.isSelected() ? invoice : null);
            }
            
            Journal journal;
            try {
                journal = new Journal(id, description, date, items);
            } catch (IllegalArgumentException e) {
                MessageDialog.showMessage(this, "gen.titleError", 
                        TextResource.getInstance().getString("gen.itemsNotInBalance"));
                return;
            }
    	    
    	    database.addJournal(journal);
            nrInvoicesCreated++;
	    }
	    
	    MessageDialog.showMessage(this, "gen.titleMessage",
	            TextResource.getInstance().getString("invoicegenerator.messageSuccess",
	            	new Object[] { new Integer(nrInvoicesCreated) }));
	}
	
    /**
     * Replaces the keywords <code>{id}</code> and <code>{name}</code> with the corresponding
     * attributes of the specified party.
     * @param s the string in which the replacement has to be made
     * @param party the party
     * @return the string after the replacements have taken place
     */
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
