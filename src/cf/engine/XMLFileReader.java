/*
 * $Id: XMLFileReader.java,v 1.15 2007-09-16 19:54:58 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;

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

    private XMLFileReader() {
        // should never be called
    }

	/**
	 * Creates a <tt>Database</tt> from a file.
	 * 
	 * @param fileName the name of the file.
	 * @return a <tt>Database</tt> with the contents of the file.
	 * @throws ParseException if a syntax error is found in the file.
     * @throws IOException if an I/O problem occurs while reading the file.
	 */
	public static Database createDatabaseFromFile(String fileName) throws ParseException, IOException {
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
			
			Account[] revenues = 
			    parseAccounts(rootElement.getElementsByTagName("revenues"), false, db); 
			db.setRevenues(revenues);
			
			Party[] parties =
			    parseParties(rootElement.getElementsByTagName("parties"));
			db.setParties(parties);
		    
		    NodeList journalsNodes = rootElement.getElementsByTagName("journals");
			AmountFormat af = new AmountFormat(Locale.US);
		    for (int i=0; i<journalsNodes.getLength(); i++)
		    {
		        Element elem = (Element)journalsNodes.item(i);
		        NodeList journalNodes = elem.getElementsByTagName("journal");
		        for (int j=0; j<journalNodes.getLength(); j++)
		        {
		            Element journalElem = (Element)journalNodes.item(j);
		            String id = journalElem.getAttribute("id");
		            String dateString = journalElem.getAttribute("date");
		            Date date = DATE_FORMAT.parse(dateString);
		            description = journalElem.getAttribute("description");
		            NodeList itemNodes = journalElem.getElementsByTagName("item");
		            Vector itemsVector = new Vector();
		            for (int k=0; k<itemNodes.getLength(); k++)
		            {
		                Element itemElem = (Element)itemNodes.item(k);
		                String itemId = itemElem.getAttribute("id");
		                String amountString = itemElem.getAttribute("amount");
		                Amount amount = af.parse(amountString);
		                String side = itemElem.getAttribute("side");
		                String partyString = itemElem.getAttribute("party");
		                itemsVector.addElement(new JournalItem(amount, db.getAccount(itemId), 
	                        "debet".equals(side), db.getParty(partyString)));
		            }
		            
		            JournalItem[] items = new JournalItem[itemsVector.size()];
		            itemsVector.copyInto(items);
			        db.addJournal( new Journal(id, description, date, items) );
		        }
		    }
		    
			return db;
		} 
		catch(Exception e) 
		{
			//logger.warn("Exception occurred while parsing XML file.", e);
			if (e instanceof ParseException) {
				throw (ParseException)e;
            } else if (e instanceof IOException) {
                throw (IOException) e;
			} else {
				throw new ParseException(e);
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
	    Vector accounts = new Vector();
	    for (int i=0; i<nodes.getLength(); i++)
	    {
	        Element elem = (Element)nodes.item(i);
	        NodeList accountNodes = elem.getElementsByTagName("account");
	        for (int j=0; j<accountNodes.getLength(); j++)
	        {
	            Element accountElem = (Element)accountNodes.item(j);
	            String id = accountElem.getAttribute("id");
	            String name = accountElem.getAttribute("name");
	            accounts.addElement(new Account(id, name, debet, database));
	        }
	    }
	    Account[] result = new Account[accounts.size()];
	    accounts.copyInto(result);
	    return result;
	}
	
	/**
	 * Parses parties.
	 * @param nodes a node list containing parties. 
	 * @return an array of parties found in <code>nodes</code>
     * @throws ParseException if a syntax error is found in the nodes
	 */
	private static Party[] parseParties(NodeList nodes)	throws ParseException {
	    ArrayList parties = new ArrayList();
	    for (int i=0; i<nodes.getLength(); i++) {
	        Element elem = (Element)nodes.item(i);
	        NodeList partyNodes = elem.getElementsByTagName("party");
	        for (int j=0; j<partyNodes.getLength(); j++) {
	            Element accountElem = (Element)partyNodes.item(j);
	            String id = accountElem.getAttribute("id");
	            String name = accountElem.getAttribute("name");
	            String address = accountElem.getAttribute("address");
	            if (address.length() == 0)
	            {
	                address = null;
	            }
	            String zipCode = accountElem.getAttribute("zip");
	            if (zipCode.length() == 0)
	            {
	                zipCode = null;
	            }
	            String city = accountElem.getAttribute("city");
	            if (city.length() == 0)
                {
	                city = null;
                }
                String birthDateString = accountElem.getAttribute("birthdate");
                Date birthDate = null;
                if (birthDateString.length() > 0) {
                    try {
                        birthDate = DATE_FORMAT.parse(birthDateString);
                    } catch (java.text.ParseException e) {
                        throw new ParseException("Invalid birth date: \"" + birthDateString + "\"");
                    }
                }
		        parties.add(new Party(id, name, address, zipCode, city, birthDate));
	        }
	    }
        
	    return (Party[]) parties.toArray(new Party[parties.size()]);
	}
}
