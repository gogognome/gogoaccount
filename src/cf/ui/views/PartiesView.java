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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import nl.gogognome.lib.gui.beans.DateSelectionBean;
import nl.gogognome.lib.swing.ActionWrapper;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SortedTable;
import nl.gogognome.lib.swing.SortedTableModel;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
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

    private SortedTable table;

    /** The database whose parties are to be shown and changed. */
    private Database database;

    /** The date model that determines the date for the balance of the parties. */
    private DateModel dateModel;

    /** Indicates whether this view should also allow the user to select a party. */
    private boolean selectioEnabled;

    /**
     * Indicates that multiple parties can be selected (<code>true</code>) or at most
     * one party (<code>false</code>).
     */
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

    /** Focus listener used to change the deafult button. */
    private FocusListener focusListener;

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
        return TextResource.getInstance().getString("partiesView.title");
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onInit()
     */
    @Override
    public void onInit() {
        // Create button panel
        WidgetFactory wf = WidgetFactory.getInstance();
        JButton addButton = wf.createButton("partiesView.addParty", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                onAddParty();
            }
        });

        JButton editButton = wf.createButton("partiesView.editParty", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                onEditParty();
            }
        });
        JButton deleteButton = wf.createButton("partiesView.deleteParty", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                onDeleteParty();
            }
        });
        btSelect = wf.createButton("partiesView.selectParty", new AbstractAction() {
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
        WidgetFactory wf = WidgetFactory.getInstance();
        TextResource tr =  TextResource.getInstance();

        // Create the criteria panel
        JPanel criteriaPanel = new JPanel(new GridBagLayout());
        criteriaPanel.setBorder(new TitledBorder(
                tr.getString("partiesView.searchCriteria")));

        focusListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setDefaultButton(btSearch);
            }
        };

        int row = 0;
        tfId = new JTextField();
        tfId.addFocusListener(focusListener);
        criteriaPanel.add(wf.createLabel("partiesView.id", tfId),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfId,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfName = new JTextField();
        tfName.addFocusListener(focusListener);
        criteriaPanel.add(wf.createLabel("partiesView.name", tfName),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfName,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfAddress = new JTextField();
        tfAddress.addFocusListener(focusListener);
        criteriaPanel.add(wf.createLabel("partiesView.address", tfAddress),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfAddress,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfZipCode = new JTextField();
        tfZipCode.addFocusListener(focusListener);
        criteriaPanel.add(wf.createLabel("partiesView.zipCode", tfZipCode),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfZipCode,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfCity = new JTextField();
        tfCity.addFocusListener(focusListener);
        criteriaPanel.add(wf.createLabel("partiesView.city", tfCity),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfCity,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        birthDateModel = new DateModel();
        dsbBirthDate = new DateSelectionBean(birthDateModel);
        dsbBirthDate.addFocusListener(focusListener);
        criteriaPanel.add(wf.createLabel("partiesView.birthDate", dsbBirthDate),
            SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(dsbBirthDate, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        String[] types = database.getPartyTypes();
        if (types.length > 0) {
            String[] typesInclEmptyType = new String[types.length + 1];
            typesInclEmptyType[0] = "";
            System.arraycopy(types, 0, typesInclEmptyType, 1, types.length);
            cmbType = new JComboBox(typesInclEmptyType);
            cmbType.addFocusListener(focusListener);
            criteriaPanel.add(wf.createLabel("partiesView.type", cmbType),
                SwingUtils.createLabelGBConstraints(0, row));
            criteriaPanel.add(cmbType, SwingUtils.createTextFieldGBConstraints(1, row));
            row++;
        }

        JPanel buttonPanel = new JPanel(new FlowLayout());
        ActionWrapper actionWrapper = wf.createAction("partiesView.btnSearch");
        actionWrapper.setAction(new AbstractAction() {
            @Override
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
        resultPanel.setBorder(new CompoundBorder(new TitledBorder(tr.getString("partiesView.foundParties")),
            new EmptyBorder(5, 12, 5, 12)));

        partiesTableModel = new PartiesTableModel();
        table = WidgetFactory.getInstance().createSortedTable(partiesTableModel);
        table.getSelectionModel().setSelectionMode(multiSelectionEnabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);

        table.getComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (selectioEnabled) {
                        onSelectParty();
                    } else {
                        onEditParty();
                    }
                }
            }
        });

        table.getComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (selectioEnabled) {
                        onSelectParty();
                    } else {
                        onEditParty();
                    }
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
			public void valueChanged(ListSelectionEvent e) {
                int[] rows = table.getSelectedRows();
                if (rows.length == 1) {
                    taRemarks.setText(partiesTableModel.getParty(rows[0]).getRemarks());
                } else {
                    taRemarks.setText("");
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table.getComponent());
//        scrollPane.setMinimumSize(new Dimension(500, 100));
        resultPanel.add(scrollPane, BorderLayout.CENTER);

        // Create details panel
        JPanel detailPanel = new JPanel(new GridBagLayout());

        taRemarks = new JTextArea();
        scrollPane = new JScrollPane(taRemarks);
        scrollPane.setPreferredSize(new Dimension(500, 100));

        detailPanel.add(wf.createLabel("partiesView.remarks", taRemarks),
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

        partiesTableModel.setParties(database.getParties(searchCriteria));
        if (partiesTableModel.getRowCount() > 0) {
        	table.getSelectionModel().setSelectionInterval(0, 0);
        }
        table.getFocusableComponent().requestFocusInWindow();

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
                MessageDialog.showMessage(getParentWindow(), "gen.titleWarning",
                    TextResource.getInstance().getString("partiesView.partyAlreadyExists"));
            }
        }
        onSearch();
    }

    /**
     * This method is called when the "edit party" button is pressed.
     */
    private void onEditParty() {
        int rows[] = table.getSelectedRows();
        if (rows.length == 0) {
            return;
        }

        Party oldParty = partiesTableModel.getParty(rows[0]);
        EditPartyView editPartyView = new EditPartyView(database, oldParty);
        ViewDialog dialog = new ViewDialog(getParentWindow(), editPartyView);
        dialog.showDialog();

        Party party = editPartyView.getEnteredParty();
        if (party != null) {
            try {
                database.updateParty(oldParty, party);
            } catch (DatabaseModificationFailedException e) {
                MessageDialog.showMessage(getParentWindow(), "gen.titleWarning",
                    TextResource.getInstance().getString("partiesView.partyAlreadyExists"));
            }
        }
        onSearch();
        // Select the row that has been edited.
        table.getSelectionModel().setSelectionInterval(rows[0], rows[0]);
    }

    /**
     * This method is called when the "delete party" button is pressed.
     */
    private void onDeleteParty() {
        int rows[] = table.getSelectedRows();
        if (rows.length == 0) {
            return;
        }

        Party party = partiesTableModel.getParty(rows[0]);
        MessageDialog messageDialog = MessageDialog.showMessage(this, "gen.warning",
            TextResource.getInstance().getString("partiesView.areYouSurePartyIsDeleted", party.getName()),
            new String[] { "gen.yes", "gen.no" });
        if (messageDialog.getSelectedButton() == 0) {
            try {
                database.removeParty(party);
            } catch (DatabaseModificationFailedException e) {
                MessageDialog.showMessage(getParentWindow(), "gen.titleWarning",
                    TextResource.getInstance().getString("partiesView.partyCouldNotBeDeleted"));
            }
        }
        onSearch();
    }

    /**
     * This method is called when the "select party" button is pressed.
     */
    private void onSelectParty() {
        int rows[] = table.getSelectedRows();
        selectedParties = new Party[rows.length];
        for (int i = 0; i < rows.length; i++) {
            selectedParties[i] = partiesTableModel.getParty(rows[i]);
        }
        closeAction.actionPerformed(null);
    }

    /**
     * Gets the parties that were selected by the user.
     * @return the parties or <code>null</code> if no party has been selected
     */
    public Party[] getSelectedParties() {
        return selectedParties;
    }

    /** The table model that shows information about the parties. */
    private static class PartiesTableModel extends AbstractTableModel implements SortedTableModel {

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

        @Override
		public Class<?> getColumnClass(int columnIndex) {
            switch(columnIndex) {
            case 5: return Date.class;
            default: return String.class;
            }
        }

        @Override
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
        @Override
		public int getColumnCount() {
            return 8;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        @Override
		public int getRowCount() {
            return parties.length;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        @Override
		public Object getValueAt(int row, int col) {
            switch(col) {
            case 0: return parties[row].getId();
            case 1: return parties[row].getName();
            case 2: return parties[row].getAddress();
            case 3: return parties[row].getZipCode();
            case 4: return parties[row].getCity();
            case 5: return parties[row].getBirthDate();
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

        @Override
		public int getColumnWidth(int column) {
            switch (column) {
            case 0: return 40;
            case 1:
            case 2: return 200;
            case 3: return 80;
            default: return 100;
            }
        }

        @Override
		public Comparator<Object> getComparator(int column) {
            switch (column) {
            case 5:
                return new DateComparator();
            }
            return null;
        }

        @Override
		public TableCellRenderer getRendererForColumn(int column) {
            return null;
        }

        @Override
        public TableCellEditor getEditorForColumn(int column) {
        	return null;
        }

        private static class DateComparator implements Comparator<Object> {
            @Override
			public int compare(Object o1, Object o2) {
                Date d1 = (Date) o1;
                Date d2 = (Date) o2;
                return DateUtil.compareDayOfYear(d1, d2);
            }

        }
    }
}
