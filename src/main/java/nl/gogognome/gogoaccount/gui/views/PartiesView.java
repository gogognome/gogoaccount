package nl.gogognome.gogoaccount.gui.views;

import static java.util.Collections.*;
import static nl.gogognome.lib.util.StringUtil.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.*;
import nl.gogognome.gogoaccount.component.automaticcollection.*;
import nl.gogognome.gogoaccount.component.document.*;
import nl.gogognome.gogoaccount.component.party.*;
import nl.gogognome.gogoaccount.gui.*;
import nl.gogognome.lib.gui.beans.*;
import nl.gogognome.lib.swing.*;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.*;
import nl.gogognome.lib.swing.views.*;
import nl.gogognome.textsearch.criteria.*;

/**
 * This class implements a view for adding, removing, editing and (optionally) selecting parties.
 */
public class PartiesView extends View {

	private static final long serialVersionUID = 1L;

    private final static Logger logger = LoggerFactory.getLogger(PartiesView.class);

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

    private final StringModel searchCriterionModel = new StringModel();

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
        JButton addButton = widgetFactory.createButton("partiesView.addParty", () -> onAddParty());

        JButton editButton = widgetFactory.createButton("partiesView.editParty", () -> onEditParty());
        JButton deleteButton = widgetFactory.createButton("partiesView.deleteParty", () -> onDeleteParty());
        JButton exportButton = widgetFactory.createButton("partiesView.exportParties", () -> onExportParties());
        btSelect = widgetFactory.createButton("partiesView.selectParty", () -> onSelectParty());

        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 0, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(exportButton);
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
                party = partyService.createPartyWithNewId(document, party, tags);
                PartyAutomaticCollectionSettings enteredAutomaticCollectionSettings = editPartyView.getEnteredAutomaticCollectionSettings();
                enteredAutomaticCollectionSettings.setPartyId(party.getId());
                automaticCollectionService.setAutomaticCollectionSettings(document, enteredAutomaticCollectionSettings);
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

	private void onExportParties() {
        handleException.of(() -> {
            File file = selectFileToExportPartiesTo();
            if (file == null) {
                return;
            }

            logger.info("Exporting parties to " + file);
            exportParties(file);
        });
    }

    private File selectFileToExportPartiesTo() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter(textResource.getString("partiesExport.fileType"), ".xlsx"));
        int choice = fc.showSaveDialog(this);
        return choice == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    private void exportParties(File file) throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(textResource.getString("partiesExport.sheet"));
        Row row = sheet.createRow(0);
        for (int columnIndex=0; columnIndex < partiesTableModel.getColumnCount(); columnIndex++) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(partiesTableModel.getColumnName(columnIndex));
        }

        for (int rowIndex = 0; rowIndex < partiesTableModel.getRowCount(); rowIndex++) {
            row = sheet.createRow(rowIndex+1);
            for (int columnIndex=0; columnIndex < partiesTableModel.getColumnCount(); columnIndex++) {
                Cell cell = row.createCell(columnIndex);
                Object value = partiesTableModel.getValueAt(rowIndex, columnIndex);
                if (value != null) {
                    cell.setCellValue(value.toString());
                }
            }
        }

        try (OutputStream fileOut = new FileOutputStream(file)) {
            wb.write(fileOut);
        }
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
