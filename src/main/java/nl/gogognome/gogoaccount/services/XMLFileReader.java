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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.businessobjects.AccountType;
import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.businessobjects.Party;
import nl.gogognome.gogoaccount.businessobjects.Payment;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.database.DatabaseModificationFailedException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.StringUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;



/**
 * This class reads the contents of a <code>Database</code> from an XML file.
 *
 * @author Sander Kooijmans
 */
public class XMLFileReader {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

    private final static AmountFormat AMOUNT_FORMAT = new AmountFormat(Locale.US);

    private Database database;

    private String fileVersion;

    private final File file;

    public XMLFileReader(File file) {
        this.file = file;
    }

	/**
	 * Creates a <tt>Database</tt> from a file.
	 * @return a <tt>Database</tt> with the contents of the file.
	 * @throws XMLParseException if a syntax error is found in the file.
     * @throws IOException if an I/O problem occurs while reading the file.
	 */
	public Database createDatabaseFromFile() throws XMLParseException, IOException {
		try {
		    int highestPaymentId = 0;

		    database = new Database();
		    database.setFileName(file.getAbsolutePath());

			DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
			Document doc = docBuilder.parse(file);
			Element rootElement = doc.getDocumentElement();

			fileVersion = rootElement.getAttribute("fileversion");

			String description = rootElement.getAttribute("description");
			database.setDescription(description);

			String currency = rootElement.getAttribute("currency");
		    database.setCurrency(Currency.getInstance(currency));

		    Date startDate = DATE_FORMAT.parse(rootElement.getAttribute("startdate"));
		    database.setStartOfPeriod(startDate);

			// parse accounts
			List<Account> assets = parseAccounts(rootElement.getElementsByTagName("assets"),
					AccountType.ASSET);
			database.setAssets(assets);

			List<Account> liabilities = parseAccounts(rootElement.getElementsByTagName("liabilities"),
					AccountType.LIABILITY);
			database.setLiabilities(liabilities);

			List<Account> expenses = parseAccounts(rootElement.getElementsByTagName("expenses"),
					AccountType.EXPENSE);
			database.setExpenses(expenses);

			List<Account> revenues = parseAccounts(rootElement.getElementsByTagName("revenues"),
					AccountType.REVENUE);
			database.setRevenues(revenues);

			parseAndAddParties(rootElement.getElementsByTagName("parties"));

            parseAndAddInvoices(rootElement.getElementsByTagName("invoices"));

		    parseAndAddJournals(highestPaymentId, rootElement);

		    parseAndAddImportedAccounts(rootElement.getElementsByTagName("importedaccounts"));

			return database;
		} catch(Exception e) {
			if (e instanceof XMLParseException) {
				throw (XMLParseException)e;
            } else if (e instanceof IOException) {
                throw (IOException) e;
			} else {
				throw new XMLParseException(e);
			}
		}
	}

	private void parseAndAddJournals(int highestPaymentId, Element rootElement)
			throws ParseException, DatabaseModificationFailedException {
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
		        ArrayList<JournalItem> itemsList = new ArrayList<JournalItem>();
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

		            itemsList.add(new JournalItem(amount, database.getAccount(itemId),
		                "debet".equals(side), invoiceId, paymentId));
		        }

		        JournalItem[] items = itemsList.toArray(new JournalItem[itemsList.size()]);
		        database.addJournal(new Journal(id, description, date, items, idOfCreatedInvoice), false);
		    }
		}

		database.setNextPaymentId("p" + (highestPaymentId + 1));
	}

	/**
	 * Parses accounts.
	 * @param nodes a node list containing accounts
	 * @param type the type of the account
	 * @return the accounts
	 */
	private List<Account> parseAccounts(NodeList nodes, AccountType type) {
	    ArrayList<Account> accounts = new ArrayList<Account>();
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
	 * @return an array of parties found in <code>nodes</code>
     * @throws XMLParseException if a syntax error is found in the nodes
	 * @throws DatabaseModificationFailedException
	 */
	private void parseAndAddParties(NodeList nodes) throws XMLParseException, DatabaseModificationFailedException {
	    ArrayList<Party> parties = new ArrayList<Party>();
	    for (int i=0; i<nodes.getLength(); i++) {
	        Element elem = (Element)nodes.item(i);
	        NodeList partyNodes = elem.getElementsByTagName("party");
	        for (int j=0; j<partyNodes.getLength(); j++) {
	            Element partyElem = (Element)partyNodes.item(j);
	            String id = partyElem.getAttribute("id");
	            String name = partyElem.getAttribute("name");
	            String address = partyElem.getAttribute("address");
	            if (address.length() == 0) {
	                address = null;
	            }
	            String zipCode = partyElem.getAttribute("zip");
	            if (zipCode.length() == 0) {
	                zipCode = null;
	            }
	            String city = partyElem.getAttribute("city");
	            if (city.length() == 0) {
	                city = null;
                }

                String type = partyElem.getAttribute("type");
                if (type.length() == 0) {
                    type = null;
                }

                String remarks = partyElem.getAttribute("remarks");
                if (remarks.length() == 0) {
                    remarks = null;
                }

                String birthDateString = partyElem.getAttribute("birthdate");
                Date birthDate = null;
                if (birthDateString.length() > 0) {
                    try {
                        birthDate = DATE_FORMAT.parse(birthDateString);
                    } catch (java.text.ParseException e) {
                        throw new XMLParseException("Invalid birth date: \"" + birthDateString + "\"");
                    }
                }
		        parties.add(new Party(id, name, address, zipCode, city, birthDate, type, remarks));
	        }
	    }

		database.setParties(parties.toArray(new Party[parties.size()]));
	}

    /**
     * Parses invoices and adds them to the database
     * @param database the database to which the invoices are to be added
     * @param nodes a node list containing invoices.
     * @return an array of invoices found in <code>nodes</code>
     * @throws XMLParseException if a syntax error is found in the nodes
     */
    private void parseAndAddInvoices(NodeList nodes) throws XMLParseException {
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

                Party concerningParty = database.getParty(invoiceElem.getAttribute("concerningParty"));
                if (concerningParty == null) {
                    throw new XMLParseException("No (valid) party specified for the invoice \"" + id + "\"");
                }

                Party payingParty = database.getParty(invoiceElem.getAttribute("payingParty"));

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
                    database.addInvoice(new Invoice(id, payingParty, concerningParty, amountToBePaid,
                        issueDate, descriptions, amounts));
                    for (Payment p : payments) {
                        database.addPayment(id, p);
                    }
                } catch (DatabaseModificationFailedException e) {
                    throw new XMLParseException(e);
                }
            }
        }
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
            	database.setImportedAccount(importedAccount, accountId);
            }
        }
	}

}
