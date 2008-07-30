/*
 * $RCSfile: InvoiceOdtFileGenerator.java,v $
 * Copyright (c) PharmaPartners BV
 */

package cf.engine.odt;

import cf.engine.Invoice;
import cf.engine.Payment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;


/**
 * This class creates an ODT file based on invoices.
 *
 * @author SanderK
 */
public class InvoiceOdtFileGenerator {

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

        try {
            readTemplate(templateFileName);
        } catch (IOException e) {
            throw new IOException(tr.getString("id.cantReadTemplateFile") + "(" + e.getMessage() + ")");
        }

        File odtFile = new File(odtFileName);
        writeOdtHeader(odtFile);

        for (int i = 0; i < invoices.length; i++) {
            if (!invoices[i].hasBeenPaid()) {
                writeInvoiceToOdtFile(invoices[i], dueDate, concerning);
            }
        }

        writeOdtTail();
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

    /**
     * Reads the template from the template file.
     * The result is stored in <code>templateContents</code>.
     * @param templateFileName
     * @throws IOException if an I/O exception occurs while reading the template file.
     */
    private void readTemplate(String templateFileName) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(templateFileName)));

        templateContents = new ArrayList<String>();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            templateContents.add(line);
            if (line.startsWith("\\begin{brief}")) {
                letterStart = templateContents.size() - 1;
            } else if ("\\end{brief}".equals(line)) {
                letterEnd = templateContents.size();
            }
        }
        reader.close();
    }

    /**
     * Writes the header of the Odt file. <code>odtPrintWriter</code> will be
     * initialised and will stay open so that the letters and the tail can
     * be written to the file.
     *
     * @param OdtFile the Odt file to be created.
     * @throws IOException if an I/O exception occurs.
     */
    private void writeOdtHeader(File OdtFile) throws IOException
    {
        // Write header of Odt file.
        odtPrintWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(OdtFile)));

        for (int i=0; i<letterStart; i++)
        {
            odtPrintWriter.println(templateContents.get(i));
        }
    }

    /**
     * Writes the tail of the Odt file.
     * <code>odtPrintWriter</code> will be closed and set to <code>null</code>.
     */
    private void writeOdtTail()
    {
        for (int i=letterEnd; i<templateContents.size(); i++)
        {
            odtPrintWriter.println(templateContents.get(i));
        }
        odtPrintWriter.close();
        odtPrintWriter = null;
    }

}

