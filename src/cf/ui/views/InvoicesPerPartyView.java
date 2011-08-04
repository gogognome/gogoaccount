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
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import nl.gogognome.gogoaccount.gui.beans.PartyBean;
import nl.gogognome.gogoaccount.models.PartyModel;
import nl.gogognome.lib.awt.layout.VerticalLayout;
import nl.gogognome.lib.gui.beans.InputFieldsRow;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.BooleanModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.util.DateUtil;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.Invoice;
import cf.engine.Party;

/**
 * This view shows all invoices per party or all invoices for a single party.
 *
 * @author Sander Kooijmans
 */
public class InvoicesPerPartyView extends View {

	private static final long serialVersionUID = 1L;

	private Database database;
	private DatabaseListener databaseListener;

	private JScrollPane invoicesScrollPane;
	private JPanel invoicesPanel;

	private PartyModel partyModel;
	private DateModel dateModel;
	private BooleanModel includeClosedInvoicesModel;

	private ModelChangeListener listener;

	public InvoicesPerPartyView(Database database) {
		super();
		this.database = database;
	}

	@Override
	public String getTitle() {
		return textResource.getString("InvoicesSinglePartyView.title");
	}

	@Override
	public void onInit() {
		initModels();
		addComponents();
		addListeners();
		updateInvoicePanel();
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
		add(northPanel, BorderLayout.NORTH);

		invoicesPanel = createInvoicesPanel();
		invoicesScrollPane = widgetFactory.createScrollPane(invoicesPanel);
		invoicesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(invoicesScrollPane, BorderLayout.CENTER);
	}

	private JPanel createInvoiceSelectionPanel() {
		InputFieldsRow row = new InputFieldsRow();
		addCloseable(row);
		row.addVariableSizeField("InvoicesSinglePartyView.party", new PartyBean(database, partyModel));
		row.addField("InvoicesSinglePartyView.date", dateModel);
		row.addField("InvoicesSinglePartyView.includeClosedInvoices", includeClosedInvoicesModel);
		return row;
	}

	private GridBagConstraints createConstraints(int col) {
		GridBagConstraints gbc = SwingUtils.createLabelGBConstraints(col, 0);
		gbc.insets.right = 10;
		return gbc;
	}

	private JPanel createInvoicesPanel() {
		JPanel panel = new JPanel(new VerticalLayout(10, VerticalLayout.BOTH));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.setBackground(Color.WHITE);

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

		databaseListener = new DatabaseListenerImpl();
		database.addListener(databaseListener);
	}

	private void removeListeners() {
		database.removeListener(databaseListener);

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

        Invoice[] invoices = database.getInvoices();
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
		Arrays.sort(invoices, new Comparator<Invoice>() {
            @Override
			public int compare(Invoice o1, Invoice o2) {
                return DateUtil.compareDayOfYear(o1.getIssueDate(), o2.getIssueDate());
            }
        });
	}

	private void sortInvoicesPerParty(Invoice[] invoices) {
		Arrays.sort(invoices, new Comparator<Invoice>() {
            @Override
			public int compare(Invoice o1, Invoice o2) {
                return o1.getPayingParty().getId().compareTo(o2.getPayingParty().getId());
            }
        });
	}

	private void addInvoicesToPanel(Invoice[] invoices, Party party, Date date) {
        int row = 0;
        Party prevParty = null;
        for (Invoice invoice : invoices) {
        	if (mustInvoiceBeIncluded(invoice, date)) {
        		Party payingParty = invoice.getPayingParty();
        		boolean addInvoice = false;
            	if (party == null) {
            		addInvoice = true;
            		if (prevParty == null || !prevParty.equals(payingParty)) {
            			JLabel label = new JLabel(payingParty.getId() + " - " + payingParty.getName());
            			label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            			invoicesPanel.add(label,
            					SwingUtils.createPanelGBConstraints(0, row));
            			prevParty = payingParty;
            		}
            	} else if (party.equals(payingParty)) {
            		addInvoice = true;
            	}

            	if (addInvoice) {
            		invoicesPanel.add(new InvoicePanel(invoice, date, database.getCurrency()),
            				SwingUtils.createPanelGBConstraints(0, row));
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
		if (includeClosedInvoicesModel.getBoolean()) {
			return true;
		} else {
			return !invoice.getRemainingAmountToBePaid(date).isZero();
		}
	}

	private final class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
			updateInvoicePanel();
		}
	}

	private class DatabaseListenerImpl implements DatabaseListener {

		@Override
		public void databaseChanged(Database db) {
			updateInvoicesInPanel();
		}

	}
}
