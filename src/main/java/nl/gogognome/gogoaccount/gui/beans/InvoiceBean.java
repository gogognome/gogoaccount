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
package nl.gogognome.gogoaccount.gui.beans;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.gui.views.InvoiceEditAndSelectionView;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.Factory;

/**
 * This class implements a widget for selecting an <code>Invoice</code>.
 *
 * @author Sander Kooijmans
 */
public class InvoiceBean extends JPanel {

	/** The database used to select the invoice from. */
    private Document document;

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
    public InvoiceBean(Document document) {
        this.document = document;
        WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
        setLayout(new GridBagLayout());

        tfDescription = new JTextField(20);
        tfDescription.setEditable(false);
        tfDescription.setFocusable(false);

		btSelect = wf.createIconButton("gen.btSelectInvoice", new SelectAction(), 21);
		btClear = wf.createIconButton("gen.btClearInvoice", new ClearAction(), 21);

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
     * Selects an invoice.
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

        InvoiceEditAndSelectionView invoicesView = new InvoiceEditAndSelectionView(document, true);
        ViewDialog dialog = new ViewDialog(parent, invoicesView);
        dialog.showDialog();
        if (invoicesView.getSelectedInvoices() != null) {
            setSelectedInvoice(invoicesView.getSelectedInvoices()[0]);
        }
    }

	private final class SelectAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    selectInvoice();
		}
	}

    private final class ClearAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    setSelectedInvoice(null);
		}
	}
}
