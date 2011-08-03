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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import nl.gogognome.lib.swing.ActionWrapper;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.TableRowSelectAction;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.InvoiceSearchCriteria;

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
    private Database database;

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

    /** Text area that shows details about the selected invoice */
    private JTextArea taRemarks;

    private JButton btSearch;
    private JButton btSelect;

    /** Focus listener used to change the deafult button. */
    private FocusListener focusListener;

    /**
     * Constructor for an invoices view in which at most one invoice can be selected.
     * @param database the database used to search for invoices
     * @param selectioEnabled <code>true</code> if the user should be able to select an invoice;
     *         <code>false</code> if the user cannot select an invoice
     */
    public InvoiceEditAndSelectionView(Database database, boolean selectionEnabled) {
        this(database, selectionEnabled, false);
    }

    /**
     * Constructor.
     * @param database the database used to search for invoices and to add, delete or update invoices from.
     * @param selectioEnabled <code>true</code> if the user should be able to select an invoice;
     *         <code>false</code> if the user cannot select an invoice
     * @param multiSelectionEnabled indicates that multiple invoices can be selected (<code>true</code>) or
     *         at most one invoice (<code>false</code>)
     */
    public InvoiceEditAndSelectionView(Database database, boolean selectionEnabled, boolean multiSelectionEnabled) {
        this.database = database;
        this.selectioEnabled = selectionEnabled;
        this.multiSelectionEnabled = multiSelectionEnabled;
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#getTitle()
     */
    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("invoicesView.title");
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onInit()
     */
    @Override
    public void onInit() {
        // Create button panel
        WidgetFactory wf = WidgetFactory.getInstance();
//        JButton addButton = wf.createButton("invoicesView.addParty", new AbstractAction() {
//            public void actionPerformed(ActionEvent evt) {
//                onAddParty();
//            }
//        });
//
//        JButton editButton = wf.createButton("invoicesView.editParty", new AbstractAction() {
//            public void actionPerformed(ActionEvent evt) {
//                onEditParty();
//            }
//        });
//        JButton deleteButton = wf.createButton("invoicesView.deleteParty", new AbstractAction() {
//            public void actionPerformed(ActionEvent evt) {
//                onDeleteParty();
//            }
//        });
        btSelect = wf.createButton("invoicesView.selectInvoice", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent evt) {
                onSelectInvoice();
            }
        });
//
        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 0, 5));
//        buttonPanel.add(addButton);
//        buttonPanel.add(editButton);
//        buttonPanel.add(deleteButton);
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
        WidgetFactory wf = WidgetFactory.getInstance();
        TextResource tr =  TextResource.getInstance();

        // Create the criteria panel
        JPanel criteriaPanel = new JPanel(new GridBagLayout());
        criteriaPanel.setBorder(new TitledBorder(
                tr.getString("invoicesView.searchCriteria")));

        focusListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setDefaultButton(btSearch);
            }
        };

        int row = 0;
        tfId = new JTextField();
        tfId.addFocusListener(focusListener);
        criteriaPanel.add(wf.createLabel("invoicesView.id", tfId),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfId,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfName = new JTextField();
        tfName.addFocusListener(focusListener);
        criteriaPanel.add(wf.createLabel("invoicesView.name", tfName),
                SwingUtils.createLabelGBConstraints(0, row));
        criteriaPanel.add(tfName,
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        btIncludeClosedInvoices = new JCheckBox(
            tr.getString("invoicesView.includeClosedInvoices"), false);
        criteriaPanel.add(btIncludeClosedInvoices,
            SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        JPanel buttonPanel = new JPanel(new FlowLayout());
        ActionWrapper actionWrapper = wf.createAction("invoicesView.btnSearch");
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
        resultPanel.setBorder(new TitledBorder(
                tr.getString("invoicesView.foundParties")));

        invoicesTableModel = new InvoicesTableModel(Collections.<Invoice>emptyList());
        table = WidgetFactory.getInstance().createSortedTable(invoicesTableModel);
        table.getSelectionModel().setSelectionMode(multiSelectionEnabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
        Action selectionAction = new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                if (selectioEnabled) {
                    onSelectInvoice();
//                } else {
//                    onEditParty();
                }
            }
        };
        TableRowSelectAction trsa = new TableRowSelectAction(table, selectionAction);
        addCloseable(trsa);
        trsa.registerListeners();

        resultPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Create details panel
        JPanel detailPanel = new JPanel(new GridBagLayout());

        taRemarks = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(taRemarks);
        scrollPane.setPreferredSize(new Dimension(500, 100));

        detailPanel.add(wf.createLabel("invoicesView.remarks", taRemarks),
            SwingUtils.createGBConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 12, 0, 0, 12));
        detailPanel.add(scrollPane, SwingUtils.createGBConstraints(1, 0, 1, 1, 1.0, 1.0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, 12, 0, 12, 12));

        resultPanel.add(detailPanel, BorderLayout.SOUTH);

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

        invoicesTableModel.replaceRows(Arrays.asList(database.getInvoices(searchCriteria)));
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
}
