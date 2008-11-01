/*
 * $Id: XMLFileReader.java,v 1.22 2008-11-01 13:26:02 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.util.StringUtil;

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
    
    private XMLFileReader() {
        // should never be called
    }

	/**
	 * Creates a <tt>Database</tt> from a file.
	 * 
	 * @param fileName the name of the file.
	 * @return a <tt>Database</tt> with the contents of the file.
	 * @throws XMLParseException if a syntax error is found in the file.
     * @throws IOException if an I/O problem occurs while reading the file.
	 */
	public static Database createDatabaseFromFile(String fileName) throws XMLParseException, IOException {
		try 
		{
		    Database db = new Database();
		    db.setFileName(fileName);
		    
			DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
			Document doc = docBuilder.parse(fileName);			
			Element rootElement = doc.getDocumentElement();

			// parse description
			String description = rootElement.getAttribute("description");
			db.setDescription(description);
			
			// parse currency
			String currency = rootElement.getAttribute("currency");
		    db.setCurrency(Currency.getInstance(currency));

			// parse start date
		    Date startDate = DATE_FORMAT.parse(rootElement.getAttribute("startdate"));
		    db.setStartOfPeriod(startDate);
			
			// parse accounts
			Account[] assets = 
			    parseAccounts(rootElement.getElementsByTagName("assets"), true, db); 
			db.setAssets(assets);
			
			Account[] liabilities = 
			    parseAccounts(rootElement.getElementsByTagName("liabilities"), false, db); 
			db.setLiabilities(liabilities);
			
			Account[] expenses = 
			    parseAccounts(rootElement.getElementsByTagName("expenses"), true, db); 
			db.setExpenses(expenses);
			
			Account[] revenues = parseAccounts(rootElement.getElementsByTagName("revenues"), false, db); 
			db.setRevenues(revenues);
			
			Party[] parties = parseParties(rootElement.getElementsByTagName("parties"));
			db.setParties(parties);
		    
            Invoice[] invoices = parseInvoices(parties, rootElement.getElementsByTagName("invoices"));
            db.setInvoices(invoices);
            
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
		                String invoiceString = itemElem.getAttribute("invoice");
		                if (invoiceString.length() == 0) {
		                    invoiceString = null;
		                }
		             // TODO: old code. remove later
		                if ("true".equals(itemElem.getAttribute("invoiceCreation"))) {
	                        idOfCreatedInvoice = invoiceString;
	                        invoiceString = null;
		                }
		                // end of old code
		                
		                itemsList.add(new JournalItem(amount, db.getAccount(itemId), 
	                        "debet".equals(side), invoiceString));
		            }
		            
		            JournalItem[] items = itemsList.toArray(new JournalItem[itemsList.size()]);
			        db.addJournal(new Journal(id, description, date, items, idOfCreatedInvoice), false);
		        }
		    }
		    
			return db;
		} 
		catch(Exception e) 
		{
			//logger.warn("Exception occurred while parsing XML file.", e);
			if (e instanceof XMLParseException) {
				throw (XMLParseException)e;
            } else if (e instanceof IOException) {
                throw (IOException) e;
			} else {
				throw new XMLParseException(e);
			}
		}
	}
	
	/**
	 * Parses accounts.
	 * @param nodes a node list containing accounts 
	 * @param debet indicates whether the accounts in the list are on the
	 *              debet side (<code>true</code>) or credit side (<code>false</code>).
	 * @param database the database to which the accounts belong
	 * @return the accounts
	 */
	private static Account[] parseAccounts(NodeList nodes, boolean debet, Database database)
	{
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
	            accounts.add(new Account(id, name, debet, database));
	        }
	    }
	    return accounts.toArray(new Account[accounts.size()]);
	}
	
	/**
	 * Parses parties.
	 * @param nodes a node list containing parties. 
	 * @return an array of parties found in <code>nodes</code>
     * @throws XMLParseException if a syntax error is found in the nodes
	 */
	private static Party[] parseParties(NodeList nodes)	throws XMLParseException {
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
        
	    return parties.toArray(new Party[parties.size()]);
	}
    
    /**
     * Parses invoices.
     * @param nodes a node list containing invoices. 
     * @return an array of invoices found in <code>nodes</code>
     * @throws XMLParseException if a syntax error is found in the nodes
     */
    private static Invoice[] parseInvoices(Party[] parties, NodeList nodes) throws XMLParseException {
        ArrayList<Invoice> invoices = new ArrayList<Invoice>();
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

                Party concerningParty = findPartyById(parties, invoiceElem.getAttribute("concerningParty"));
                if (concerningParty == null) {
                    throw new XMLParseException("No (valid) party specified for the invoice \"" + id + "\"");
                }
                
                Party payingParty = findPartyById(parties, invoiceElem.getAttribute("payingParty"));
                
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
                
                invoices.add(new Invoice(id, payingParty, concerningParty, amountToBePaid, 
                    issueDate, descriptions, amounts, payments));
            }
        }
        
        return invoices.toArray(new Invoice[invoices.size()]);
    }

    /**
     * Finds a party by its id.
     * @param parties the parties to be searched for the id
     * @param id the id
     * @return the party or <code>null</code> if no party was found with the specified id 
     */
    private static Party findPartyById(Party[] parties, String id) {
        for (int i = 0; i < parties.length; i++) {
            if (parties[i].getId().equals(id)) {
                return parties[i];
            }
        }
        return null;
    }
}
