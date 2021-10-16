package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceSearchCriteria;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.TableRowSelectAction;
import nl.gogognome.lib.swing.action.ActionWrapper;
import nl.gogognome.lib.swing.models.Tables;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.AmountFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.*;

/**
 * This class implements a view for selecting and editing invoices.
 */
public class InvoiceEditAndSelectionView extends View {

    private final Document document;
    private final AmountFormat amountFormat;
    private final InvoiceService invoiceService;
    private final PartyService partyService;

	private InvoicesTableModel invoicesTableModel;

    private JTable table;

    /** Indicates whether this view should also allow the user to select an invoice. */
    private boolean selectionEnabled;

    private boolean multiSelectionEnabled;

    private java.util.List<Invoice> selectedInvoices;

    private JTextField tfId;
    private JTextField tfName;
    private JCheckBox btIncludeClosedInvoices;

    private JButton btSearch;
    private JButton btSelect;

    private FocusListener focusListener;

    public InvoiceEditAndSelectionView(Document document, AmountFormat amountFormat, InvoiceService invoiceService, PartyService partyService) {
        this.document = document;
        this.amountFormat = amountFormat;
        this.invoiceService = invoiceService;
        this.partyService = partyService;
    }

    public void enableSingleSelect() {
        selectionEnabled = true;
        multiSelectionEnabled = false;
    }

    public void enableMultiSelect() {
        selectionEnabled = true;
        multiSelectionEnabled = true;
    }

    @Override
    public String getTitle() {
        return textResource.getString("invoicesView.title");
    }

    @Override
    public void onInit() {
        // Create button panel
        btSelect = widgetFactory.createButton("invoicesView.selectInvoice", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                onSelectInvoice();
            }
        });

        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 0, 5));
        if (selectionEnabled) {
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
     * found invoices.
     *
     * @return the panel
     */
    private JPanel createPanel() {
        // Create the criteria panel
        JPanel criteriaPanel = new JPanel(new GridBagLayout());
        criteriaPanel.setBorder(widgetFactory.createTitleBorderWithPadding(
                "invoicesView.searchCriteria"));

        focusListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setDefaultButton(btSearch);
            }
        };

        int row = 0;
        tfId = new JTextField();
        tfId.addFocusListener(focusListener);
        criteriaPanel.add(widgetFactory.createLabel("invoicesView.id", tfId),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfId,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfName = new JTextField();
        tfName.addFocusListener(focusListener);
        criteriaPanel.add(widgetFactory.createLabel("invoicesView.name", tfName),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfName,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        btIncludeClosedInvoices = new JCheckBox(
            textResource.getString("invoicesView.includePaidInvoices"), false);
        criteriaPanel.add(btIncludeClosedInvoices,
            SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        JPanel buttonPanel = new JPanel(new FlowLayout());
        ActionWrapper actionWrapper = widgetFactory.createActionWrapper("invoicesView.btnSearch", this::onSearch);
        btSearch = new JButton(actionWrapper);

        buttonPanel.add(btSearch);
        criteriaPanel.add(buttonPanel,
                SwingUtils.createGBConstraints(0, row, 2, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE,
                        5, 0, 0, 0));

        // Create the result panel
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(widgetFactory.createTitleBorderWithPadding(
                "invoicesView.foundInvoices"));

        invoicesTableModel = new InvoicesTableModel(document, amountFormat, invoiceService, partyService);
        table = Tables.createSortedTable(invoicesTableModel);
        table.getSelectionModel().setSelectionMode(multiSelectionEnabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
        Action selectionAction = new SelectInvoiceAction();
        TableRowSelectAction action = new TableRowSelectAction(table, selectionAction);
        addCloseable(action);
        action.registerListeners();

        resultPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Create a panel containing the search criteria and result panels
        JPanel result = new JPanel(new GridBagLayout());
        result.add(criteriaPanel, SwingUtils.createTextFieldGBConstraints(0, 0));
        result.add(resultPanel, SwingUtils.createPanelGBConstraints(0, 1));

        return result;
    }

    @Override
    public void onClose() {
        tfId.removeFocusListener(focusListener);
        tfName.removeFocusListener(focusListener);
        focusListener = null;
    }

    private void onSearch() {
        try {
            InvoiceSearchCriteria searchCriteria = new InvoiceSearchCriteria();

            if (tfId.getText().length() > 0) {
                searchCriteria.setId(tfId.getText());
            }
            if (tfName.getText().length() > 0) {
                searchCriteria.setName(tfName.getText());
            }
            searchCriteria.setIncludeClosedInvoices(btIncludeClosedInvoices.isSelected());

            invoicesTableModel.setRows(invoiceService.findInvoices(document, searchCriteria));
            Tables.selectFirstRow(table);
            table.requestFocusInWindow();

            // Update the default button if the select button is present
            if (btSelect != null) {
                setDefaultButton(btSelect);
            }
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
    }

    /**
     * This method is called when the "select invoice" button is pressed.
     */
    private void onSelectInvoice() {
        int rows[] = Tables.getSelectedRowsConvertedToModel(table);
        selectedInvoices = new ArrayList<>();
        for (int row : rows) {
            selectedInvoices.add(invoicesTableModel.getRow(row));
        }
        closeAction.actionPerformed(null);
    }

    /**
     * Gets the invoices that were selected by the user.
     * @return the invoices or <code>null</code> if no invoice has been selected
     */
    public java.util.List<Invoice> getSelectedInvoices() {
        return selectedInvoices;
    }

    private final class SelectInvoiceAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    if (selectionEnabled) {
		        onSelectInvoice();
		    }
		}
	}

}
