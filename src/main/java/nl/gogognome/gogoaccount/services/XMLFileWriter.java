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
package nl.gogognome.gogoaccount.services;

import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * This class writes the contents of a <code>Database</code> to an XML file.
 *
 * @author Sander Kooijmans
 */
public class XMLFileWriter {

	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

	private final static AmountFormat AMOUNT_FORMAT = new AmountFormat(Locale.US);

	private static final String FILE_VERSION = "2.2";

	private final Document document;

	private final File file;

	private org.w3c.dom.Document doc;

	public XMLFileWriter(Document document, File file) {
		this.document = document;
		this.file = file;
	}

	public void writeDatabaseToFile() throws ServiceException {
		ServiceTransaction.withoutResult(() -> {
			doc = createDocument();

			Element rootElement = doc.createElement("gogoAccountBookkeeping");
			doc.appendChild(rootElement);

			Bookkeeping bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
			rootElement.setAttribute("fileversion", FILE_VERSION);
			rootElement.setAttribute("description", bookkeeping.getDescription());
			rootElement.setAttribute("currency", bookkeeping.getCurrency().getCurrencyCode());
			rootElement.setAttribute("startdate", DATE_FORMAT.format(bookkeeping.getStartOfPeriod()));

			rootElement.appendChild(createElementForAccounts(new ConfigurationService().findAllAccounts(document)));

			List<Party> parties = ObjectFactory.create(PartyService.class).findAllParties(document);
			rootElement.appendChild(createElementForParties(parties));
			appendElementsForJournals(rootElement);
			rootElement.appendChild(createElementForInvoices());
			rootElement.appendChild(createElementForImportedAccounts());

			writeDomToFile(doc);
		});
	}

	private void appendElementsForJournals(Element rootElement) {
		Element journalsElem = doc.createElement("journals");
		List<Journal> journals = document.getJournals();
		for (Journal journal : journals) {
			Element journalElem = doc.createElement("journal");
			journalElem.setAttribute("id", journal.getId());
			journalElem.setAttribute("date", DATE_FORMAT.format(journal.getDate()));
			journalElem.setAttribute("description", journal.getDescription());
			if (journal.createsInvoice()) {
				journalElem.setAttribute("createdInvoice", journal.getIdOfCreatedInvoice());
			}
			JournalItem[] items = journal.getItems();
            for (JournalItem item1 : items) {
                Element item = doc.createElement("item");
                item.setAttribute("id", item1.getAccount().getId());
                item.setAttribute("amount", AMOUNT_FORMAT.formatAmount(item1.getAmount()));
                item.setAttribute("side", item1.isDebet() ? "debet" : "credit");
                if (item1.getInvoiceId() != null) {
                    item.setAttribute("invoice", item1.getInvoiceId());
                    if (item1.getPaymentId() != null) {
                        item.setAttribute("payment", item1.getPaymentId());
                    }
                }
                journalElem.appendChild(item);
            }
			journalsElem.appendChild(journalElem);
		}
		rootElement.appendChild(journalsElem);
	}

	private void writeDomToFile(org.w3c.dom.Document doc)
			throws TransformerFactoryConfigurationError,
            TransformerException {
		// Use a Transformer for output
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty( OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
	}

	private static org.w3c.dom.Document createDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
        return docBuilder.newDocument();
	}

	private Element createElementForAccounts(List<Account> accounts) {
		Element groupElem = doc.createElement("accounts");
		for (Account account : accounts) {
			Element elem = doc.createElement("account");
			elem.setAttribute("id", account.getId());
			elem.setAttribute("name", account.getName());
			elem.setAttribute("type", account.getType().name());
			groupElem.appendChild(elem);
		}
		return groupElem;
	}

	private Element createElementForParties(List<Party> parties) {
		Element groupElem = doc.createElement("parties");
		for (Party party : parties) {
			Element elem = doc.createElement("party");
			elem.setAttribute("id", party.getId());
			elem.setAttribute("name", party.getName());
			if (party.getAddress() != null) {
				elem.setAttribute("address", party.getAddress());
			}
			if (party.getZipCode() != null) {
				elem.setAttribute("zip", party.getZipCode());
			}
			if (party.getCity() != null) {
				elem.setAttribute("city", party.getCity());
			}
			if (party.getType() != null) {
				elem.setAttribute("type", party.getType());
			}
			if (party.getRemarks() != null) {
				elem.setAttribute("remarks", party.getRemarks());
			}
			if (party.getBirthDate() != null) {
				elem.setAttribute("birthdate", DATE_FORMAT.format(party.getBirthDate()));
			}
			groupElem.appendChild(elem);
		}
		return groupElem;
	}

	private Element createElementForInvoices() {
		Element groupElem = doc.createElement("invoices");
		for (Invoice invoice : document.getInvoices()) {
			Element elem = doc.createElement("invoice");
			elem.setAttribute("id", invoice.getId());
			elem.setAttribute("amountToBePaid", AMOUNT_FORMAT.formatAmount(invoice.getAmountToBePaid()));
			elem.setAttribute("concerningParty", invoice.getConcerningParty().getId());

			Party payingParty = invoice.getPayingParty();
			if (payingParty != null) {
				elem.setAttribute("payingParty", payingParty.getId());
			}

			String[] descriptions = invoice.getDescriptions();
			Amount[] amounts = invoice.getAmounts();
			assert descriptions.length == amounts.length;
			for (int l=0; l<descriptions.length; l++) {
				Element lineElem = doc.createElement("line");
				lineElem.setAttribute("description", descriptions[l]);
				if (amounts[l] != null) {
					lineElem.setAttribute("amount", AMOUNT_FORMAT.formatAmount(amounts[l]));
				}
				elem.appendChild(lineElem);
			}
			elem.setAttribute("issueDate", DATE_FORMAT.format(invoice.getIssueDate()));

			List<Payment> payments = InvoiceService.getPayments(document, invoice.getId());
			for (Payment payment : payments) {
				Element paymentElem = doc.createElement("payment");
				paymentElem.setAttribute("id", payment.getId());
				paymentElem.setAttribute("date", DATE_FORMAT.format(payment.getDate()));
				paymentElem.setAttribute("amount", AMOUNT_FORMAT.formatAmount(payment.getAmount()));
				paymentElem.setAttribute("description", payment.getDescription());
				elem.appendChild(paymentElem);
			}

			groupElem.appendChild(elem);
		}
		return groupElem;
	}

	private Element createElementForImportedAccounts() {
		Element groupElem = doc.createElement("importedaccounts");
		Map<String, String> map = document.getImportedTransactionAccountToAccountMap();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			Element elem = doc.createElement("mapping");
			elem.setAttribute("importedaccount", entry.getKey());
			elem.setAttribute("account", entry.getValue());
			groupElem.appendChild(elem);
		}
		return groupElem;
	}

}
