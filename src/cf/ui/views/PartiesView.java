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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ActionWrapper;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.TableRowSelectAction;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.StringUtil;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Party;
import cf.engine.PartySearchCriteria;

/**
 * This class implements a view for adding, removing, editing and (optionally) selecting parties.
 *
 * @author Sander Kooijmans
 */
public class PartiesView extends View {

	private static final long serialVersionUID = 1L;

    private Database database;

    private JTable table;
	private PartiesTableModel partiesTableModel;

    private boolean selectioEnabled;
    private boolean multiSelectionEnabled;

    private StringModel idModel = new StringModel();
    private StringModel nameModel = new StringModel();
    private StringModel addressModel = new StringModel();
    private StringModel zipCodeModel = new StringModel();
    private StringModel cityModel = new StringModel();
    private ListModel<String> typeListModel = new ListModel<String>();
    private DateModel birthDateModel = new DateModel();

    private JTextArea taRemarks;

    private JButton btSearch;
    private JButton btSelect;

    private DatabaseListener databaseListener;
    private ListSelectionListener listSelectionListener;
    private FocusListener focusListener;

    private Party[] selectedParties;

    private InputFieldsColumn ifc;

    /**
     * Constructor for the parties view. Parties can be edited. There is no select
     * party button.
     * @param database
     */
    public PartiesView(Database database) {
        this.database = database;
    }

    public void setSelectioEnabled(boolean selectioEnabled) {
		this.selectioEnabled = selectioEnabled;
	}

    public void setMultiSelectionEnabled(boolean multiSelectionEnabled) {
		this.multiSelectionEnabled = multiSelectionEnabled;
	}

    @Override
    public String getTitle() {
        return textResource.getString("partiesView.title");
    }

    @Override
    public void onInit() {
    	initModels();
    	addComponents();
    	addListeners();
    }

    private void initModels() {
    	updateTypeListModel();
    }

    private void addComponents() {
        JButton addButton = widgetFactory.createButton("partiesView.addParty", new AddPartyAction());

        JButton editButton = widgetFactory.createButton("partiesView.editParty", new EditPartyAction());
        JButton deleteButton = widgetFactory.createButton("partiesView.deleteParty", new DeletePartyAction());
        btSelect = widgetFactory.createButton("partiesView.selectParty", new SelectActionParty());

        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 0, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        if (selectioEnabled) {
            buttonPanel.add(new JLabel());
            buttonPanel.add(btSelect);
        }

        setLayout(new GridBagLayout());
        add(createSearchCriteriaAndResultsPanel(), SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 12, 12, 12, 12));
        add(buttonPanel, SwingUtils.createGBConstraints(1, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 12, 12, 12, 12));

