/*
 * $Id: XMLFileWriter.java,v 1.13 2007-02-10 16:28:46 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.gogognome.text.AmountFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class writes the contents of a <code>Database</code> to an XML file.
 *
 * @author Sander Kooijmans
 */
public class XMLFileWriter 
{

    private XMLFileWriter()
    {
        // should never be called
    }
    
	/**
	 * Writes a <tt>Database</tt> to a file.
	 * 
	 * @param fileName the name of the file.
	 */
	public static void writeDatabaseToFile( Database db, String fileName ) 
	{
		try 
		{
			DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();			
			Element rootElement = (Element) doc.createElement("cfbookkeeping");
			doc.appendChild(rootElement); 
			
			// write description
			rootElement.setAttribute("description", db.getDescription());
			
			// write currency
			rootElement.setAttribute("currency", db.getCurrency().getCurrencyCode());
			
			// write start date
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
			rootElement.setAttribute("startdate", sdf.format(db.getStartOfPeriod()));
			
			// write accounts
			rootElement.appendChild(createElementForAccounts(doc, db, "assets", 
			        db.getAssets()));
			rootElement.appendChild(createElementForAccounts(doc, db, "liabilities",
			        db.getLiabilities()));
			rootElement.appendChild(createElementForAccounts(doc, db, "expenses",
			        db.getExpenses()));
			rootElement.appendChild(createElementForAccounts(doc, db, "revenues",
			        db.getRevenues()));
			
			// write debtors and creditors
			rootElement.appendChild(createElementForParties(doc, db, 
			        "parties", db.getParties()));
			
			// write journals
			Element journalsElem = doc.createElement("journals");
			Journal[] journals = db.getJournals();
			AmountFormat af = new AmountFormat(Locale.US);
			for (int i = 0; i < journals.length; i++) 
			{
			    Element journalElem = doc.createElement("journal");
			    journalElem.setAttribute("id", journals[i].getId());
			    journalElem.setAttribute("date", sdf.format(journals[i].getDate()));
			    journalElem.setAttribute("description", journals[i].getDescription());
			    JournalItem[] items = journals[i].getItems();
			    for (int j = 0; j < items.length; j++) 
			    {
                    Element item = doc.createElement("item");
                    item.setAttribute("id", items[j].getAccount().getId());
                    item.setAttribute("amount", af.formatAmount(items[j].getAmount()));
                    item.setAttribute("side", items[j].isDebet() ? "debet" : "credit");
                    if (items[j].getParty() != null)
                    {
                        item.setAttribute("party", items[j].getParty().getId());
                    }
                    journalElem.appendChild(item);
                }
			    journalsElem.appendChild(journalElem);
            }
			rootElement.appendChild(journalsElem);
			
			// Use a Transformer for output
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty( OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult( new File(fileName) );
			transformer.transform(source, result);
		} 
		catch( ParserConfigurationException e ) 
		{
		    //logger.error("An exception occurred that should never have occurred: ", e);
		    throw new RuntimeException(e);
		} 
		catch( TransformerException e ) 
		{
			//logger.error("An exception occurred: ", e);
			throw new RuntimeException(e);
		}		
	}
	
	private static Element createElementForAccounts(Document doc, Database db, 
	        String groupName, Account[] accounts)
	{
		Element groupElem = doc.createElement(groupName);
		for (int i=0; i<accounts.length; i++) 
		{
		    Element elem = doc.createElement("account");
		    elem.setAttribute("id", accounts[i].getId());
		    elem.setAttribute("name", accounts[i].getName());
		    groupElem.appendChild(elem);
		}
		return groupElem;
	}
	
	private static Element createElementForParties(Document doc, Database db, 
	        String groupName, Party[] parties)
	{
		Element groupElem = doc.createElement(groupName);
		for (int i=0; i<parties.length; i++) 
		{
		    Element elem = doc.createElement("party");
		    elem.setAttribute("id", parties[i].getId());
		    elem.setAttribute("name", parties[i].getName());
		    if (parties[i].getAddress() != null)
		    {
		        elem.setAttribute("address", parties[i].getAddress());
		    }
		    if (parties[i].getZipCode() != null)
		    {
		        elem.setAttribute("zip", parties[i].getZipCode());
		    }
		    if (parties[i].getCity() != null)
		    {
		        elem.setAttribute("city", parties[i].getCity());
		    }
		    groupElem.appendChild(elem);
		}
		return groupElem;
	}
}
