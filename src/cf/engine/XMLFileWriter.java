/*
 * $Id: XMLFileWriter.java,v 1.17 2007-11-27 21:14:59 sanderk Exp $
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

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cf.engine.Invoice.Payment;

/**
 * This class writes the contents of a <code>Database</code> to an XML file.
 *
 * @author Sander Kooijmans
 */
public class XMLFileWriter {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

    private final static AmountFormat AMOUNT_FORMAT = new AmountFormat(Locale.US);
    
    private XMLFileWriter() {
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
			Element rootElement = doc.createElement("cfbookkeeping");
			doc.appendChild(rootElement); 
			
			// write description
			rootElement.setAttribute("description", db.getDescription());
			
			// write currency
			rootElement.setAttribute("currency", db.getCurrency().getCurrencyCode());
			
			// write start date
			rootElement.setAttribute("startdate", DATE_FORMAT.format(db.getStartOfPeriod()));
			
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
			for (int i = 0; i < journals.length; i++) 
			{
			    Element journalElem = doc.createElement("journal");
			    journalElem.setAttribute("id", journals[i].getId());
			    journalElem.setAttribute("date", DATE_FORMAT.format(journals[i].getDate()));
			    journalElem.setAttribute("description", journals[i].getDescription());
			    JournalItem[] items = journals[i].getItems();
			    for (int j = 0; j < items.length; j++) 
			    {
                    Element item = doc.createElement("item");
                    item.setAttribute("id", items[j].getAccount().getId());
                    item.setAttribute("amount", AMOUNT_FORMAT.formatAmount(items[j].getAmount()));
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

            // write the invoices
            rootElement.appendChild(createElementForInvoices(doc, db, "invoices", db.getInvoices()));


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
	        String groupName, Party[] parties) {
		Element groupElem = doc.createElement(groupName);
		for (int i=0; i<parties.length; i++) {
		    Element elem = doc.createElement("party");
		    elem.setAttribute("id", parties[i].getId());
		    elem.setAttribute("name", parties[i].getName());
		    if (parties[i].getAddress() != null) {
		        elem.setAttribute("address", parties[i].getAddress());
		    }
		    if (parties[i].getZipCode() != null) {
		        elem.setAttribute("zip", parties[i].getZipCode());
		    }
		    if (parties[i].getCity() != null) {
		        elem.setAttribute("city", parties[i].getCity());
		    }
            if (parties[i].getType() != null) {
                elem.setAttribute("type", parties[i].getType());
            }
            if (parties[i].getRemarks() != null) {
                elem.setAttribute("remarks", parties[i].getRemarks());
            }
            if (parties[i].getBirthDate() != null) {
                elem.setAttribute("birthdate", DATE_FORMAT.format(parties[i].getBirthDate()));
            }
		    groupElem.appendChild(elem);
		}
		return groupElem;
	}
    
    private static Element createElementForInvoices(Document doc, Database db, 
            String groupName, Invoice[] invoices) {
        Element groupElem = doc.createElement(groupName);
        for (int i=0; i<invoices.length; i++) {
            Element elem = doc.createElement("invoice");
            elem.setAttribute("id", invoices[i].getId());
            elem.setAttribute("amountToBePaid", AMOUNT_FORMAT.formatAmount(invoices[i].getAmountToBePaid()));
            elem.setAttribute("concerningParty", invoices[i].getConcerningParty().getId());
            
            Party payingParty = invoices[i].getPayingParty();
            if (payingParty != null) {
                elem.setAttribute("payingParty", payingParty.getId());
            }
            
            String[] descriptions = invoices[i].getDescriptions();
            Amount[] amounts = invoices[i].getAmounts();
            assert descriptions.length == amounts.length;
            for (int l=0; i<descriptions.length; l++) {
                Element lineElem = doc.createElement("line");
                lineElem.setAttribute("description", descriptions[l]);
                if (amounts[l] != null) {
                    lineElem.setAttribute("amount", AMOUNT_FORMAT.formatAmount(amounts[l]));
                }
                elem.appendChild(lineElem);
            }
            elem.setAttribute("issueDate", DATE_FORMAT.format(invoices[i].getIssueDate()));
            
            Payment[] payments = invoices[i].getPayments();
            for (int p = 0; p < payments.length; p++) {
                Element paymentElem = doc.createElement("payment");
                paymentElem.setAttribute("date", DATE_FORMAT.format(payments[p].date));
                paymentElem.setAttribute("amount", AMOUNT_FORMAT.formatAmount(payments[p].amount));
                if (payments[p].description != null) {
                    paymentElem.setAttribute("description", payments[p].description);
                }
            }
            
            groupElem.appendChild(elem);
        }
        return groupElem;
    }
        
}
