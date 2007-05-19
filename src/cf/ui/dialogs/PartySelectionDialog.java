/*
 * $Id: PartySelectionDialog.java,v 1.6 2007-05-19 17:33:31 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import cf.engine.Database;
import cf.engine.Party;
import cf.engine.PartySearchCriteria;

import nl.gogognome.swing.ActionWrapper;
import nl.gogognome.swing.DialogWithButtons;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;

/**
 * 
 *
 * @author Sander Kooijmans
 */
public class PartySelectionDialog extends DialogWithButtons {

    public final static int SELECTION_MODE = 0;
    public final static int EDIT_MODE = 1;

    /** The mode of this dialog. */
    private int mode;
    
    private final static String[] TITLE_IDS = new String[] {
            "psd.titleSelectionMode", "psd.titleEditMode"
    };
    
    private final static String[][] BUTTON_IDS = new String[][] {
            { "psd.btnSelect", "gen.cancel" }, { "psd.btnDone" }
    };
    
    private JTextField tfId;
    private JTextField tfName;
    private JTextField tfAddress;
    private JTextField tfZipCode;
    private JTextField tfCity;
    
    private JTable table;
    private PartyTableModel tableModel;
    private JButton btSearch;

    private Party selectedParty;
    
    private JButton defaultButton;
    
    /**
     * Constructor.
     * 
     * @param frame the parent of this dialog
     * @param mode the mode of this dialog
     */
    public PartySelectionDialog(Frame frame, int mode) {
        this(frame, TITLE_IDS[mode], mode);
    }
    
    /**
     * Constructor.
     * 
     * @param frame the parent of this dialog
     * @param mode the mode of this dialog
     */
    public PartySelectionDialog(Frame frame, String titleId, int mode) {
        super(frame, titleId, BUTTON_IDS[mode]);
        this.mode = mode;
        
        componentInitialized(createPanel());
        defaultButton = getDefaultButton();
        setDefaultButton(btSearch);
    }
    
    /**
     * Gets the party that is selected by the user.
     * @return the party that is selected by the user
     */
    public Party getSelectedParty() {
        return selectedParty;
    }
    
	/**
	 * Handles the button-pressed event. This method is called when one of the buttons
	 * has been pressed by the user.
	 * @param index the index of the button (as specified by the <tt>buttonIds</tt>
	 *        passed to the constructor. 
	 */
	protected void handleButton(int index) {
	    switch(mode) {
	    case SELECTION_MODE:
	        if (index == 0) {
	            int row = table.getSelectedRow();
	            if (row != -1 && row < tableModel.getParties().length) {
	                selectedParty = tableModel.getParties()[row];
		    		hideDialog(); 
	            }
	        } else {
	    		hideDialog(); 
	        }
	        break;
	        
	    case EDIT_MODE:
	        break;
	    }
	}
    
