/*
 * $Id: PartiesView.java,v 1.8 2007-10-15 19:33:48 sanderk Exp $
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

import nl.gogognome.beans.DateSelectionBean;
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
    private DateModel birthDateModel;
    
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
        
        JButton editButton = wf.createButton("partiesView.editParty", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                onEditParty();
            }
        });
        JButton deleteButton = wf.createButton("partiesView.deleteParty", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                onDeleteParty();
            }
        });
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        
        setLayout(new GridBagLayout());
        add(createPanel(), SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 1.0, 
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 12, 12, 12, 12));
        add(buttonPanel, SwingUtils.createGBConstraints(1, 0, 1, 1, 0.0, 0.0, 
            GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 12, 12, 12, 12));
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
	   
        int row = 0;
        tfId = new JTextField();
        criteriaPanel.add(wf.createLabel("partiesView.id"), 
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfId, 
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        tfName = new JTextField();
        criteriaPanel.add(wf.createLabel("partiesView.name"), 
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfName, 
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        tfAddress = new JTextField();
        criteriaPanel.add(wf.createLabel("partiesView.address"), 
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfAddress, 
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        tfZipCode = new JTextField();
        criteriaPanel.add(wf.createLabel("partiesView.zipCode"), 
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfZipCode, 
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        tfCity = new JTextField();
        criteriaPanel.add(wf.createLabel("partiesView.city"), 
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfCity, 
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;
        
        criteriaPanel.add(wf.createLabel("partiesView.birthDate"),
            SwingUtils.createLabelGBConstraints(0, row));
        birthDateModel = new DateModel();
        DateSelectionBean dsbBirthDate = new DateSelectionBean(birthDateModel);
        criteriaPanel.add(dsbBirthDate, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

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
                SwingUtils.createGBConstraints(0, row, 2, 1, 0.0, 0.0, 
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
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        
        resultPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create a panel containing the search criteria and result panels
        JPanel result = new JPanel(new GridBagLayout());
        result.add(criteriaPanel,
                SwingUtils.createTextFieldGBConstraints(0, 0));
        result.add(resultPanel,
                SwingUtils.createPanelGBConstraints(0, 1));
        
        return result;
    }

    /**
     * @see nl.gogognome.framework.View#onClose()
     */
    public void onClose() {
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
        if (birthDateModel.getDate() != null) {
            searchCriteria.setBirthDate(birthDateModel.getDate());
        }
        
        partiesTableModel.setParties(database.getParties(searchCriteria));
        table.getSelectionModel().setSelectionInterval(0, 0);
        table.requestFocusInWindow();
    }
    
    /**
     * This method is called when the "add party" button is pressed.
     */
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
        onSearch();
    }

    /**
     * This method is called when the "edit party" button is pressed.
     */
    private void onEditParty() {
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        
        Party oldParty = partiesTableModel.getParty(row);
        EditPartyView editPartyView = new EditPartyView(oldParty);
        ViewDialog dialog = new ViewDialog(getParentFrame(), editPartyView);
        dialog.showDialog();
        
        Party party = editPartyView.getEnteredParty();
        if (party != null) {
            try {
                database.updateParty(oldParty, party);
            } catch (IllegalArgumentException e) {
                MessageDialog.showMessage(getParentFrame(), "gen.titleWarning", 
                    TextResource.getInstance().getString("partiesView.partyAlreadyExists"));
            }
        }
        onSearch();
    }

    /**
     * This method is called when the "delete party" button is pressed.
     */
    private void onDeleteParty() {
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        
        Party party = partiesTableModel.getParty(row);
        MessageDialog messageDialog = MessageDialog.showMessage(this, "gen.warning", 
            TextResource.getInstance().getString("partiesView.areYouSurePartyIsDeleted", party.getName()), 
            new String[] { "gen.yes", "gen.no" });
        if (messageDialog.getSelectedButton() == 0) {
            database.removeParty(party);
        }
        onSearch();
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
        
        /**
         * Gets the party for the specified row.
         * @param row the row
         */
        public Party getParty(int row) {
            return parties[row];
        }
        
        public String getColumnName(int columnIndex) {
            String id; 
            switch(columnIndex) {
            case 0: id = "gen.id"; break;
            case 1: id = "gen.name"; break;
            case 2: id = "gen.address"; break;
            case 3: id = "gen.zipCode"; break;
            case 4: id = "gen.city"; break;
            case 5: id = "gen.birthDate"; break;
            case 6: id = "gen.type"; break;
            case 7: id = "gen.remarks"; break;
            default: 
                id = null;
            }
            return TextResource.getInstance().getString(id);
        }
        
        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public int getColumnCount() {
            return 8;
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
            case 5: Date birthDate = parties[row].getBirthDate();
                if (birthDate != null) {
                    return TextResource.getInstance().formatDate("gen.dateFormat", birthDate);
                } else {
                    return null;
                }
            case 6: return parties[row].getType();
            case 7:
                String remarks = parties[row].getRemarks();
                if (remarks != null && remarks.length() > 30) {
                    int size = Math.max(20, remarks.lastIndexOf(' ', 30));
                    remarks = remarks.substring(0, size) + "...";
                }
                return remarks;
            default: return null;
            }
        }
        
    }

    /**
     * Gets the default button of this view.
     * @return the default button of this view
     */
    public JButton getDefaultButton() {
        return btSearch;
    }
}
