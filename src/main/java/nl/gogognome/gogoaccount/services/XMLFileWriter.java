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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.gogognome.dataaccess.transaction.RunTransaction;
import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.businessobjects.Party;
import nl.gogognome.gogoaccount.businessobjects.Payment;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This class writes the contents of a <code>Database</code> to an XML file.
 *
 * @author Sander Kooijmans
 */
public class XMLFileWriter {

	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

	private final static AmountFormat AMOUNT_FORMAT = new AmountFormat(Locale.US);

	private static final String FILE_VERSION = "2.2";

	private final Database database;

	private final File file;

	private Document doc;

	public XMLFileWriter(Database database, File file) {
		this.database = database;
		this.file = file;
	}

	public void writeDatabaseToFile() throws IOException {
		try	{
			RunTransaction.withoutResult(() -> {
				doc = createDocument();

				Element rootElement = doc.createElement("gogoAccountBookkeeping");
				doc.appendChild(rootElement);

				rootElement.setAttribute("fileversion", FILE_VERSION);
				rootElement.setAttribute("description", database.getDescription());
				rootElement.setAttribute("currency", database.getCurrency().getCurrencyCode());
				rootElement.setAttribute("startdate", DATE_FORMAT.format(database.getStartOfPeriod()));

				rootElement.appendChild(createElementForAccounts(database.getAccountDAO().findAll("id")));

				rootElement.appendChild(createElementForParties(database.getParties()));
				appendElementsForJournals(rootElement);
				rootElement.appendChild(createElementForInvoices());
				rootElement.appendChild(createElementForImportedAccounts());

				writeDomToFile(doc);
			});
		} catch (Exception e) 	{
			throw new IOException("Failed to write the XML file " + file, e);
		}
	}

	private void appendElementsForJournals(Element rootElement) {
		Element journalsElem = doc.createElement("journals");
		List<Journal> journals = database.getJournals();
		for (Journal journal : journals) {
			Element journalElem = doc.createElement("journal");
			journalElem.setAttribute("id", journal.getId());
			journalElem.setAttribute("date", DATE_FORMAT.format(journal.getDate()));
			journalElem.setAttribute("description", journal.getDescription());
			if (journal.createsInvoice()) {
				journalElem.setAttribute("createdInvoice", journal.getIdOfCreatedInvoice());
			}
			JournalItem[] items = journal.getItems();
			for (int j = 0; j < items.length; j++) {
				Element item = doc.createElement("item");
				item.setAttribute("id", items[j].getAccount().getId());
				item.setAttribute("amount", AMOUNT_FORMAT.formatAmount(items[j].getAmount()));
				item.setAttribute("side", items[j].isDebet() ? "debet" : "credit");
				if (items[j].getInvoiceId() != null) {
					item.setAttribute("invoice", items[j].getInvoiceId());
					if (items[j].getPaymentId() != null) {
						item.setAttribute("payment", items[j].getPaymentId());
					}
				}
				journalElem.appendChild(item);
			}
			journalsElem.appendChild(journalElem);
		}
		rootElement.appendChild(journalsElem);
	}

	private void writeDomToFile(Document doc)
			throws TransformerFactoryConfigurationError,
			TransformerConfigurationException, TransformerException {
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

	private static Document createDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		return doc;
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

	private Element createElementForParties(Party[] parties) {
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
		for (Invoice invoice : database.getInvoices()) {
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

			List<Payment> payments = InvoiceService.getPayments(database, invoice.getId());
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
		Map<String, String> map = database.getImportedTransactionAccountToAccountMap();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			Element elem = doc.createElement("mapping");
			elem.setAttribute("importedaccount", entry.getKey());
			elem.setAttribute("account", entry.getValue());
			groupElem.appendChild(elem);
		}
		return groupElem;
	}

}
