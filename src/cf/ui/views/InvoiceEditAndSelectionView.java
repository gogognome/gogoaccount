/*
 * $Id: InvoiceEditAndSelectionView.java,v 1.2 2008-04-06 17:49:49 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
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
import java.util.Comparator;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import nl.gogognome.framework.View;
import nl.gogognome.swing.ActionWrapper;
import nl.gogognome.swing.SortedTable;
import nl.gogognome.swing.SortedTableModel;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;
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
@SuppressWarnings("serial")
public class InvoiceEditAndSelectionView extends View {

    private InvoicesTableModel invoicesTableModel;
    
    private SortedTable table;
    
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
    public String getTitle() {
        return TextResource.getInstance().getString("invoicesView.title");
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onInit()
     */
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
        // TODO: add empty border
        resultPanel.setBorder(new TitledBorder(
                tr.getString("invoicesView.foundParties"))); 

        invoicesTableModel = new InvoicesTableModel();
        table = WidgetFactory.getInstance().createSortedTable(invoicesTableModel);
        table.getSelectionModel().setSelectionMode(multiSelectionEnabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table.getComponent());

        Action selectionAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (selectioEnabled) {
                    onSelectInvoice();
//                } else {
//                    onEditParty();
                }
            }
        };
        table.setSelectionAction(selectionAction);
        
        resultPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create details panel
        JPanel detailPanel = new JPanel(new GridBagLayout());
        
        taRemarks = new JTextArea();
        scrollPane = new JScrollPane(taRemarks);
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
     * @see nl.gogognome.framework.View#onClose()
     */
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
        
        invoicesTableModel.setInvoices(database.getInvoices(searchCriteria));
        table.selectFirstRow();
        table.getFocusableComponent().requestFocusInWindow();
        
        // Update the default button if the select button is present
        if (btSelect != null) {
            setDefaultButton(btSelect);
        }
    }

    /**
     * This method is called when the "select invoice" button is pressed.
     */
    private void onSelectInvoice() {
        int rows[] = table.getSelectedRows();
        selectedInvoices = new Invoice[rows.length];
        for (int i = 0; i < rows.length; i++) {
            selectedInvoices[i] = invoicesTableModel.getInvoice(rows[i]);
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
    
    /** The table model that shows information about the invoices. */
    private static class InvoicesTableModel extends AbstractTableModel implements SortedTableModel {

        /** The invoices to be shown. */
        private Invoice[] invoices = new Invoice[0];

        /**
         * Sets the invoices to be shown in the table.
         * @param invoices the invoices
         */
        public void setInvoices(Invoice[] invoices) {
            this.invoices = invoices;
            fireTableDataChanged();
        }
        
        /**
         * Gets the invoice for the specified row.
         * @param row the row
         */
        public Invoice getInvoice(int row) {
            return invoices[row];
        }

        /**
         * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
         */
        public Class<?> getColumnClass(int columnIndex) {
            switch(columnIndex) {
            case 0:
            case 1:
            case 2:
                return String.class;
            case 3:
                return Date.class;
            }
            return null;
        }
        
        /**
         * @see javax.swing.table.AbstractTableModel#getColumnName(int)
         */
        public String getColumnName(int columnIndex) {
            String id; 
            switch(columnIndex) {
            case 0: id = "gen.id"; break;
            case 1: id = "gen.name"; break;
            case 2: id = "gen.saldo"; break;
            case 3: id = "gen.date"; break;
            default: 
                id = null;
            }
            return TextResource.getInstance().getString(id);
        }
        
        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public int getColumnCount() {
            return 4;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            return invoices.length;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        public Object getValueAt(int row, int col) {
            switch(col) {
            case 0: return invoices[row].getId();
            case 1: return invoices[row].getPayingParty().getName();
            case 2: return TextResource.getInstance().getAmountFormat().formatAmount(
                invoices[row].getRemainingAmountToBePaid());
            case 3: return invoices[row].getIssueDate();
            default: return null;
            }
        }

        public int getColumnWidth(int column) {
            switch (column) {
            case 0:
                return 40;
            case 1:
            case 2:
                return 200;
            case 3:
                return 100;
            }
            return 0;
        }

        public Comparator<Object> getComparator(int column) {
            switch (column) {
            case 3:
                return new DateComparator();
            }
            return null;
        }

        public TableCellRenderer getRendererForColumn(int column) {
            return null;
        }
    }
    
    private static class DateComparator implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
            Date d1 = (Date) o1;
            Date d2 = (Date) o2;
            return DateUtil.compareDayOfYear(d1, d2);
        }
        
    }
}
