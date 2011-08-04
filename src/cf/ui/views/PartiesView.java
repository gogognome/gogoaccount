/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import nl.gogognome.lib.gui.beans.DateSelectionBean;
import nl.gogognome.lib.swing.ActionWrapper;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.TableRowSelectAction;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import cf.engine.Database;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Party;
import cf.engine.PartySearchCriteria;

/**
 * This class implements a view for adding, removing, editing and (optionally) selecting parties.
 *
 * @author Sander Kooijmans
 */
public class PartiesView extends View {

    private PartiesTableModel partiesTableModel;

    private JTable table;
    private ListSelectionListener listSelectionListener;

    private Database database;

    private DateModel dateModel;

    private boolean selectioEnabled;

    private boolean multiSelectionEnabled;

    /** The parties selected by the user or <code>null</code> if no party has been selected. */
    private Party[] selectedParties;

    private JTextField tfId;
    private JTextField tfName;
    private JTextField tfAddress;
    private JTextField tfZipCode;
    private JTextField tfCity;
    private JComboBox cmbType;
    private DateSelectionBean dsbBirthDate;

    private DateModel birthDateModel;

    /** Text area that shows the description in the result details. */
    private JTextArea taRemarks;

    private JButton btSearch;
    private JButton btSelect;

    /** Focus listener used to change the default button. */
    private FocusListener focusListener;

    /**
     * Constructor for the parties view. Parties can be edited. There is no select
     * party button.
     * @param database
     */
    public PartiesView(Database database) {
        this(database, false);
    }

    /**
     * Constructor for a parties view in which at most one party can be selected.
     * @param database the database used to search for parties and to add, delete or update parties from.
     * @param selectioEnabled <code>true</code> if the user should be able to select a party;
     *         <code>false</code> if the user cannot select a party
     */
    public PartiesView(Database database, boolean selectionEnabled) {
        this(database, selectionEnabled, false);
    }

