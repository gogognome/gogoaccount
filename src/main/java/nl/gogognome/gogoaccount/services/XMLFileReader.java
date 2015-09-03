package nl.gogognome.gogoaccount.services;

import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.StringUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class reads the contents of a <code>Database</code> from an XML file.
 */
public class XMLFileReader {

	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

	private final static AmountFormat AMOUNT_FORMAT = new AmountFormat(Locale.US);

    private final BookkeepingService bookkeepingService = ObjectFactory.create(BookkeepingService.class);
    private final ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
	private final PartyService partyService = ObjectFactory.create(PartyService.class);

	private Document document;
	private String fileVersion;
	private final File file;

	public XMLFileReader(File file) {
		this.file = file;
	}

	/**
	 * Creates a <tt>Database</tt> from a file.
	 * @return a <tt>Database</tt> with the contents of the file.
	 * @throws ServiceException if a problem occurs
	 */
	public Document createDatabaseFromFile() throws ServiceException {
		return ServiceTransaction.withResult(() -> {
			int highestPaymentId = 0;

			document = bookkeepingService.createNewDatabase("New bookkeeping");
			document.setFileName(file.getAbsolutePath());

			DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
			org.w3c.dom.Document doc = docBuilder.parse(file);
			Element rootElement = doc.getDocumentElement();

			fileVersion = rootElement.getAttribute("fileversion");

			String description = rootElement.getAttribute("description");

			Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
			bookkeeping.setDescription(description);
			bookkeeping.setCurrency(Currency.getInstance(rootElement.getAttribute("currency")));
            bookkeeping.setStartOfPeriod(DATE_FORMAT.parse(rootElement.getAttribute("startdate")));
            configurationService.updateBookkeeping(document, bookkeeping);

			// parse accounts
			if (fileVersion == null || fileVersion.equals("1.0")) {
				parseAccountsBeforeVersion2_2(rootElement);
			} else {
				parseAccountsForVersion2_2(rootElement);
			}

			parseAndAddParties(rootElement.getElementsByTagName("parties"));

			parseAndAddInvoices(rootElement.getElementsByTagName("invoices"));

			parseAndAddJournals(highestPaymentId, rootElement);

			parseAndAddImportedAccounts(rootElement.getElementsByTagName("importedaccounts"));

			return document;
		});
	}

	private void parseAccountsForVersion2_2(Element rootElement) throws ServiceException {
        NodeList nodes = rootElement.getElementsByTagName("accounts");
		for (int i=0; i<nodes.getLength(); i++)
		{
			Element elem = (Element)nodes.item(i);
			NodeList accountNodes = elem.getElementsByTagName("account");
			for (int j=0; j<accountNodes.getLength(); j++)
			{
				Element accountElem = (Element)accountNodes.item(j);
				String id = accountElem.getAttribute("id");
				String name = accountElem.getAttribute("name");
				AccountType type = AccountType.valueOf(accountElem.getAttribute("type"));
                configurationService.createAccount(document, new Account(id, name, type));
			}
		}
	}

	private void parseAccountsBeforeVersion2_2(Element rootElement) throws ServiceException {
		List<Account> assets = parseAccounts(rootElement.getElementsByTagName("assets"), AccountType.ASSET);
		for (Account account : assets) {
			configurationService.createAccount(document, account);
        }

		List<Account> liabilities = parseAccounts(rootElement.getElementsByTagName("liabilities"), AccountType.LIABILITY);
        for (Account account : liabilities) {
            configurationService.createAccount(document, account);
        }

		List<Account> expenses = parseAccounts(rootElement.getElementsByTagName("expenses"), AccountType.EXPENSE);
        for (Account account : expenses) {
            configurationService.createAccount(document, account);
        }

		List<Account> revenues = parseAccounts(rootElement.getElementsByTagName("revenues"), AccountType.REVENUE);
        for (Account account : revenues) {
            configurationService.createAccount(document, account);
        }
    }