        setDefaultButton(btSearch);
    }

    private JPanel createSearchCriteriaAndResultsPanel() {
        JPanel result = new JPanel(new BorderLayout());
        result.add(createSearchCriteriaPanel(), BorderLayout.NORTH);
        result.add(createSearchResultPanel(), BorderLayout.CENTER);

        return result;
    }

    private JPanel createSearchCriteriaPanel() {
        ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("partiesView.searchCriteria"));

        ifc.addField("partiesView.id", idModel);
        ifc.addField("partiesView.name", nameModel);
        ifc.addField("partiesView.address", addressModel);
        ifc.addField("partiesView.zipCode", zipCodeModel);
        ifc.addField("partiesView.city", cityModel);
        ifc.addField("partiesView.birthDate", birthDateModel);
        ifc.addComboBoxField("partiesView.type", typeListModel, null);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        ActionWrapper actionWrapper = widgetFactory.createAction("partiesView.btnSearch");
        actionWrapper.setAction(new SearchAction());
        btSearch = new JButton(actionWrapper);

        buttonPanel.add(btSearch);
        ifc.add(buttonPanel, SwingUtils.createGBConstraints(0, 10, 2, 1, 0.0, 0.0,
        		GridBagConstraints.EAST, GridBagConstraints.NONE, 5, 0, 0, 0));
        return ifc;
    }

    private JPanel createSearchResultPanel() {
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(new CompoundBorder(new TitledBorder(textResource.getString("partiesView.foundParties")),
            new EmptyBorder(5, 12, 5, 12)));

        partiesTableModel = new PartiesTableModel(Collections.<Party>emptyList());
        table = widgetFactory.createSortedTable(partiesTableModel);
        table.getSelectionModel().setSelectionMode(multiSelectionEnabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);

        TableRowSelectAction trsa = new TableRowSelectAction(table, new SelectionActionImpl());
        addCloseable(trsa);
        trsa.registerListeners();

        resultPanel.add(widgetFactory.createScrollPane(table), BorderLayout.CENTER);
        resultPanel.add(createDetailPanel(), BorderLayout.SOUTH);

        return resultPanel;
    }

    private Component createDetailPanel() {
        JPanel detailPanel = new JPanel(new GridBagLayout());

        taRemarks = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(taRemarks);
        scrollPane.setPreferredSize(new Dimension(500, 100));

        detailPanel.add(widgetFactory.createLabel("partiesView.remarks", taRemarks),
            SwingUtils.createGBConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 12, 0, 0, 12));
        detailPanel.add(scrollPane, SwingUtils.createGBConstraints(1, 0, 1, 1, 1.0, 1.0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, 12, 0, 12, 12));

		return detailPanel;
	}

    private void updateTypeListModel() {
    	List<String> items = new ArrayList<String>();
    	items.add("\u00a0");
    	items.addAll(Arrays.asList(database.getPartyTypes()));
    	typeListModel.setItems(items);
    }

    private void addListeners() {
    	databaseListener = new DatabaseListenerImpl();
    	database.addListener(databaseListener);

        listSelectionListener = new RemarksUpdateSelectionListener();
        table.getSelectionModel().addListSelectionListener(listSelectionListener);

        focusListener = new DefaultButtonFocusListener();
        addListeners(ifc);
    }

    private void addListeners(Container container) {
        for (Component c : container.getComponents()) {
        	if ((c instanceof JTextField) || (c instanceof JComboBox)) {
        		c.addFocusListener(focusListener);
        	} else if (c instanceof Container) {
        		addListeners((Container) c);
        	}
        }
	}

	@Override
    public void onClose() {
    	removeListeners();
    }

    private void removeListeners() {
    	database.removeListener(databaseListener);
    	table.getSelectionModel().removeListSelectionListener(listSelectionListener);
    	removeListeners(ifc);
    }

    private void removeListeners(Container container) {
        for (Component c : container.getComponents()) {
        	if ((c instanceof JTextField) || (c instanceof JComboBox)) {
        		c.removeFocusListener(focusListener);
        	} else if (c instanceof Container) {
        		removeListeners((Container) c);
        	}
        }
	}

    /**
     * Searches for matching parties. The entered search criteria are used
     * to find parties. The matching parties are shown in the table.
     */
    private void onSearch() {
        PartySearchCriteria searchCriteria = new PartySearchCriteria();

        if (!StringUtil.isNullOrEmpty(idModel.getString())) {
            searchCriteria.setId(idModel.getString());
        }
        if (!StringUtil.isNullOrEmpty(nameModel.getString())) {
            searchCriteria.setName(nameModel.getString());
        }
        if (!StringUtil.isNullOrEmpty(addressModel.getString())) {
            searchCriteria.setAddress(addressModel.getString());
        }
        if (!StringUtil.isNullOrEmpty(zipCodeModel.getString())) {
            searchCriteria.setZipCode(zipCodeModel.getString());
        }
        if (!StringUtil.isNullOrEmpty(cityModel.getString())) {
            searchCriteria.setCity(cityModel.getString());
        }
        if (birthDateModel.getDate() != null) {
            searchCriteria.setBirthDate(birthDateModel.getDate());
        }
        if (typeListModel.getSelectedIndex() > 0) {
            searchCriteria.setType(typeListModel.getSelectedItem());
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
        int choice = MessageDialog.showYesNoQuestion(this, "gen.titleWarning",
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

    private final class RemarksUpdateSelectionListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
		    int row = SwingUtils.getSelectedRowConvertedToModel(table);
		    if (row != -1) {
		        taRemarks.setText(partiesTableModel.getRow(row).getRemarks());
		    } else {
		        taRemarks.setText("");
		    }
		}
	}

	private final class SearchAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent event) {
		    onSearch();
		}
	}

	private final class DefaultButtonFocusListener extends FocusAdapter {
		@Override
		public void focusGained(FocusEvent e) {
		    setDefaultButton(btSearch);
		}
	}

	private final class SelectActionParty extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent evt) {
		    onSelectParty();
		}
	}

	private final class DeletePartyAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent evt) {
		    onDeleteParty();
		}
	}

	private final class EditPartyAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent evt) {
		    onEditParty();
		}
	}

	private final class AddPartyAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent evt) {
		    onAddParty();
		}
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

	private class DatabaseListenerImpl implements DatabaseListener {
		@Override
		public void databaseChanged(Database db) {
			updateTypeListModel();
		}
	}
}
