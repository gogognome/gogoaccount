package nl.gogognome.gogoaccount.gui.beans;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.gui.views.InvoiceEditAndSelectionView;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * This class implements a widget for selecting an <code>Invoice</code>.
 */
public class InvoiceBean extends JPanel {

    private final Logger logger = LoggerFactory.getLogger(InvoiceBean.class);

    private final PartyService partyService = ObjectFactory.create(PartyService.class);

	/** The database used to select the invoice from. */
    private Document document;

    /** Contains a description of the selected invoice. */
    private JTextField tfDescription;

    /** The invoice that is selected in this selector. */
    private Invoice selectedInvoice;

    public InvoiceBean(Document document) {
        this.document = document;
        WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
        setLayout(new GridBagLayout());

        tfDescription = new JTextField(20);
        tfDescription.setEditable(false);
        tfDescription.setFocusable(false);

		/* The button to select a party from a dialog. */
        JButton btSelect = wf.createIconButton("gen.btSelectInvoice", new SelectAction(), 21);
		/* The button to clear the selected party. */
        JButton btClear = wf.createIconButton("gen.btClearInvoice", new ClearAction(), 21);

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

    public void setSelectedInvoice(Invoice invoice) {
        selectedInvoice = invoice;
        if (selectedInvoice != null) {
            try {
                tfDescription.setText(invoice.getId() + " (" + partyService.getParty(document, invoice.getPayingPartyId()).getName() + ")");
            } catch (ServiceException e) {
                logger.warn("Ignored exception: " + e.getMessage(), e);
                tfDescription.setText(invoice.getId() + " (???)");
            }
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
        Container finalParent = parent;

        HandleException.for_(parent, () -> {
            InvoiceEditAndSelectionView invoicesView = new InvoiceEditAndSelectionView(document, true);
            ViewDialog dialog = new ViewDialog(finalParent, invoicesView);
            dialog.showDialog();
            if (invoicesView.getSelectedInvoices() != null) {
                setSelectedInvoice(invoicesView.getSelectedInvoices().get(0));
            }
        });
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