	private void parseAndAddJournals(int highestPaymentId, Element rootElement)
            throws ParseException, DocumentModificationFailedException, ServiceException {
		String description;
		NodeList journalsNodes = rootElement.getElementsByTagName("journals");
		for (int i=0; i<journalsNodes.getLength(); i++) {
			Element elem = (Element)journalsNodes.item(i);
			NodeList journalNodes = elem.getElementsByTagName("journal");
			for (int j=0; j<journalNodes.getLength(); j++) {
				Element journalElem = (Element)journalNodes.item(j);
				String id = journalElem.getAttribute("id");
				String dateString = journalElem.getAttribute("date");
				Date date = DATE_FORMAT.parse(dateString);
				String idOfCreatedInvoice = journalElem.getAttribute("createdInvoice");
				if (idOfCreatedInvoice.length() == 0) {
					idOfCreatedInvoice = null;
				}
				description = journalElem.getAttribute("description");
				NodeList itemNodes = journalElem.getElementsByTagName("item");
				ArrayList<JournalItem> itemsList = new ArrayList<>();
				for (int k=0; k<itemNodes.getLength(); k++) {
					Element itemElem = (Element)itemNodes.item(k);
					String itemId = itemElem.getAttribute("id");
					String amountString = itemElem.getAttribute("amount");
					Amount amount = AMOUNT_FORMAT.parse(amountString);
					String side = itemElem.getAttribute("side");
					String invoiceId = itemElem.getAttribute("invoice");
					if (invoiceId.length() == 0) {
						invoiceId = null;
					}
					String paymentId = itemElem.getAttribute("payment");
					if (paymentId.length() == 0) {
						paymentId = null;
					} else {
						if (paymentId.matches("p\\d+")) {
							int pid = Integer.parseInt(paymentId.substring(1));
							highestPaymentId = Math.max(highestPaymentId, pid);
						}
					}

					itemsList.add(new JournalItem(amount, configurationService.getAccount(document, itemId),
							"debet".equals(side), invoiceId, paymentId));
				}

				JournalItem[] items = itemsList.toArray(new JournalItem[itemsList.size()]);
				document.addJournal(new Journal(id, description, date, items, idOfCreatedInvoice), false);
			}
		}

		document.setNextPaymentId("p" + (highestPaymentId + 1));
	}

	private List<Account> parseAccounts(NodeList nodes, AccountType type) {
		ArrayList<Account> accounts = new ArrayList<>();
		for (int i=0; i<nodes.getLength(); i++)
		{
			Element elem = (Element)nodes.item(i);
			NodeList accountNodes = elem.getElementsByTagName("account");
			for (int j=0; j<accountNodes.getLength(); j++)
			{
				Element accountElem = (Element)accountNodes.item(j);
				String id = accountElem.getAttribute("id");
				String name = accountElem.getAttribute("name");
				accounts.add(new Account(id, name, type));
			}
		}
		return accounts;
	}

	/**
	 * Parses parties.
	 * @param nodes a node list containing parties.
	 */
	private void parseAndAddParties(NodeList nodes) throws XMLParseException, ServiceException {
        for (int i=0; i<nodes.getLength(); i++) {
			Element elem = (Element)nodes.item(i);
			NodeList partyNodes = elem.getElementsByTagName("party");
			for (int j=0; j<partyNodes.getLength(); j++) {
				Element partyElem = (Element)partyNodes.item(j);
				Party party = new Party(partyElem.getAttribute("id"));
				party.setName(partyElem.getAttribute("name"));
				party.setAddress(emptyToNull(partyElem.getAttribute("address")));
                party.setZipCode(emptyToNull(partyElem.getAttribute("zip")));
                party.setCity(emptyToNull(partyElem.getAttribute("city")));
                party.setType(emptyToNull(partyElem.getAttribute("type")));
                party.setRemarks(emptyToNull(partyElem.getAttribute("remarks")));

				String birthDateString = partyElem.getAttribute("birthdate");
                if (birthDateString.length() > 0) {
					try {
						party.setBirthDate(DATE_FORMAT.parse(birthDateString));
					} catch (java.text.ParseException e) {
						throw new XMLParseException("Invalid birth date: \"" + birthDateString + "\"");
                    }
                }
                partyService.createParty(document, party);
			}
		}
	}