    /**
     * Creates the panel with search criteria and the table with
     * found parties.
     * 
     * @return the panel
     */
    private JPanel createPanel() {
        WidgetFactory wf = WidgetFactory.getInstance();
        TextResource tr =  TextResource.getInstance();
        
        // Create the criteria panel
        JPanel criteriaPanel = new JPanel(new GridBagLayout());
        criteriaPanel.setBorder(new TitledBorder(
                tr.getString("psd.searchCriteria"))); 
	   
        tfId = new JTextField();
        criteriaPanel.add(wf.createLabel("gen.id"), 
                SwingUtils.createLabelGBConstraints(0, 0));
        criteriaPanel.add(tfId, 
                SwingUtils.createTextFieldGBConstraints(1, 0));
        
        tfName = new JTextField();
        criteriaPanel.add(wf.createLabel("gen.name"), 
                SwingUtils.createLabelGBConstraints(0, 1));
        criteriaPanel.add(tfName, 
                SwingUtils.createTextFieldGBConstraints(1, 1));
        
        tfAddress = new JTextField();
        criteriaPanel.add(wf.createLabel("gen.address"), 
                SwingUtils.createLabelGBConstraints(0, 2));
        criteriaPanel.add(tfAddress, 
                SwingUtils.createTextFieldGBConstraints(1, 2));
        
        tfZipCode = new JTextField();
        criteriaPanel.add(wf.createLabel("gen.zipCode"), 
                SwingUtils.createLabelGBConstraints(0, 3));
        criteriaPanel.add(tfZipCode, 
                SwingUtils.createTextFieldGBConstraints(1, 3));
        
        tfCity = new JTextField();
        criteriaPanel.add(wf.createLabel("gen.city"), 
                SwingUtils.createLabelGBConstraints(0, 4));
        criteriaPanel.add(tfCity, 
                SwingUtils.createTextFieldGBConstraints(1, 4));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        ActionWrapper actionWrapper = wf.createAction("psd.btnSearch");
        actionWrapper.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                handleSearch();
            }
        });
        btSearch = new JButton(actionWrapper);
        
        buttonPanel.add(btSearch);
        criteriaPanel.add(buttonPanel,
                SwingUtils.createGBConstraints(0, 5, 2, 1, 0.0, 0.0, 
                        GridBagConstraints.EAST, GridBagConstraints.NONE, 
                        5, 0, 0, 0));
        
        // Create the result panel
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(new TitledBorder(
                tr.getString("psd.foundParties"))); 

        tableModel = new PartyTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollableTable = new JScrollPane(table);
        
        resultPanel.add(scrollableTable, BorderLayout.CENTER);
        
        // Create a panel containing the search criteria and result panels
        JPanel result = new JPanel(new GridBagLayout());
        result.add(criteriaPanel,
                SwingUtils.createTextFieldGBConstraints(0, 0));
        result.add(resultPanel,
                SwingUtils.createTextFieldGBConstraints(0, 1));
        
        return result;
    }
    
    /**
     * Searches for matching parties. The entered search criteria are used 
     * to find parties. The matching parties are shown in the table. 
     */
    private void handleSearch() {
        PartySearchCriteria searchCriteria = new PartySearchCriteria();

        if (tfId.getText().length() > 0) {
            searchCriteria.setId(tfId.getText());
        }
        if (tfName.getText().length() > 0) {
            searchCriteria.setName(tfName.getText());
        }
        if (tfAddress.getText().length() > 0) {
            searchCriteria.setAddress(tfAddress.getText());
        }
        if (tfZipCode.getText().length() > 0) {
            searchCriteria.setZipCode(tfZipCode.getText());
        }
        if (tfCity.getText().length() > 0) {
            searchCriteria.setCity(tfCity.getText());
        }

        tableModel.setParties(
                Database.getInstance().getParties(searchCriteria));
        table.getSelectionModel().setSelectionInterval(0, 0);
        setDefaultButton(defaultButton);
        table.requestFocusInWindow();
    }
    
    private static class PartyTableModel extends AbstractTableModel {

        /** The parties to be shown in the table. */
        private Party[] parties = new Party[0];
        
        /**
         * Sets the parties to be shwon in the table.
         * @param parties the parties
         */
        public synchronized void setParties(Party[] parties) {
            this.parties = parties;
            fireTableStructureChanged();
        }
        
        /**
         * Gets the parties that are shown in the table.
         * @return the parties
         */
        public synchronized Party[] getParties() {
            return parties;
        }
        
        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnName(int)
         */
        public synchronized String getColumnName(int col) 
        {
            String id = null;
            switch(col) {
	            case 0: id = "gen.id"; break;
	            case 1: id = "gen.name"; break;
	            case 2: id = "gen.address"; break;
	            case 3: id = "gen.zipCode"; break;
	            case 4: id = "gen.city"; break;
            }
            return TextResource.getInstance().getString(id);
        }
        
        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public synchronized int getColumnCount() {
            return 5;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public synchronized int getRowCount() {
            return parties.length;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        public synchronized Object getValueAt(int row, int col) {
            String result = null;
            Party party = parties[row];
            switch(col) {
	            case 0: result = party.getId(); break;
	            case 1: result = party.getName(); break;
	            case 2: result = party.getAddress(); break;
	            case 3: result = party.getZipCode(); break;
	            case 4: result = party.getCity(); break;
            }
            return result;
        }
        
    }
}
