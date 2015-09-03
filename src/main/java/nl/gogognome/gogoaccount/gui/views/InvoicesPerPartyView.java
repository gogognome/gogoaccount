package nl.gogognome.gogoaccount.gui.views;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.businessobjects.Payment;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.components.document.DocumentListener;
import nl.gogognome.gogoaccount.gui.beans.PartyBean;
import nl.gogognome.gogoaccount.gui.controllers.EditInvoiceController;
import nl.gogognome.gogoaccount.models.PartyModel;
import nl.gogognome.gogoaccount.services.InvoiceService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.awt.layout.VerticalLayout;
import nl.gogognome.lib.gui.beans.InputFieldsRow;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.BooleanModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.util.DateUtil;

/**
 * This view shows all invoices per party or all invoices for a single party.
 */
public class InvoicesPerPartyView extends View {

	private static final long serialVersionUID = 1L;

	private Document document;
	private DocumentListener documentListener;
    private Currency currency;

	private JScrollPane invoicesScrollPane;
	private JPanel invoicesPanel;

	private PartyModel partyModel;
	private DateModel dateModel;
	private BooleanModel includeClosedInvoicesModel;

	private InvoicePanel selectedInvoicePanel;
	private JButton editInvoiceButton;

	private ModelChangeListener listener;

	public InvoicesPerPartyView(Document document) {
		super();
		this.document = document;
	}

	@Override
	public String getTitle() {
		return textResource.getString("InvoicesSinglePartyView.title");
	}

	@Override
	public void onInit() {
        try {
            Bookkeeping bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
            currency = bookkeeping.getCurrency();

            initModels();
            addComponents();
            addListeners();
            updateInvoicePanel();
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            close();
        }
	}

	private void initModels() {
		partyModel = new PartyModel();
		dateModel = new DateModel(new Date());
		includeClosedInvoicesModel = new BooleanModel();
	}

