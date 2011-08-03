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
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import nl.gogognome.gogoaccount.gui.beans.PartyBean;
import nl.gogognome.gogoaccount.models.PartyModel;
import nl.gogognome.lib.awt.layout.VerticalLayout;
import nl.gogognome.lib.gui.beans.ValuesEditPanel;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Party;
import cf.engine.Payment;

/**
 * This view shows all invoices for a single party.
 *
 * @author Sander Kooijmans
 */
public class InvoicesSinglePartyView extends View {

	private final static Color CLOSED_INVOICE_COLOR = new Color(128, 255, 128);
	private final static Color OPEN_INVOICE_COLOR = new Color(255, 128, 128);

	private static final long serialVersionUID = 1L;

	private Database database;

	private JScrollPane tableScrollPane;
	private JPanel invoicesPanel;

	private PartyModel partyModel;
	private DateModel dateModel;
	private ModelChangeListener listener;

	public InvoicesSinglePartyView(Database database) {
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
	}

	private void addComponents() {
		JPanel partyAndDatePanel = new JPanel(new FlowLayout());
		ValuesEditPanel vep = new ValuesEditPanel();
		addCloseable(vep);
		vep.addField("InvoicesSinglePartyView.party", new PartyBean(database, partyModel));
		partyAndDatePanel.add(vep);

		vep = new ValuesEditPanel();
		addCloseable(vep);
		vep.addField("InvoicesSinglePartyView.date", dateModel);
		partyAndDatePanel.add(vep);

		setLayout(new GridBagLayout());
		add(partyAndDatePanel, SwingUtils.createGBConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                10, 10, 10, 10));

