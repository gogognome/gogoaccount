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
package nl.gogognome.gogoaccount.gui.views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.InvoiceSearchCriteria;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.lib.swing.ActionWrapper;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.TableRowSelectAction;
import nl.gogognome.lib.swing.views.View;

/**
 * This class implements a view for selecting and editing invoices.
 *
 * TODO: implement editing invoices
 *
 * @author Sander Kooijmans
 */
public class InvoiceEditAndSelectionView extends View {

	private InvoicesTableModel invoicesTableModel;

    private JTable table;

    /** The database whose invoices are to be shown and changed. */
    private Document document;

    /** Indicates whether this view should also allow the user to select an invoice. */
    private boolean selectioEnabled;

    /**
     * Indicates that multiple invoices can be selected (<code>true</code>) or at most
     * one invoice (<code>false</code>).
     */
    private boolean multiSelectionEnabled;

    /** The invoices selected by the user or <code>null</code> if no invoice has been selected. */
    private Invoice[] selectedInvoices;

    private JTextField tfId;
    private JTextField tfName;
    private JCheckBox btIncludeClosedInvoices;

    private JButton btSearch;
    private JButton btSelect;

    /** Focus listener used to change the deafult button. */
    private FocusListener focusListener;

    /**
     * Constructor for an invoices view in which at most one invoice can be selected.
     * @param document the database used to search for invoices
     * @param selectioEnabled <code>true</code> if the user should be able to select an invoice;
     *         <code>false</code> if the user cannot select an invoice
     */
    public InvoiceEditAndSelectionView(Document document, boolean selectionEnabled) {
        this(document, selectionEnabled, false);
    }

    /**
     * Constructor.
     * @param document the database used to search for invoices and to add, delete or update invoices from.
     * @param selectioEnabled <code>true</code> if the user should be able to select an invoice;
     *         <code>false</code> if the user cannot select an invoice
     * @param multiSelectionEnabled indicates that multiple invoices can be selected (<code>true</code>) or
     *         at most one invoice (<code>false</code>)
     */
    public InvoiceEditAndSelectionView(Document document, boolean selectionEnabled, boolean multiSelectionEnabled) {
        this.document = document;
        this.selectioEnabled = selectionEnabled;
        this.multiSelectionEnabled = multiSelectionEnabled;
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
            textResource.getString("invoicesView.includeClosedInvoices"), false);
        criteriaPanel.add(btIncludeClosedInvoices,
            SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        JPanel buttonPanel = new JPanel(new FlowLayout());
        ActionWrapper actionWrapper = widgetFactory.createAction("invoicesView.btnSearch");
        actionWrapper.setAction(new SearchAction());
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

        invoicesTableModel = new InvoicesTableModel(document);
        table = widgetFactory.createSortedTable(invoicesTableModel);
        table.getSelectionModel().setSelectionMode(multiSelectionEnabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
        Action selectionAction = new SelectInvoiceAction();
        TableRowSelectAction trsa = new TableRowSelectAction(table, selectionAction);
        addCloseable(trsa);
        trsa.registerListeners();

        resultPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Create a panel containing the search criteria and result panels
        JPanel result = new JPanel(new GridBagLayout());
        result.add(criteriaPanel,
                SwingUtils.createTextFieldGBConstraints(0, 0));
        result.add(resultPanel,
                SwingUtils.createPanelGBConstraints(0, 1));

        return result;
    }

    /**
     * @see nl.gogognome.lib.swing.views.View#onClose()
     */
    @Override
    public void onClose() {
        tfId.removeFocusListener(focusListener);
        tfName.removeFocusListener(focusListener);
        focusListener = null;
    }

    /**
     * Searches for matching invoices. The entered search criteria are used
     * to find invoices. The matching invoices are shown in the table.
     */
    private void onSearch() {
        InvoiceSearchCriteria searchCriteria = new InvoiceSearchCriteria();

        if (tfId.getText().length() > 0) {
            searchCriteria.setId(tfId.getText());
        }
        if (tfName.getText().length() > 0) {
            searchCriteria.setName(tfName.getText());
        }
        searchCriteria.setIncludeClosedInvoices(btIncludeClosedInvoices.isSelected());

        invoicesTableModel.replaceRows(Arrays.asList(document.getInvoices(searchCriteria)));
        SwingUtils.selectFirstRow(table);
        table.requestFocusInWindow();

        // Update the default button if the select button is present
        if (btSelect != null) {
            setDefaultButton(btSelect);
        }
    }

    /**
     * This method is called when the "select invoice" button is pressed.
     */
    private void onSelectInvoice() {
        int rows[] = SwingUtils.getSelectedRowsConvertedToModel(table);
        selectedInvoices = new Invoice[rows.length];
        for (int i = 0; i < rows.length; i++) {
            selectedInvoices[i] = invoicesTableModel.getRow(rows[i]);
        }
        closeAction.actionPerformed(null);
    }

    /**
     * Gets the invoices that were selected by the user.
     * @return the invoices or <code>null</code> if no invoice has been selected
     */
    public Invoice[] getSelectedInvoices() {
        return selectedInvoices;
    }

    private final class SelectInvoiceAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    if (selectioEnabled) {
		        onSelectInvoice();
		    }
		}
	}

	private final class SearchAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent event) {
		    onSearch();
		}
	}
}
