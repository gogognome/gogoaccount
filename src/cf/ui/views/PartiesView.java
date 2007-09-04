/*
 * $Id: PartiesView.java,v 1.6 2007-09-04 19:04:03 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import nl.gogognome.framework.View;
import nl.gogognome.framework.ViewDialog;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.ActionWrapper;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.engine.Party;
import cf.engine.PartySearchCriteria;

/**
 * This class implements a view for adding, removing and editing parties.
 *
 * @author Sander Kooijmans
 */
public class PartiesView extends View {

    private PartiesTableModel partiesTableModel;
    
    private JTable table;
    
    /** The database whose parties are to be shown and changed. */
    private Database database;
    
    /** The date model that determines the date for the balance of the parties. */  
    private DateModel dateModel;

    private JTextField tfId;
    private JTextField tfName;
    private JTextField tfAddress;
    private JTextField tfZipCode;
    private JTextField tfCity;

    private JButton btSearch;
    
    public PartiesView(Database database) {
        this.database = database;
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
    }
    
    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#getTitle()
     */
    public String getTitle() {
        return TextResource.getInstance().getString("partiesView.title");
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onInit()
     */
    public void onInit() {
        // Create button panel
        WidgetFactory wf = WidgetFactory.getInstance();
        JButton addButton = wf.createButton("partiesView.addParty", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                onAddParty();
            }
        });
        
        JButton editButton = wf.createButton("partiesView.editParty", null);
        JButton deleteButton = wf.createButton("partiesView.deleteParty", null);
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        
        add(createPanel(), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
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
                tr.getString("partiesView.searchCriteria"))); 
	   
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
        ActionWrapper actionWrapper = wf.createAction("partiesView.btnSearch");
        actionWrapper.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                onSearch();
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
                tr.getString("partiesView.foundParties"))); 

        partiesTableModel = new PartiesTableModel();
        table = new JTable(partiesTableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        
        resultPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create a panel containing the search criteria and result panels
        JPanel result = new JPanel(new GridBagLayout());
        result.add(criteriaPanel,
                SwingUtils.createTextFieldGBConstraints(0, 0));
        result.add(resultPanel,
                SwingUtils.createTextFieldGBConstraints(0, 1));
        
        return result;
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onClose()
     */
    public void onClose() {
        // TODO Auto-generated method stub
        
    }

    /**
     * Searches for matching parties. The entered search criteria are used 
     * to find parties. The matching parties are shown in the table. 
     */
    private void onSearch() {
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

        partiesTableModel.setParties(database.getParties(searchCriteria));
        table.getSelectionModel().setSelectionInterval(0, 0);
        table.requestFocusInWindow();
    }
    
    private void onAddParty() {
        EditPartyView editPartyView = new EditPartyView(null);
        ViewDialog dialog = new ViewDialog(getParentFrame(), editPartyView);
        dialog.showDialog();
        
        Party party = editPartyView.getEnteredParty();
        if (party != null) {
            try {
                database.addParty(party);
            } catch (IllegalArgumentException e) {
                MessageDialog.showMessage(getParentFrame(), "gen.titleWarning", 
                    TextResource.getInstance().getString("partiesView.partyAlreadyExists"));
            }
        }
    }
    
    /** The table model that shows information about the parties. */
    private static class PartiesTableModel extends AbstractTableModel {

        /** The parties to be shown. */
        private Party[] parties = new Party[0];

        /**
         * Sets the parties to be shown in the table.
         * @param parties the parties
         */
        public void setParties(Party[] parties) {
            this.parties = parties;
            fireTableDataChanged();
        }
        
        public String getColumnName(int columnIndex) {
            String id; 
            switch(columnIndex) {
            case 0: id = "gen.id"; break;
            case 1: id = "gen.name"; break;
            case 2: id = "gen.address"; break;
            case 3: id = "gen.zipCode"; break;
            case 4: id = "gen.city"; break;
            default: 
                id = null;
            }
            return TextResource.getInstance().getString(id);
        }
        
        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public int getColumnCount() {
            return 5;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            return parties.length;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        public Object getValueAt(int row, int col) {
            switch(col) {
            case 0: return parties[row].getId();
            case 1: return parties[row].getName();
            case 2: return parties[row].getAddress();
            case 3: return parties[row].getZipCode();
            case 4: return parties[row].getCity();
            default: return null;
            }
        }
        
    }
}
