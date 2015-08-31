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

import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Payment;
import nl.gogognome.gogoaccount.services.InvoiceService;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.lib.util.StringUtil;

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
		putNullable(model, "concerning", parameters.getConcerning());
		putNullable(model, "date", textResource.formatDate("gen.dateFormatFull", parameters.getDate()));
		putNullable(model, "dueDate", textResource.formatDate("gen.dateFormatFull", parameters.getDueDate()));
		putNullable(model, "ourReference", parameters.getOurReference());
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

		putNullable(map, "id", invoice.getId());

		putNullable(map, "partyName", invoice.getConcerningParty().getName());
		putNullable(map, "partyAddress", invoice.getConcerningParty().getAddress());
		putNullable(map, "partyZip", invoice.getConcerningParty().getZipCode());
		putNullable(map, "partyCity", invoice.getConcerningParty().getCity());

		Amount totalAmount = InvoiceService.getRemainingAmountToBePaid(
				parameters.getDocument(), invoice.getId(), parameters.getDate());
		putNullable(map, "totalAmount", amountFormat.formatAmount(totalAmount));

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

		List<Payment> payments = InvoiceService.getPayments(parameters.getDocument(), invoice.getId());
		for (Payment p : payments) {
			lines.add(createLine(p.getDate(), p.getDescription(), p.getAmount().negate()));
		}

		return lines;
	}

	private Map<String, Object> createLine(Date date, String description, Amount amount) {
		Map<String, Object> map = new HashMap<String, Object>();

		putNullable(map, "date", textResource.formatDate("gen.dateFormat", date));
		putNullable(map, "description", description);
		putNullable(map, "amount", amountFormat.formatAmount(amount));

		return map;
	}

	private void putNullable(Map<String, Object> map, String key, String nullableValue) {
		map.put(key, StringUtil.nullToEmptyString(nullableValue));
	}
}
