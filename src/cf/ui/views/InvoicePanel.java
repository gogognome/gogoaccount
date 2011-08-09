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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Currency;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;
import cf.engine.Invoice;
import cf.engine.Payment;

/**
 * This class implements a component that displays an invoice.
 * @author Sander Kooijmans
 */
class InvoicePanel extends JPanel {

	private final static Color CLOSED_INVOICE_COLOR = new Color(128, 255, 128);
	private final static Color OPEN_INVOICE_COLOR = new Color(255, 128, 128);

	private Invoice invoice;
	private JPanel linesPanel;
	private int row;
	private Date date;
	private Amount totalDebet;
	private Amount totalCredit;

    private TextResource textResource = Factory.getInstance(TextResource.class);
    private WidgetFactory widgetFactory = Factory.getInstance(WidgetFactory.class);
	private AmountFormat amountFormat = Factory.getInstance(AmountFormat.class);

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
		JPanel titlePanel = new JPanel(new GridBagLayout());
		titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		titlePanel.setBackground(
				isInvoicePaid() ? CLOSED_INVOICE_COLOR : OPEN_INVOICE_COLOR);
		StringBuilder sb = new StringBuilder(invoice.getId());
		Amount[] amounts = invoice.getAmounts();
		String[] descriptions = invoice.getDescriptions();
		for (int i=0; i<amounts.length; i++) {
			if (amounts[i] == null) {
				sb.append(' ').append(descriptions[i]);
			}
		}

		if (mustPayingPartyBeShown()) {
			sb.append(' ');
			String payingParty = invoice.getPayingParty().getId() + ' '
				+ invoice.getPayingParty().getName();
			sb.append(textResource.getString("InvoicesSinglePartyView.paidBy", payingParty));
		}

		titlePanel.add(new JLabel(sb.toString()), SwingUtils.createLabelGBConstraints(0, 0));
		titlePanel.add(new JLabel(""), SwingUtils.createTextFieldGBConstraints(1, 0));

		String amountText;
		if (!invoice.getAmountToBePaid().isNegative()) {
			amountText = textResource.getString("InvoicesSinglePartyView.debetInvoice",
				amountFormat.formatAmount(invoice.getAmountToBePaid()));
		} else {
			amountText = textResource.getString("InvoicesSinglePartyView.creditInvoice",
					amountFormat.formatAmount(invoice.getAmountToBePaid().negate()));
		}
		titlePanel.add(new JLabel(amountText), SwingUtils.createLabelGBConstraints(2, 0));
		return titlePanel;
	}

	private boolean mustPayingPartyBeShown() {
		return !isInvoicePaid()
			&& !invoice.getConcerningParty().equals(invoice.getPayingParty())
			&& invoice.getPayingParty() != null;
	}

	private boolean isInvoicePaid() {
		return totalDebet.equals(totalCredit);
	}

	private void addHeader() {
		int col = 0;
		addToLinePanel(col++, widgetFactory.createLabel("gen.date"));
		addToLinePanel(col++, widgetFactory.createLabel("gen.description"));

		String debetId;
		String creditId;
		if (!invoice.getAmountToBePaid().isNegative()) {
			debetId = "InvoicesSinglePartyView.toReceive";
			creditId = "InvoicesSinglePartyView.received";
		} else {
			debetId = "InvoicesSinglePartyView.paid";
			creditId = "InvoicesSinglePartyView.toPay";
		}

		addToLinePanel(col++, widgetFactory.createLabel(debetId));
		addToLinePanel(col++, widgetFactory.createLabel(creditId));
		row++;
	}

	private void addDescriptionLines() {
		Amount[] amounts = invoice.getAmounts();
		String[] descriptions = invoice.getDescriptions();
		for (int i=0; i<amounts.length; i++) {
			if (amounts[i] != null) {
				int col = 0;
				addToLinePanel(col++, new JLabel(
						textResource.formatDate("gen.dateFormat", invoice.getIssueDate())));

				addToLinePanel(col++, new JLabel(descriptions[i]));

				String debet = amounts[i].isPositive() ? amountFormat.formatAmount(amounts[i]) : "";
				addToLinePanel(col++, new JLabel(debet));

				String credit = amounts[i].isNegative() ? amountFormat.formatAmount(amounts[i].negate()) : "";
				addToLinePanel(col++, new JLabel(credit));

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
		for (Payment payment : invoice.getPayments()) {
			if (DateUtil.compareDayOfYear(payment.getDate(), date) <= 0) {
				int col = 0;
				addToLinePanel(col++, new JLabel(
						textResource.formatDate("gen.dateFormat", payment.getDate())));

				addToLinePanel(col++, new JLabel(payment.getDescription()));
				Amount amount = payment.getAmount();

				String debet = amount.isNegative() ? amountFormat.formatAmount(amount.negate()) : "";
				addToLinePanel(col++, new JLabel(debet));

				String credit = amount.isPositive() ? amountFormat.formatAmount(amount) : "";
				addToLinePanel(col++, new JLabel(credit));

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
		int col = 1;
		addToLinePanel(col++, widgetFactory.createLabel("gen.total"));
		addToLinePanel(col++, new JLabel(amountFormat.formatAmount(totalDebet)));
		addToLinePanel(col++, new JLabel(amountFormat.formatAmount(totalCredit)));
		row++;
	}

	private void addToLinePanel(int col, Component c) {
		GridBagConstraints constraints;
		if (col == 1) {
			constraints = SwingUtils.createGBConstraints(col, row, 1, 1, 4.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 0, 0);
		} else {
			constraints = SwingUtils.createGBConstraints(col, row);
		}
		linesPanel.add(c, constraints);
	}
}