		invoicesPanel = new JPanel(new VerticalLayout(10, VerticalLayout.BOTH));
		invoicesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		invoicesPanel.setBackground(Color.WHITE);
		tableScrollPane = widgetFactory.createScrollPane(invoicesPanel);
		tableScrollPane.setBorder(widgetFactory.createTitleBorder("InvoicesSinglePartyView.initialPanelTitle"));
		add(tableScrollPane, SwingUtils.createPanelGBConstraints(0, 1));
	}

	@Override
	public void onClose() {
		removeListeners();
	}

	private void addListeners() {
		listener = new ModelChangeListenerImpl();
		partyModel.addModelChangeListener(listener);
		dateModel.addModelChangeListener(listener);
	}

	private void removeListeners() {
		dateModel.removeModelChangeListener(listener);
		partyModel.removeModelChangeListener(listener);
	}

	private void updateInvoicePanel() {
		updateTitleBorder();
		updateInvoices();
	}

	private void updateTitleBorder() {
		Date date = dateModel.getDate();
		Party party = partyModel.getParty();

		if (party != null && date != null) {
			tableScrollPane.setBorder(widgetFactory.createTitleBorder("InvoicesSinglePartyView.invoicesForParty",
			        party.getId() + " - " + party.getName(),
			        textResource.formatDate("gen.dateFormat", date)));
		} else {
			tableScrollPane.setBorder(widgetFactory.createTitleBorder("InvoicesSinglePartyView.initialPanelTitle"));
		}
	}

	private void updateInvoices() {
		Date date = dateModel.getDate();
		Party party = partyModel.getParty();

		invoicesPanel.removeAll();

		if (party == null || date == null) {
			return;
		}

        Invoice[] invoices = database.getInvoices();

        // Sort invoices on date
        Arrays.sort(invoices, new Comparator<Invoice>() {
            @Override
			public int compare(Invoice o1, Invoice o2) {
                return DateUtil.compareDayOfYear(o1.getIssueDate(), o2.getIssueDate());
            }
        });

        int row = 0;
        for (Invoice invoice : invoices) {
        	if (party.equals(invoice.getPayingParty())
        			&& DateUtil.compareDayOfYear(invoice.getIssueDate(), date) <= 0) {
        		invoicesPanel.add(new InvoicePanel(invoice, date, database.getCurrency()),
        				SwingUtils.createPanelGBConstraints(0, row));
        		row++;
        	}
        }

        invoicesPanel.validate();
	}

	private final class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
			updateInvoicePanel();
		}
	}

	private static class InvoicePanel extends JPanel {

		private Invoice invoice;
		private JPanel linesPanel;
		private int row;
		private Date date;
		private Amount totalDebet;
		private Amount totalCredit;

		public InvoicePanel(Invoice invoice, Date date, Currency currency) {
			super(new BorderLayout());
			this.invoice = invoice;
			this.date = date;
			this.totalDebet = Amount.getZero(currency);
			this.totalCredit = Amount.getZero(currency);

			linesPanel = new JPanel(new GridBagLayout());
			linesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			addHeader();
			addDescriptionLines();
			addPayments();
			addFooter();
			add(linesPanel, BorderLayout.CENTER);

			add(createTitleBar(), BorderLayout.NORTH);
		}

		private JPanel createTitleBar() {
			AmountFormat af = Factory.getInstance(AmountFormat.class);
			JPanel titlePanel = new JPanel(new GridBagLayout());
			titlePanel.setBackground(
					totalDebet.equals(totalCredit) ? CLOSED_INVOICE_COLOR : OPEN_INVOICE_COLOR);
			StringBuilder sb = new StringBuilder(invoice.getId());
			Amount[] amounts = invoice.getAmounts();
			String[] descriptions = invoice.getDescriptions();
			for (int i=0; i<amounts.length; i++) {
				if (amounts[i] == null) {
					sb.append("   ").append(descriptions[i]);
				}
			}
			sb.append("   ").append(af.formatAmount(invoice.getAmountToBePaid()));
			titlePanel.add(new JLabel(sb.toString()), SwingUtils.createLabelGBConstraints(0, 0));
			titlePanel.add(new JLabel(""), SwingUtils.createTextFieldGBConstraints(1, 0));
			return titlePanel;
		}

		private void addHeader() {
			WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
			linesPanel.add(wf.createLabel("gen.description"),
					SwingUtils.createGBConstraints(0, row, 1, 1, 4.0, 1.0,
							GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 0, 0));
			linesPanel.add(wf.createLabel("gen.debet"), SwingUtils.createGBConstraints(1, row));
			linesPanel.add(wf.createLabel("gen.credit"), SwingUtils.createGBConstraints(2, row));
			row++;
		}

		private void addDescriptionLines() {
			Amount[] amounts = invoice.getAmounts();
			String[] descriptions = invoice.getDescriptions();
			AmountFormat af = Factory.getInstance(AmountFormat.class);
			for (int i=0; i<amounts.length; i++) {
				if (amounts[i] != null) {
					addToLinePanel(0, new JLabel(descriptions[i]));

					String debet = amounts[i].isPositive() ? af.formatAmount(amounts[i]) : "";
					addToLinePanel(1, new JLabel(debet));

					String credit = amounts[i].isNegative() ? af.formatAmount(amounts[i].negate()) : "";
					addToLinePanel(2, new JLabel(credit));

					row++;

					if (amounts[i].isPositive()) {
						totalDebet = totalDebet.add(amounts[i]);
					} else {
						totalCredit = totalCredit.subtract(amounts[i]);
					}
				}
			}
		}

		private void addPayments() {
			AmountFormat af = Factory.getInstance(AmountFormat.class);
			for (Payment payment : invoice.getPayments()) {
				if (DateUtil.compareDayOfYear(payment.getDate(), date) <= 0) {
					addToLinePanel(0, new JLabel(payment.getDescription()));
					Amount amount = payment.getAmount();

					String debet = amount.isNegative() ? af.formatAmount(amount.negate()) : "";
					addToLinePanel(1, new JLabel(debet));

					String credit = amount.isPositive() ? af.formatAmount(amount) : "";
					addToLinePanel(2, new JLabel(credit));

					row++;

					if (amount.isNegative()) {
						totalDebet = totalDebet.subtract(amount);
					} else {
						totalCredit = totalCredit.add(amount);
					}
				}
			}
		}

		private void addFooter() {
			WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
			AmountFormat af = Factory.getInstance(AmountFormat.class);

			addToLinePanel(0, wf.createLabel("gen.total"));
			addToLinePanel(1, new JLabel(af.formatAmount(totalDebet)));
			addToLinePanel(2, new JLabel(af.formatAmount(totalCredit)));
			row++;
		}

		private void addToLinePanel(int col, Component c) {
			GridBagConstraints constraints;
			if (col == 0) {
				constraints = SwingUtils.createGBConstraints(col, row, 1, 1, 4.0, 1.0,
						GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 0, 0);
			} else {
				constraints = SwingUtils.createGBConstraints(col, row);
			}
			linesPanel.add(c, constraints);
		}
	}

}
