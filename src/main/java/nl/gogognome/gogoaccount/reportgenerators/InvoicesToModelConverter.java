package nl.gogognome.gogoaccount.reportgenerators;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
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

	private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

	private final OdtInvoiceParameters parameters;

    private Map<String, Object> model;

    private TextResource textResource = Factory.getInstance(TextResource.class);
    private AmountFormat amountFormat = Factory.getInstance(AmountFormat.class);

	public InvoicesToModelConverter(OdtInvoiceParameters parameters) throws ServiceException {
		super();
		this.parameters = parameters;

		createModel();
	}

	public Map<String, Object> getModel() {
		return model;
	}

	private void createModel() throws ServiceException {
		model = new HashMap<>();

		addGeneralProperties();
		addInvoicesToModel();
	}

	private void addGeneralProperties() {
		putNullable(model, "concerning", parameters.getConcerning());
		putNullable(model, "date", textResource.formatDate("gen.dateFormatFull", parameters.getDate()));
		putNullable(model, "dueDate", textResource.formatDate("gen.dateFormatFull", parameters.getDueDate()));
		putNullable(model, "ourReference", parameters.getOurReference());
	}

	private void addInvoicesToModel() throws ServiceException {
		List<Map<String, Object>> invoiceModels = new ArrayList<>();
		for (Invoice invoice : parameters.getInvoices()) {
			invoiceModels.add(createInvoiceModel(invoice));
		}
		model.put("invoices", invoiceModels);
	}

	private Map<String, Object> createInvoiceModel(Invoice invoice) throws ServiceException {
		Map<String, Object> map = new HashMap<>();

		putNullable(map, "id", invoice.getId());

        Party party = partyService.getParty(parameters.getDocument(), invoice.getConcerningPartyId());
		putNullable(map, "partyName", party.getName());
		putNullable(map, "partyAddress", party.getAddress());
		putNullable(map, "partyZip", party.getZipCode());
		putNullable(map, "partyCity", party.getCity());

		Amount totalAmount = invoiceService.getRemainingAmountToBePaid(
				parameters.getDocument(), invoice.getId(), parameters.getDate());
		putNullable(map, "totalAmount", amountFormat.formatAmount(totalAmount));

		map.put("lines", createLinesForInvoice(invoice));

		return map;
	}

	private Object createLinesForInvoice(Invoice invoice) throws ServiceException {
		List<Map<String, Object>> lines = new ArrayList<>();

        List<String> descriptions = invoiceService.findDescriptions(parameters.getDocument(), invoice);
		List<Amount> amounts = invoiceService.findAmounts(parameters.getDocument(), invoice);
		for (int i=0; i<descriptions.size(); i++) {
			lines.add(createLine(invoice.getIssueDate(), descriptions.get(i), amounts.get(i)));
		}

		List<Payment> payments = invoiceService.findPayments(parameters.getDocument(), invoice);
		for (Payment p : payments) {
			lines.add(createLine(p.getDate(), p.getDescription(), p.getAmount().negate()));
		}

		return lines;
	}

	private Map<String, Object> createLine(Date date, String description, Amount amount) {
		Map<String, Object> map = new HashMap<>();

		putNullable(map, "date", textResource.formatDate("gen.dateFormat", date));
		putNullable(map, "description", description);
		putNullable(map, "amount", amountFormat.formatAmount(amount));

		return map;
	}

	private void putNullable(Map<String, Object> map, String key, String nullableValue) {
		map.put(key, StringUtil.nullToEmptyString(nullableValue));
	}
}
