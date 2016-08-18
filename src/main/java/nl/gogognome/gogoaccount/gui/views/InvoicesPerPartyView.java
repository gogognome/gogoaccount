package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentListener;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.beans.PartyBean;
import nl.gogognome.gogoaccount.gui.invoice.EditInvoiceController;
import nl.gogognome.gogoaccount.models.PartyModel;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class InvoicesPerPartyView extends View {

	private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(InvoicesPerPartyView.class);
	private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

	private Document document;
	private DocumentListener documentListener;

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
		row.addField("invoicesView.includePaidInvoices", includeClosedInvoicesModel);
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

	private void updateInvoicePanel() throws ServiceException {
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

	private void updateInvoicesInPanel() throws ServiceException {
		Date date = dateModel.getDate();
		Party party = partyModel.getParty();

		invoicesPanel.removeAll();

		if (date == null) {
			return;
		}

        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        sortInvoices(invoices, party);
        addInvoicesToPanel(invoices, party, date);

        validate();
	}

	private void sortInvoices(List<Invoice> invoices, Party party) {
        sortInvoicesOnDate(invoices);
        if (party == null) {
        	sortInvoicesPerParty(invoices);
        }
	}

	private void sortInvoicesOnDate(List<Invoice> invoices) {
		Collections.sort(invoices, (o1, o2) -> DateUtil.compareDayOfYear(o1.getIssueDate(), o2.getIssueDate()));
	}

	private void sortInvoicesPerParty(List<Invoice> invoices) {
		Collections.sort(invoices, (o1, o2) -> o1.getConcerningPartyId().compareTo(o2.getConcerningPartyId()));
	}

	private void addInvoicesToPanel(List<Invoice> invoices, Party party, Date date) throws ServiceException {
		int row = 0;
        Party prevParty = null;
        for (Invoice invoice : invoices) {
        	if (mustInvoiceBeIncluded(invoice, date)) {
        		Party concerningParty = partyService.getParty(document, invoice.getConcerningPartyId());
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
            		List<Payment> payments = invoiceService.findPayments(document, invoice);
            		InvoicePanel ip = new InvoicePanel(invoice,
                            invoiceService.findDescriptions(document, invoice),
                            invoiceService.findAmounts(document, invoice),
                            payments, partyService.getParty(document, invoice.getPayingPartyId()), date);
            		invoicesPanel.add(ip, SwingUtils.createPanelGBConstraints(0, row));
            		row++;
            	}
        	}
        }
	}

	private boolean mustInvoiceBeIncluded(Invoice invoice, Date date) throws ServiceException {
		return isInvoiceCreatedBeforeDate(invoice, date)
			&& isInvoiceClosedAndMustItBeIncluded(invoice, date);
	}

	private boolean isInvoiceCreatedBeforeDate(Invoice invoice, Date date) {
		return DateUtil.compareDayOfYear(invoice.getIssueDate(), date) <= 0;
	}

	private boolean isInvoiceClosedAndMustItBeIncluded(Invoice invoice, Date date) throws ServiceException {
        return includeClosedInvoicesModel.getBoolean() || !invoiceService.isPaid(document, invoice.getId(), date);
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
            try {
                updateInvoicePanel();
            } catch (ServiceException e) {
                logger.warn("ignored exception: " + e.getMessage(), e);
            }
		}
	}

	private class DocumentListenerImpl implements DocumentListener {

		@Override
		public void documentChanged(Document document) {

            try {
                updateInvoicesInPanel();
            } catch (ServiceException e) {
                logger.warn("ignored exception: " + e.getMessage(), e);
            }
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