    /**
     * Constructor.
     * @param database the database used to search for parties and to add, delete or update parties from.
     * @param selectioEnabled <code>true</code> if the user should be able to select a party;
     *         <code>false</code> if the user cannot select a party
     * @param multiSelectionEnabled indicates that multiple parties can be selected (<code>true</code>) or
     *         at most one party (<code>false</code>)
     */
    public PartiesView(Database database, boolean selectionEnabled, boolean multiSelectionEnabled) {
        this.database = database;
        this.selectioEnabled = selectionEnabled;
        this.multiSelectionEnabled = multiSelectionEnabled;
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#getTitle()
     */
    @Override
    public String getTitle() {
        return textResource.getString("partiesView.title");
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onInit()
     */
    @Override
    public void onInit() {
        // Create button panel
        JButton addButton = widgetFactory.createButton("partiesView.addParty", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                onAddParty();
            }
        });

        JButton editButton = widgetFactory.createButton("partiesView.editParty", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                onEditParty();
            }
        });
        JButton deleteButton = widgetFactory.createButton("partiesView.deleteParty", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                onDeleteParty();
            }
        });
        btSelect = widgetFactory.createButton("partiesView.selectParty", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                onSelectParty();
            }
        });

        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 0, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        if (selectioEnabled) {
            buttonPanel.add(new JLabel());
            buttonPanel.add(btSelect);
        }

        setLayout(new GridBagLayout());
        add(createPanel(), SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 12, 12, 12, 12));
        add(buttonPanel, SwingUtils.createGBConstraints(1, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 12, 12, 12, 12));

        setDefaultButton(btSearch);
    }

    /**
     * Creates the panel with search criteria and the table with
     * found parties.
     *
     * @return the panel
     */
    private JPanel createPanel() {
        // Create the criteria panel
        JPanel criteriaPanel = new JPanel(new GridBagLayout());
        criteriaPanel.setBorder(new TitledBorder(
                textResource.getString("partiesView.searchCriteria")));

        focusListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setDefaultButton(btSearch);
            }
        };

        int row = 0;
        tfId = new JTextField();
        tfId.addFocusListener(focusListener);
        criteriaPanel.add(widgetFactory.createLabel("partiesView.id", tfId),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfId,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfName = new JTextField();
        tfName.addFocusListener(focusListener);
        criteriaPanel.add(widgetFactory.createLabel("partiesView.name", tfName),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfName,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfAddress = new JTextField();
        tfAddress.addFocusListener(focusListener);
        criteriaPanel.add(widgetFactory.createLabel("partiesView.address", tfAddress),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfAddress,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfZipCode = new JTextField();
        tfZipCode.addFocusListener(focusListener);
        criteriaPanel.add(widgetFactory.createLabel("partiesView.zipCode", tfZipCode),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfZipCode,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfCity = new JTextField();
        tfCity.addFocusListener(focusListener);
        criteriaPanel.add(widgetFactory.createLabel("partiesView.city", tfCity),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfCity,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        birthDateModel = new DateModel();
        dsbBirthDate = beanFactory.createDateSelectionBean(birthDateModel);
        dsbBirthDate.addFocusListener(focusListener);
        criteriaPanel.add(widgetFactory.createLabel("partiesView.birthDate", dsbBirthDate),
            SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(dsbBirthDate, SwingUtils.createLabelGBConstraints(1, row));
        row++;

        String[] types = database.getPartyTypes();
        if (types.length > 0) {
            String[] typesInclEmptyType = new String[types.length + 1];
            typesInclEmptyType[0] = "";
            System.arraycopy(types, 0, typesInclEmptyType, 1, types.length);
            cmbType = new JComboBox(typesInclEmptyType);
            cmbType.addFocusListener(focusListener);
            criteriaPanel.add(widgetFactory.createLabel("partiesView.type", cmbType),
                SwingUtils.createLabelGBConstraints(0, row));
            criteriaPanel.add(cmbType, SwingUtils.createTextFieldGBConstraints(1, row));
            row++;
        }

        JPanel buttonPanel = new JPanel(new FlowLayout());
        ActionWrapper actionWrapper = widgetFactory.createAction("partiesView.btnSearch");
        actionWrapper.setAction(new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent event) {
                onSearch();
            }
        });
        btSearch = new JButton(actionWrapper);

        buttonPanel.add(btSearch);
        criteriaPanel.add(buttonPanel, SwingUtils.createGBConstraints(0, row, 2, 1, 0.0, 0.0,
        		GridBagConstraints.EAST, GridBagConstraints.NONE, 5, 0, 0, 0));

        // Create the result panel
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(new CompoundBorder(new TitledBorder(textResource.getString("partiesView.foundParties")),
            new EmptyBorder(5, 12, 5, 12)));

        partiesTableModel = new PartiesTableModel(Collections.<Party>emptyList());
        table = widgetFactory.createSortedTable(partiesTableModel);
        table.getSelectionModel().setSelectionMode(multiSelectionEnabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);

        TableRowSelectAction trsa = new TableRowSelectAction(table, new SelectionActionImpl());
        addCloseable(trsa);
        trsa.registerListeners();

        listSelectionListener = new ListSelectionListener() {
            @Override
			public void valueChanged(ListSelectionEvent e) {
                int row = SwingUtils.getSelectedRowConvertedToModel(table);
                if (row != -1) {
                    taRemarks.setText(partiesTableModel.getRow(row).getRemarks());
                } else {
                    taRemarks.setText("");
                }
            }
        };
        table.getSelectionModel().addListSelectionListener(listSelectionListener);
        resultPanel.add(widgetFactory.createScrollPane(table), BorderLayout.CENTER);

        // Create details panel
        JPanel detailPanel = new JPanel(new GridBagLayout());

        taRemarks = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(taRemarks);
        scrollPane.setPreferredSize(new Dimension(500, 100));

        detailPanel.add(widgetFactory.createLabel("partiesView.remarks", taRemarks),
            SwingUtils.createGBConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 12, 0, 0, 12));
        detailPanel.add(scrollPane, SwingUtils.createGBConstraints(1, 0, 1, 1, 1.0, 1.0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, 12, 0, 12, 12));

        resultPanel.add(detailPanel, BorderLayout.SOUTH);

        // Create a panel containing the search criteria and result panels
        JPanel result = new JPanel(new BorderLayout());
        result.add(criteriaPanel, BorderLayout.NORTH);
        result.add(resultPanel, BorderLayout.CENTER);

        return result;
    }

    /**
     * @see nl.gogognome.lib.swing.views.View#onClose()
     */
    @Override
    public void onClose() {
    	table.getSelectionModel().removeListSelectionListener(listSelectionListener);

        tfId.removeFocusListener(focusListener);
        tfName.removeFocusListener(focusListener);
        tfAddress.removeFocusListener(focusListener);
        tfZipCode.removeFocusListener(focusListener);
        tfCity.removeFocusListener(focusListener);
        dsbBirthDate.removeFocusListener(focusListener);
        if (cmbType != null) {
            cmbType.removeFocusListener(focusListener);
        }
        focusListener = null;
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
        if (cmbType != null && cmbType.getSelectedIndex() > 0) {
            searchCriteria.setType((String)cmbType.getSelectedItem());
        }

        partiesTableModel.replaceRows(Arrays.asList(database.getParties(searchCriteria)));
        SwingUtils.selectFirstRow(table);
        table.requestFocusInWindow();

        // Update the default button if the select button is present
        if (btSelect != null) {
            setDefaultButton(btSelect);
        }
    }

    /**
     * This method is called when the "add party" button is pressed.
     */
    private void onAddParty() {
        EditPartyView editPartyView = new EditPartyView(database, null);
        ViewDialog dialog = new ViewDialog(getParentWindow(), editPartyView);
        dialog.showDialog();

        Party party = editPartyView.getEnteredParty();
        if (party != null) {
            try {
                database.addParty(party);
            } catch (DatabaseModificationFailedException e) {
                MessageDialog.showErrorMessage(this, "partiesView.partyAlreadyExists");
            }
        }
        onSearch();
    }

    /**
     * This method is called when the "edit party" button is pressed.
     */
    private void onEditParty() {
        int row = SwingUtils.getSelectedRowConvertedToModel(table);
        if (row == -1) {
            return;
        }

        Party oldParty = partiesTableModel.getRow(row);
        EditPartyView editPartyView = new EditPartyView(database, oldParty);
        ViewDialog dialog = new ViewDialog(getParentWindow(), editPartyView);
        dialog.showDialog();

        Party party = editPartyView.getEnteredParty();
        if (party != null) {
            try {
                database.updateParty(oldParty, party);
            } catch (DatabaseModificationFailedException e) {
                MessageDialog.showErrorMessage(this, e, "partiesView.partyAlreadyExists");
            }
        }
        onSearch();

        SwingUtils.selectRowWithModelIndex(table, row);
    }

    /**
     * This method is called when the "delete party" button is pressed.
     */
    private void onDeleteParty() {
        int row = SwingUtils.getSelectedRowConvertedToModel(table);
        if (row == -1) {
            return;
        }

        Party party = partiesTableModel.getRow(row);
        int choice = MessageDialog.showYesNoQuestion(this, "gen.warning",
            "partiesView.areYouSurePartyIsDeleted", party.getName());
        if (choice == MessageDialog.YES_OPTION) {
            try {
                database.removeParty(party);
            } catch (DatabaseModificationFailedException e) {
                MessageDialog.showErrorMessage(this, e, "partiesView.partyCouldNotBeDeleted");
            }
        }
        onSearch();
    }

    /**
     * This method is called when the "select party" button is pressed.
     */
    private void onSelectParty() {
        int rows[] = SwingUtils.getSelectedRowsConvertedToModel(table);
        selectedParties = new Party[rows.length];
        for (int i = 0; i < rows.length; i++) {
            selectedParties[i] = partiesTableModel.getRow(rows[i]);
        }
        requestClose();
    }

    /**
     * Gets the parties that were selected by the user.
     * @return the parties or <code>null</code> if no party has been selected
     */
    public Party[] getSelectedParties() {
        return selectedParties;
    }

    private class SelectionActionImpl extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent arg0) {
            if (selectioEnabled) {
                onSelectParty();
            } else {
                onEditParty();
            }
		}
    }
}