	/**
	 * Parses invoices and adds them to the database
	 * @param nodes a node list containing invoices.
	 * @throws XMLParseException if a syntax error is found in the nodes
	 */
	private void parseAndAddInvoices(NodeList nodes) throws XMLParseException, ServiceException {
		for (int i=0; i<nodes.getLength(); i++) {
			Element elem = (Element)nodes.item(i);
			NodeList invoiceNodes = elem.getElementsByTagName("invoice");
			for (int j=0; j<invoiceNodes.getLength(); j++) {
				Element invoiceElem = (Element)invoiceNodes.item(j);
				String id = invoiceElem.getAttribute("id");
				Amount amountToBePaid;
				try {
					amountToBePaid = AMOUNT_FORMAT.parse(invoiceElem.getAttribute("amountToBePaid"));
				} catch (ParseException e) {
					throw new XMLParseException("Invalid amount: " + invoiceElem.getAttribute("amountToBePaid"));
				}

				Party concerningParty = partyService.getParty(document, invoiceElem.getAttribute("concerningParty"));
				if (concerningParty == null) {
					throw new XMLParseException("No (valid) party specified for the invoice \"" + id + "\"");
				}

				Party payingParty = partyService.getParty(document, invoiceElem.getAttribute("payingParty"));

				NodeList lineNodes = invoiceElem.getElementsByTagName("line");
				int numNodes = lineNodes.getLength();
				String[] descriptions = new String[numNodes];
				Amount[] amounts = new Amount[numNodes];
				for (int l=0; l<numNodes; l++) {
					Element lineElem = (Element)lineNodes.item(l);
					descriptions[l] = lineElem.getAttribute("description");
					String amountString = lineElem.getAttribute("amount");
					if (amountString != null && amountString.length() > 0) {
						try {
							amounts[l] = AMOUNT_FORMAT.parse(amountString);
						} catch (ParseException e) {
							throw new XMLParseException("Invalid amount: " + amountString);
						}
					}
				}

				Date issueDate;
				try {
					issueDate = DATE_FORMAT.parse(invoiceElem.getAttribute("issueDate"));
				} catch (ParseException e2) {
					throw new XMLParseException("Invalid date: " + invoiceElem.getAttribute("issueDate"));
				}

				NodeList paymentNodes = invoiceElem.getElementsByTagName("payment");
				numNodes = paymentNodes.getLength();
				Payment[] payments = new Payment[numNodes];
				for (int p=0; p<numNodes; p++) {
					Element paymentElem = (Element)paymentNodes.item(p);
					String paymentId;
					Amount amount;
					Date date;
					String description;
					paymentId = paymentElem.getAttribute("id");
					if (StringUtil.isNullOrEmpty(paymentId)) {
						paymentId = "p" + id + "-" + p;
					}

					try {
						date = DATE_FORMAT.parse(paymentElem.getAttribute("date"));
					} catch (ParseException e1) {
						throw new XMLParseException("Invalid date: " + paymentElem.getAttribute("date"));
					}
					try {
						amount = AMOUNT_FORMAT.parse(paymentElem.getAttribute("amount"));
					} catch (ParseException e) {
						throw new XMLParseException("Invalid amount: " + paymentElem.getAttribute("amount"));
					}
					description = paymentElem.getAttribute("description");
					payments[p] = new Payment(paymentId, amount, date, description);
				}

				try {
					document.addInvoice(new Invoice(id, payingParty, concerningParty, amountToBePaid,
							issueDate, descriptions, amounts));
					for (Payment p : payments) {
						document.addPayment(id, p);
					}
				} catch (DocumentModificationFailedException e) {
					throw new XMLParseException(e);
				}
			}
		}
	}

    private static String emptyToNull(String s) {
        return s == null || !s.isEmpty() ? s : null;
    }

	private void parseAndAddImportedAccounts(NodeList nodes) {
		if (StringUtil.isNullOrEmpty(fileVersion)) {
			return;
		}

		for (int i=0; i<nodes.getLength(); i++) {
			Element elem = (Element)nodes.item(i);
			NodeList mappingNodes = elem.getElementsByTagName("mapping");
			for (int j=0; j<mappingNodes.getLength(); j++) {
				Element element = (Element) mappingNodes.item(j);
				String importedAccount = element.getAttribute("importedaccount");
				String accountId = element.getAttribute("account");
				document.setImportedAccount(importedAccount, accountId);
			}
		}
	}

}
