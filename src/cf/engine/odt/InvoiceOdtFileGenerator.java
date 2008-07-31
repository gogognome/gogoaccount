/*
 * $RCSfile: InvoiceOdtFileGenerator.java,v $
 * Copyright (c) PharmaPartners BV
 */

package cf.engine.odt;

import cf.engine.Invoice;
import cf.engine.Payment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * This class creates an ODT file based on invoices.
 *
 * @author SanderK
 */
public class InvoiceOdtFileGenerator {

    /** The inputstream of the template. */
    private ZipInputStream zipInputStream;

    /** The outputstream of the ODT file. */
    private ZipOutputStream zipOutputStream;

    /** The contents of the template file. */
    private ArrayList<String> templateContents;

    /**
     * Indicates the start of a single letter in the <code>templateContents</code>.
     * The letter consists of the elements with indices
     * <code>[letterStart..letterEnd)</code>.
     */
    private int letterStart;

    /**
     * Indicates the end of a single letter in the <code>templateContents</code>.
     * The letter consists of the elements with indices
     * <code>[letterStart..letterEnd)</code>.
     */
    private int letterEnd;

    /** The <code>PrintWriter</code> used to write the Odt file. */
    private PrintWriter odtPrintWriter;

    /**
     * @param templateFileName the file name of the template
     * @param odtFileName the file name of the ODT file to be created
     * @param invoices the invoices to be added to the PDF file
     * @param date
     * @param concerning
     * @param ourReference
     * @param dueDate
     * @throws IOException if a problem occurs while reading the template or
     *         while creating the ODT file
     */
    public void generateInvoices(String templateFileName, String odtFileName,
            Invoice[] invoices, Date date, String concerning, String ourReference,
            Date dueDate) throws IOException {
        TextResource tr = TextResource.getInstance();

        zipOutputStream = new ZipOutputStream(new BufferedOutputStream(
            new FileOutputStream(odtFileName)));
        try {
            zipInputStream = new ZipInputStream(new BufferedInputStream(
                new FileInputStream(templateFileName)));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if ("content.xml".equals(zipEntry.getName())) {
                    copyContentXml(zipEntry, invoices, dueDate, concerning);
                } else {
                    copyZipEntry(zipEntry);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        } finally {
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException e) {
                    // ignore this exception
                }
            }
            if (zipOutputStream != null) {
                try {
                    zipOutputStream.close();
                } catch (IOException e) {
                    // ignore this exception
                }
            }
        }
    }

    /**
     * Copies a zip entry from the input stream to the output stream.
     * No bytes must have been read from the zip entry in the input stream.
     * A new zip entry is created in the output stream.
     * @param zipEntry the zip entry of the input stream
     * @throws IOException if an I/O problem occurs
     */
    private void copyZipEntry(ZipEntry zipEntry) throws IOException {
        System.out.println("copying " + zipEntry.getName());
        zipOutputStream.putNextEntry(new ZipEntry(zipEntry));
        byte[] buffer = new byte[16384];
        for (int bytesRead = zipInputStream.read(buffer); bytesRead != -1; bytesRead = zipInputStream.read(buffer)) {
            zipOutputStream.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Copies the content.xml entry from the input stream to the output stream.
     * No bytes must have been read from the zip entry in the input stream.
     * A new zip entry is created in the output stream.
     * The content.xml stream will be modified so that it contains all invoices
     * @param zipEntry the zip entry of the input stream
     * @param invoices the invoices
     * @param dueDate the due date
     * @param concerning specifies what the invoice is about
     * @throws IOException if an I/O problem occurs
     */
    private void copyContentXml(ZipEntry zipEntry, Invoice[] invoices, Date dueDate,
            String concerning) throws IOException {
        DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docBuilderFac.newDocumentBuilder();
            Document doc = docBuilder.parse(zipInputStream);
            Element rootElement = doc.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new IOException("Could not parse content.xml: " + e.getMessage());
        } catch (SAXException e) {
            throw new IOException("Could not parse content.xml: " + e.getMessage());
        }
        // TODO Continue here!!
    }

    /**
     * Formats an amount for Odt. E.g., EUR is replaced by the
     * euro sign.
     *
     * @param amount the amount
     * @return the amount for Odt
     */
    private String formatAmountForOdt(Amount amount) {
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        String currency = amount.getCurrency().getSymbol();
        if (currency.equals("EUR")) {
            currency = "\\euro";
        }
        return af.formatAmount(amount, currency);
    }

    /**
     * Writes an invoice to the Odt file.
     * @param invoice the invoice to be written
     * @param dueDate the due date of the invoice
     * @param concerning describes what the invoice is about
     */
    private void writeInvoiceToOdtFile(Invoice invoice, Date dueDate, String concerning) {
        StringBuilder sb;
        TextResource tr = TextResource.getInstance();
        for (int lineIndex=letterStart; lineIndex<letterEnd; lineIndex++) {
            sb = new StringBuilder(1000);
            String line = templateContents.get(lineIndex);
            sb.append(line);

            // apply substitutions
            replace(sb, "$date$", tr.formatDate("gen.dateFormat", new Date()));
            replace(sb, "$party-name$", invoice.getConcerningParty().getName());
            replace(sb, "$party-id$", invoice.getConcerningParty().getId());
            replace(sb, "$party-address$", invoice.getConcerningParty().getAddress());
            replace(sb, "$party-zip$", invoice.getConcerningParty().getZipCode());
            replace(sb, "$party-city$", invoice.getConcerningParty().getCity());
            replace(sb, "$concerning$", concerning);
            replace(sb, "$our-reference$", invoice.getId());
            replace(sb, "$due-date$", tr.formatDate("gen.dateFormat", dueDate));
            replace(sb, "$total-amount$", formatAmountForOdt(invoice.getRemainingAmountToBePaid()));
            int index = sb.indexOf("$item-line$");
            if (index != -1) {
                sb.delete(index, index + "$item-line$".length());
                StringBuilder itemLineTemplate = new StringBuilder(sb.toString());
                sb.setLength(0);

                String[] descriptions = invoice.getDescriptions();
                Amount[] amounts = invoice.getAmounts();
                for (int i=0; i<invoice.getDescriptions().length; i++) {
                    StringBuilder tempBuffer = new StringBuilder();
                    tempBuffer.append(itemLineTemplate);
                    replace(tempBuffer, "$item-description$", descriptions[i]);
                    String formattedAmount = amounts[i] != null ? formatAmountForOdt(amounts[i]) : "";
                    replace(tempBuffer, "$item-amount$", formattedAmount);
                    replace(tempBuffer, "$item-date$", tr.formatDate("gen.dateFormat", invoice.getIssueDate()));
                    sb.append(tempBuffer);
                }

                Payment[] payments = invoice.getPayments();
                for (int i = 0; i < payments.length; i++) {
                    StringBuilder tempBuffer = new StringBuilder();
                    tempBuffer.append(itemLineTemplate);
                    replace(tempBuffer, "$item-description$", payments[i].getDescription());
                    String formattedAmount = formatAmountForOdt(payments[i].getAmount().negate());
                    replace(tempBuffer, "$item-amount$", formattedAmount);
                    replace(tempBuffer, "$item-date$", tr.formatDate("gen.dateFormat", payments[i].getDate()));
                    sb.append(tempBuffer);
                }
            }

            odtPrintWriter.println(sb.toString());
        }
    }

    /**
     * Replaces all occurrences of <code>oldValue</code> with <code>newValue</code>
     * in the specified string buffer.
     * @param sb the string buffer
     * @param oldValue the old value
     * @param newValue the new value
     */
    private void replace(StringBuilder sb, String oldValue, String newValue) {
        if (newValue == null) {
            newValue = "";
        }
        for (int index = sb.indexOf(oldValue); index != -1; index = sb.indexOf(oldValue)) {
            sb.replace(index, index + oldValue.length(), newValue);
        }
    }
}
