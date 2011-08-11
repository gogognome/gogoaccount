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
package nl.gogognome.gogoaccount.reportgenerators;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import cf.engine.Invoice;
import cf.engine.Payment;

/**
 * Converts invoices to a model for ODT generation.
 *
 * @author Sander Kooijmans
 */
public class InvoicesToModelConverter {

	private final OdtInvoiceParameters parameters;

    private Map<String, Object> model;

    private TextResource textResource = Factory.getInstance(TextResource.class);
    private AmountFormat amountFormat = Factory.getInstance(AmountFormat.class);

	public InvoicesToModelConverter(OdtInvoiceParameters parameters) {
		super();
		this.parameters = parameters;

		createModel();
	}

	public Map<String, Object> getModel() {
		return model;
	}

	private void createModel() {
		model = new HashMap<String, Object>();

		addGeneralProperties();
		addInvoicesToModel();
	}

	private void addGeneralProperties() {
		model.put("concerning", parameters.getConcerning());
		model.put("date", textResource.formatDate("gen.dateFormatFull", parameters.getDate()));
		model.put("dueDate", textResource.formatDate("gen.dateFormatFull", parameters.getDueDate()));
		model.put("ourReference", parameters.getOurReference());
	}

	private void addInvoicesToModel() {
		List<Map<String, Object>> invoiceModels = new ArrayList<Map<String, Object>>();
		for (Invoice invoice : parameters.getInvoices()) {
			invoiceModels.add(createInvoiceModel(invoice));
		}
		model.put("invoices", invoiceModels);
	}

	private Map<String, Object> createInvoiceModel(Invoice invoice) {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("ourReference", invoice.getId());

		map.put("partyName", invoice.getConcerningParty().getName());
		map.put("partyAddress", invoice.getConcerningParty().getName());
		map.put("partyZip", invoice.getConcerningParty().getName());
		map.put("partyCity", invoice.getConcerningParty().getName());

		map.put("lines", createLinesForInvoice(invoice));

		return map;
	}

	private Object createLinesForInvoice(Invoice invoice) {
		List<Map<String, Object>> lines = new ArrayList<Map<String,Object>>();

		String[] descriptions = invoice.getDescriptions();
		Amount[] amounts = invoice.getAmounts();
		for (int i=0; i<descriptions.length; i++) {
			lines.add(createLine(invoice.getIssueDate(), descriptions[i], amounts[i]));
		}

		for (Payment p : invoice.getPayments()) {
			lines.add(createLine(p.getDate(), p.getDescription(), p.getAmount().negate()));
		}

		return lines;
	}

	private Map<String, Object> createLine(Date date, String description, Amount amount) {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("date", textResource.formatDate("gen.dateFormat", date));
		map.put("description", description);
		map.put("amount", amountFormat.formatAmount(amount));

		return map;
	}

}
