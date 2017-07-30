package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionService;
import nl.gogognome.gogoaccount.component.automaticcollection.PartyAutomaticCollectionSettings;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.TableRowSelectAction;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.models.Tables;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.criteria.Parser;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static nl.gogognome.lib.util.StringUtil.isNullOrEmpty;

/**
 * This class implements a view for adding, removing, editing and (optionally) selecting parties.
 */
public class PartiesView extends View {

	private static final long serialVersionUID = 1L;

    private final Document document;
    private final AutomaticCollectionService automaticCollectionService;
    private final PartyService partyService;
    private final ViewFactory viewFactory;
    private final MessageDialog messageDialog;
    private final HandleException handleException;

    private JTable table;
	private PartiesTableModel partiesTableModel;

    private boolean selectionEnabled;
    private boolean multiSelectionEnabled;

    private StringModel searchCriterionModel = new StringModel();

    private JTextArea taRemarks;

    private JButton btSearch;
    private JButton btSelect;

    private ListSelectionListener listSelectionListener;

    private Party[] selectedParties;

    public PartiesView(Document document, AutomaticCollectionService automaticCollectionService, PartyService partyService, ViewFactory viewFactory) {
        this.automaticCollectionService = automaticCollectionService;
        this.partyService = partyService;
        this.document = document;
        this.viewFactory = viewFactory;
        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);
    }

    public void setSelectionEnabled(boolean selectionEnabled) {
		this.selectionEnabled = selectionEnabled;
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
    	addComponents();
    	addListeners();
        if (!selectionEnabled) {
            onSearch();
        }
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
        if (selectionEnabled) {
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
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("partiesView.filter"));

        ifc.addField("gen.filterCriterion", searchCriterionModel);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        btSearch = widgetFactory.createButton("gen.btnSearch", this::onSearch);

        buttonPanel.add(btSearch);
        ifc.add(buttonPanel, SwingUtils.createGBConstraints(0, 10, 2, 1, 0.0, 0.0,
        		GridBagConstraints.EAST, GridBagConstraints.NONE, 5, 0, 0, 0));
        return ifc;
    }

    private JPanel createSearchResultPanel() {
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(new CompoundBorder(new TitledBorder(textResource.getString("partiesView.foundParties")),
            new EmptyBorder(5, 12, 5, 12)));

        partiesTableModel = new PartiesTableModel(emptyList(), emptyMap());
        table = Tables.createSortedTable(partiesTableModel);
        table.getSelectionModel().setSelectionMode(multiSelectionEnabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);

        TableRowSelectAction action = new TableRowSelectAction(table, new SelectionActionImpl());
        addCloseable(action);
        action.registerListeners();

        resultPanel.add(widgetFactory.createScrollPane(table), BorderLayout.CENTER);
        resultPanel.add(createDetailPanel(), BorderLayout.SOUTH);

        return resultPanel;
    }

    private Component createDetailPanel() {
        JPanel detailPanel = new JPanel(new GridBagLayout());

        taRemarks = new JTextArea();
        taRemarks.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(taRemarks);
        scrollPane.setPreferredSize(new Dimension(500, 100));

        detailPanel.add(widgetFactory.createLabel("partiesView.remarks", taRemarks),
                SwingUtils.createGBConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 12, 0, 0, 12));
        detailPanel.add(scrollPane, SwingUtils.createGBConstraints(1, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, 12, 0, 12, 12));

		return detailPanel;
	}

    private void addListeners() {
        listSelectionListener = new RemarksUpdateSelectionListener();
        table.getSelectionModel().addListSelectionListener(listSelectionListener);
        searchCriterionModel.addModelChangeListener(m -> setDefaultButton(btSearch));
    }

	@Override
    public void onClose() {
    	removeListeners();
    }

    private void removeListeners() {
    	table.getSelectionModel().removeListSelectionListener(listSelectionListener);
    }

    private void onSearch() {
        handleException.of(() -> {
            Criterion criterion = isNullOrEmpty(searchCriterionModel.getString()) ? null : new Parser().parse(searchCriterionModel.getString());
            List<Party> matchingParties = partyService.findParties(document, criterion);
            partiesTableModel.setRows(matchingParties, partyService.findPartyIdToTags(document));
            Tables.selectFirstRow(table);
            SwingUtilities.invokeLater(() -> table.requestFocusInWindow());

            // Update the default button if the select button is present
            if (btSelect != null) {
                setDefaultButton(btSelect);
            }
        });
    }

    private void onAddParty() {
        handleException.of(() -> {
            EditPartyView editPartyView = (EditPartyView) viewFactory.createView(EditPartyView.class);
            ViewDialog dialog = new ViewDialog(getViewOwner().getWindow(), editPartyView);
            dialog.showDialog();

            Party party = editPartyView.getEnteredParty();
            List<String> tags = editPartyView.getEnteredTags();
            if (party != null && tags != null) {
                partyService.createPartyWithNewId(document, party, tags);
                automaticCollectionService.setAutomaticCollectionSettings(document, editPartyView.getEnteredAutomaticCollectionSettings());
            }
            onSearch();
        });
    }

    private void onEditParty() {
        handleException.of(() -> {
            int row = Tables.getSelectedRowConvertedToModel(table);
            if (row == -1) {
                return;
            }

            Party oldParty = partiesTableModel.getRow(row);
            List<String> oldTags = partyService.findTagsForParty(document, oldParty);
            PartyAutomaticCollectionSettings oldSettings = automaticCollectionService.findSettings(document, oldParty);
            EditPartyView editPartyView = (EditPartyView) viewFactory.createView(EditPartyView.class);
            editPartyView.setInitialParty(oldParty, oldTags, oldSettings);
            ViewDialog dialog = new ViewDialog(getViewOwner().getWindow(), editPartyView);
            dialog.showDialog();

            Party party = editPartyView.getEnteredParty();
            List<String> tags = editPartyView.getEnteredTags();
            if (party != null && tags != null) {
                partyService.updateParty(document, party, tags);
                automaticCollectionService.setAutomaticCollectionSettings(document, editPartyView.getEnteredAutomaticCollectionSettings());
            }
            onSearch();

            try {
                Tables.selectRowWithModelIndex(table, row);
            } catch (IndexOutOfBoundsException e) {
                // ignore this exception. It occurs when a party is changed such that it does not match the current filter anymore.
            }
        });
    }

    private void onDeleteParty() {
        handleException.of(() -> {
            int row = Tables.getSelectedRowConvertedToModel(table);
            if (row == -1) {
                return;
            }

            Party party = partiesTableModel.getRow(row);
            int choice = messageDialog.showYesNoQuestion("gen.titleWarning",
                    "partiesView.areYouSurePartyIsDeleted", party.getName());
            if (choice == MessageDialog.YES_OPTION) {
                partyService.deleteParty(document, party);
            }
            onSearch();
        });
    }

    private void onSelectParty() {
        int rows[] = Tables.getSelectedRowsConvertedToModel(table);
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
		    int row = Tables.getSelectedRowConvertedToModel(table);
		    if (row != -1) {
		        taRemarks.setText(partiesTableModel.getRow(row).getRemarks());
		    } else {
		        taRemarks.setText("");
		    }
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
            if (selectionEnabled) {
                onSelectParty();
            } else {
                onEditParty();
            }
		}
    }
}