	private void addComponents() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel northPanel = createInvoiceSelectionPanel();
		northPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));

		invoicesPanel = createInvoicesPanel();
		invoicesScrollPane = widgetFactory.createScrollPane(invoicesPanel);
		invoicesScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
		editInvoiceButton = buttonPanel.addButton("InvoicesSinglePartyView.edit",
				new EditInvoiceAction());

		add(northPanel, BorderLayout.NORTH);
		add(invoicesScrollPane, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createInvoiceSelectionPanel() {
		InputFieldsRow row = new InputFieldsRow();
		addCloseable(row);
		row.addVariableSizeField("InvoicesSinglePartyView.party", new PartyBean(document, partyModel));
		row.addField("InvoicesSinglePartyView.date", dateModel);
		row.addField("InvoicesSinglePartyView.includeClosedInvoices", includeClosedInvoicesModel);
		return row;
	}

	private JPanel createInvoicesPanel() {
		JPanel panel = new JPanel(new VerticalLayout(10, VerticalLayout.BOTH));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.setBackground(Color.WHITE);
		panel.setFocusCycleRoot(true);
		panel.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());

		return panel;
	}

	@Override
	public void onClose() {
		removeListeners();
	}

	private void addListeners() {
		listener = new ModelChangeListenerImpl();
		partyModel.addModelChangeListener(listener);
		dateModel.addModelChangeListener(listener);
		includeClosedInvoicesModel.addModelChangeListener(listener);

		documentListener = new DocumentListenerImpl();
		document.addListener(documentListener);

		KeyboardFocusManager focusManager =
		    KeyboardFocusManager.getCurrentKeyboardFocusManager();
		focusManager.addPropertyChangeListener(
		    new InvoicePanelFocusListener()
		);
	}

	private void removeListeners() {
		document.removeListener(documentListener);

		includeClosedInvoicesModel.removeModelChangeListener(listener);
		dateModel.removeModelChangeListener(listener);
		partyModel.removeModelChangeListener(listener);
	}

	private void updateInvoicePanel() {
		updateTitleBorder();
		updateInvoicesInPanel();
	}

	private void updateTitleBorder() {
		Date date = dateModel.getDate();
		Party party = partyModel.getParty();

		if (party != null && date != null) {
			invoicesScrollPane.setBorder(widgetFactory.createTitleBorder("InvoicesSinglePartyView.invoicesForParty",
			        party.getId() + " - " + party.getName(),
			        textResource.formatDate("gen.dateFormat", date)));
		} else if (date != null) {
			invoicesScrollPane.setBorder(widgetFactory.createTitleBorder("InvoicesSinglePartyView.invoicesForAllParties",
			        textResource.formatDate("gen.dateFormat", date)));
		} else {
			invoicesScrollPane.setBorder(widgetFactory.createTitleBorder("InvoicesSinglePartyView.initialPanelTitle"));
		}
	}

	private void updateInvoicesInPanel() {
		Date date = dateModel.getDate();
		Party party = partyModel.getParty();

		invoicesPanel.removeAll();

		if (date == null) {
			return;
		}

        Invoice[] invoices = document.getInvoices();
        sortInvoices(invoices, party);
        addInvoicesToPanel(invoices, party, date);

        validate();
	}

	private void sortInvoices(Invoice[] invoices, Party party) {
        sortInvoicesOnDate(invoices);
        if (party == null) {
        	sortInvoicesPerParty(invoices);
        }
	}

	private void sortInvoicesOnDate(Invoice[] invoices) {
		Arrays.sort(invoices, (o1, o2) -> DateUtil.compareDayOfYear(o1.getIssueDate(), o2.getIssueDate()));
	}

	private void sortInvoicesPerParty(Invoice[] invoices) {
		Arrays.sort(invoices, (o1, o2) -> o1.getConcerningParty().getId().compareTo(o2.getConcerningParty().getId()));
	}

	private void addInvoicesToPanel(Invoice[] invoices, Party party, Date date) {
		int row = 0;
        Party prevParty = null;
        for (Invoice invoice : invoices) {
        	if (mustInvoiceBeIncluded(invoice, date)) {
        		Party concerningParty = invoice.getConcerningParty();
        		boolean addInvoice = false;
            	if (party == null) {
            		addInvoice = true;
            		if (prevParty == null || !prevParty.equals(concerningParty)) {
            			JLabel label = new JLabel(concerningParty.getId() + " - " + concerningParty.getName());
            			label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            			invoicesPanel.add(label,
            					SwingUtils.createPanelGBConstraints(0, row));
            			prevParty = concerningParty;
            		}
            	} else if (party.equals(concerningParty)) {
            		addInvoice = true;
            	}

            	if (addInvoice) {
            		List<Payment> payments = InvoiceService.getPayments(document, invoice.getId());
            		InvoicePanel ip = new InvoicePanel(invoice, payments, date, currency);
            		invoicesPanel.add(ip, SwingUtils.createPanelGBConstraints(0, row));
            		row++;
            	}
        	}
        }
	}

	private boolean mustInvoiceBeIncluded(Invoice invoice, Date date) {
		return isInvoiceCreatedBeforeDate(invoice, date)
			&& isInvoiceClosedAndMustItBeIncluded(invoice, date);
	}

	private boolean isInvoiceCreatedBeforeDate(Invoice invoice, Date date) {
		return DateUtil.compareDayOfYear(invoice.getIssueDate(), date) <= 0;
	}

	private boolean isInvoiceClosedAndMustItBeIncluded(Invoice invoice, Date date) {
        return includeClosedInvoicesModel.getBoolean() || !InvoiceService.isPaid(document, invoice.getId(), date);
    }

	private void editSelectedInvoice() {
		EditInvoiceController controller =
			new EditInvoiceController(this, document, selectedInvoicePanel.getInvoice());
		controller.execute();
	}

	private void setSelectedInvoicePanel(InvoicePanel invoicePanel) {
		if (invoicePanel != null) {
			if (selectedInvoicePanel != null) {
				selectedInvoicePanel.onSelectionLost();
			}
			selectedInvoicePanel = invoicePanel;
			invoicePanel.onSelectionGained();
			Rectangle bounds = selectedInvoicePanel.getBounds();
			Point p = invoicesScrollPane.getViewport().getViewPosition();
			bounds.translate(-p.x, -p.y);
			invoicesScrollPane.getViewport().scrollRectToVisible(bounds);
		}
		editInvoiceButton.setEnabled(selectedInvoicePanel != null);
	}

	private class EditInvoiceAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			editSelectedInvoice();
		}
	}

	private final class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
			updateInvoicePanel();
		}
	}

	private class DocumentListenerImpl implements DocumentListener {

		@Override
		public void documentChanged(Document document) {
			updateInvoicesInPanel();
		}
	}

	private final class InvoicePanelFocusListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent e) {
		    String prop = e.getPropertyName();
		    if ("focusOwner".equals(prop)) {
		    	InvoicePanel invoicePanel = null;
		         if (e.getNewValue() instanceof InvoicePanel) {
				    invoicePanel = (InvoicePanel)e.getNewValue();
		         }
		         setSelectedInvoicePanel(invoicePanel);
		    }
		}
	}
}
