/*
 * $Id: EditJournalItemDialog.java,v 1.5 2007-02-10 16:28:46 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.OkCancelDialog;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;

import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Party;
import cf.engine.JournalItem;
import cf.ui.components.AccountCellEditor;
import cf.ui.components.PartySelector;

/**
 * This class implements the Edit Journal Item dialog.
 *
 * @author Sander Kooijmans
 */
class EditJournalItemDialog extends OkCancelDialog
{
    private AccountCellEditor accountCellEditor;
    
    private JTextField tfAmount;
    
    private JComboBox cbSide;
    
    private PartySelector partySelector;
    
    private Frame parent;
    
    
    /** 
     * Contains the journal item thas has been entered.
     * Its value will be set when the user presses the Ok button and the input fields
     * are correct. Otherwise, this variable will be <code>null</code>.
     */
    private JournalItem enteredJournalItem;
    
    /**
     * Constructor.
     * @param parent the parent frame of this dialog.
     * @param titleId the id of this dialog's title.
     */
    public EditJournalItemDialog(Frame parent, String titleId) 
    {
        super(parent, titleId);
        this.parent = parent;
        initDialog("", null, true, null);
    }

    /**
     * Constructor.
     * @param parent the parent dialog of this dialog.
     * @param titleId the id of this dialog's title.
     * @param item the item used to fill in the initial values of the fields.
     */
    public EditJournalItemDialog(Frame parent, String titleId, JournalItem item) 
    {
        super(parent, titleId);
        this.parent = parent;
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        initDialog(af.formatAmountWithoutCurrency(item.getAmount()), item.getAccount(), 
                item.isDebet(), item.getParty());
    }

    /**
     * Initializes the dialog. Adds buttons and labels to this dialog.
     * @param amount used to initialize the amount field
     * @param account used to initialize the account combo box
     * @param debet used to initialize the side combo box
     * @param party used to initialize the party combo box
     */
    private void initDialog(String amount, Account account, boolean debet, Party party)
    {
        JPanel labelsAndFieldsPanel = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints labelConstraints = new GridBagConstraints();
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		labelsAndFieldsPanel.setLayout(gridBag);

		labelConstraints.gridx = 0;
		labelConstraints.anchor = GridBagConstraints.EAST;
		labelConstraints.insets = new Insets( 0, 0, 0, 10 );
		fieldConstraints.gridx = 1;
		fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.anchor = GridBagConstraints.WEST;
        
        WidgetFactory wf = WidgetFactory.getInstance();
        addComponentToGridBag(labelsAndFieldsPanel, wf.createLabel("gen.account"), 
                gridBag, labelConstraints);
        accountCellEditor = new AccountCellEditor(account);
        addComponentToGridBag(labelsAndFieldsPanel, accountCellEditor.getComponent(),
                gridBag, fieldConstraints);
        
        addComponentToGridBag(labelsAndFieldsPanel, wf.createLabel("gen.amount"), 
                gridBag, labelConstraints);
        tfAmount = wf.createTextField(amount);
        addComponentToGridBag(labelsAndFieldsPanel, tfAmount,
                gridBag, fieldConstraints);
        
        addComponentToGridBag(labelsAndFieldsPanel, wf.createLabel("gen.side"), 
                gridBag, labelConstraints);

        cbSide = wf.createComboBox(new String[] { "gen.debet", "gen.credit" });
        cbSide.setSelectedIndex(debet ? 0 : 1);
        addComponentToGridBag(labelsAndFieldsPanel, cbSide,
                gridBag, fieldConstraints);

        addComponentToGridBag(labelsAndFieldsPanel, wf.createLabel("gen.party"), 
                gridBag, labelConstraints);

        partySelector = new PartySelector();
        partySelector.setSelectedParty(party);
        addComponentToGridBag(labelsAndFieldsPanel, partySelector,
                gridBag, fieldConstraints);
        
        componentInitialized(labelsAndFieldsPanel);
    }

    /* (non-Javadoc)
     * @see cf.ui.OkCancelDialog#handleOk()
     */
    protected void handleOk() 
    {
        Amount amount;
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        try 
        {
            amount = af.parse(tfAmount.getText(), Database.getInstance().getCurrency());
        } 
        catch (ParseException e) 
        {
            new MessageDialog(parent, "gen.titleError",
                TextResource.getInstance().getString("ejid.invalidAmount"));            
            return;
        }
        
        Account account = (Account)accountCellEditor.getCellEditorValue();
        boolean debet = cbSide.getSelectedIndex() == 0; 
        Party party = partySelector.getSelectedParty();
        
        enteredJournalItem = new JournalItem(amount, account, debet, party);
        hideDialog();
    }

    /** 
     * Gets the journal item thas has been entered.
     * Its value will be set when the user presses the Ok button and the input fields
     * are correct. Otherwise, this variable will be <code>null</code>.
     */
    public JournalItem getEnteredJournalItem()
    {
        return enteredJournalItem;
    }
    
	/**
	 * Adds a component to a container with a grid bag layout manager.
	 *  
	 * @param container the container
	 * @param comp the component
	 * @param gridBag the grid bag layout manager, which must be the layout manager
	 *                for <tt>container</tt>
	 * @param c the constraints for the component
	 */
	private static void addComponentToGridBag( Container container, Component comp, 
		GridBagLayout gridBag, GridBagConstraints c) 
	{
		gridBag.setConstraints( comp, c );
		container.add(comp);		
	}
    
}
