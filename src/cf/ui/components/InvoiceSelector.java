/*
 * $Id: InvoiceSelector.java,v 1.3 2008-01-17 20:51:57 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import cf.engine.Database;
import cf.engine.Invoice;
import cf.ui.views.InvoiceEditAndSelectionView;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import nl.gogognome.framework.ViewDialog;
import nl.gogognome.swing.ActionWrapper;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;

/**
 * This class implements a widget for selecting an <code>Invoice</code>. 
 *
 * @author Sander Kooijmans
 */
public class InvoiceSelector extends JPanel {

    /** The database used to select the invoice from. */
    private Database database;
    
    /** Contains a description of the selected invoice. */
    private JTextField tfDescription;
    
    /** The button to select a party from a dialog. */
    private JButton btSelect;
    
    /** The button to clear the selected party. */
    private JButton btClear;
    
    /** The invoice that is selected in this selector. */
    private Invoice selectedInvoice;
    
    /**
     * Constructor.
     */
    public InvoiceSelector(Database database) {
        this.database = database;
        WidgetFactory wf = WidgetFactory.getInstance();
        setLayout(new GridBagLayout());
        
        tfDescription = new JTextField();
        tfDescription.setEditable(false);
        tfDescription.setFocusable(false);
        
        Dimension dimension = new Dimension(21, 21);
        ActionWrapper actionWrapper = wf.createAction("gen.btSelectInvoice");
        btSelect = new JButton(actionWrapper);
        btSelect.setText(null);
        btSelect.setPreferredSize(dimension);
        actionWrapper.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                selectInvoice();
            }
        });

        actionWrapper = wf.createAction("gen.btClearInvoice");
        btClear = new JButton(actionWrapper);
        btClear.setText(null);
        btClear.setPreferredSize(dimension);
        actionWrapper.setAction(new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                setSelectedInvoice(null);
            }
        });
        
        add(tfDescription, SwingUtils.createTextFieldGBConstraints(0, 0));
        add(btSelect, SwingUtils.createGBConstraints(1, 0, 1, 1, 0.0, 0.0, 
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        0, 5, 0, 0));
        add(btClear, SwingUtils.createGBConstraints(2, 0, 1, 1, 0.0, 0.0, 
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                0, 2, 0, 0));
    }
    
    /**
     * Gets the selected invoice.
     * @return the selected invoice or <code>null</code> if no invoice is selected.
     */
    public Invoice getSelectedInvoice() {
        return selectedInvoice;
    }
    
    /**
     * Selecs an invoice.
     * @param party the invoice
     */
    public void setSelectedInvoice(Invoice invoice) {
        selectedInvoice = invoice;
        if (selectedInvoice != null) {
            tfDescription.setText(invoice.getId() + " (" + invoice.getPayingParty().getName() + ")");
        } else {
            tfDescription.setText(null);
        }
    }
    
    /**
     * Lets the user select an invoice in a dialog.
     */
    public void selectInvoice() {
        Container parent = getParent();
        while(!(parent instanceof Window)) {
            parent = parent.getParent();
        }

        InvoiceEditAndSelectionView invoicesView = new InvoiceEditAndSelectionView(database, true);
        ViewDialog dialog = new ViewDialog((Window)parent, invoicesView);
        dialog.showDialog();
        if (invoicesView.getSelectedInvoices() != null) {
            setSelectedInvoice(invoicesView.getSelectedInvoices()[0]);
        }
    }
    
}
