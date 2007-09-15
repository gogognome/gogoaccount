/*
 * $Id: InvoiceDialog.java,v 1.15 2007-09-15 19:00:05 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
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
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.OkCancelDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;
import cf.engine.Database;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;
import cf.pdf.PdfLatex;

/**
 * This class implements the invoice dialog. This dialog can generate
 * invoices for debtors.
 * 
 * @author Sander Kooijmans
 */
public class InvoiceDialog extends OkCancelDialog 
{
    private static final Amount AMOUNT_TO_BE_IGNORED = 
        Amount.getZero(Database.getInstance().getCurrency());
        
    private static class Invoice {
        String date;
        String dueDate;
        Party party;
        Vector itemDescriptions = new Vector();
        Vector itemAmounts = new Vector();
        Vector itemDates = new Vector();
        Amount totalAmount;
        String ourReference;
        String concerning;
    }
    
    private JTextField tfTemplateFileName;
    
    private JTextField tfPdfFileName;
    
    private DateModel dateModel;
    
    private JTextField tfConcerning;
    
    private JTextField tfOurReference;

    private DateModel dueDateModel;
    
    private Frame parentFrame;
    
    /** The contents of the template file. */
    private ArrayList templateContents;
    
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
    
    /** The <code>PrintWriter</code> used to write the tex file. */
    private PrintWriter texPrintWriter;
    
    /**
     * Constructor.
     * @param frame the parent of this dialog
     */
    public InvoiceDialog(final Frame frame) 
    {
        super(frame, "id.title");
        this.parentFrame = frame;
        
        WidgetFactory wf = WidgetFactory.getInstance();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(wf.createLabel("id.templateFilename"),
                SwingUtils.createLabelGBConstraints(0, 0));

        tfTemplateFileName = wf.createTextField(30);
        panel.add(tfTemplateFileName,
                SwingUtils.createTextFieldGBConstraints(1, 0));

        JButton button = wf.createButton("gen.btSelectFile", new AbstractAction() { 
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser(tfTemplateFileName.getText());
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    tfTemplateFileName.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
                
            }
        });
        panel.add(button,
                SwingUtils.createLabelGBConstraints(2, 0));
        
        panel.add(wf.createLabel("id.pdfFileName"),
                SwingUtils.createLabelGBConstraints(0, 1));
        tfPdfFileName = wf.createTextField(30);
        panel.add(tfPdfFileName,
                SwingUtils.createTextFieldGBConstraints(1, 1));

        button = wf.createButton("gen.btSelectFile", new AbstractAction() { 
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser(tfPdfFileName.getText());
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    tfPdfFileName.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
                
            }
        });
        panel.add(button,
                SwingUtils.createLabelGBConstraints(2, 1));
        
        panel.add(wf.createLabel("id.date"),
                SwingUtils.createLabelGBConstraints(0, 2));
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
        panel.add(new DateSelectionBean(dateModel),
                SwingUtils.createLabelGBConstraints(1, 2));

        panel.add(wf.createLabel("id.concerning"),
                SwingUtils.createLabelGBConstraints(0, 3));
        tfConcerning = wf.createTextField(30);
        panel.add(tfConcerning,
                SwingUtils.createLabelGBConstraints(1, 3));
        
        panel.add(wf.createLabel("id.ourReference"),
                SwingUtils.createLabelGBConstraints(0, 4));
        tfOurReference = wf.createTextField(30);
        panel.add(tfOurReference,
                SwingUtils.createLabelGBConstraints(1, 4));

        panel.add(wf.createLabel("id.dueDate"),
                SwingUtils.createLabelGBConstraints(0, 5));
        dueDateModel = new DateModel();
        dueDateModel.setDate(DateUtil.addDays(new Date(), 14), null);
        panel.add(new DateSelectionBean(dueDateModel),
                SwingUtils.createLabelGBConstraints(1, 5));
        
        componentInitialized(panel);
    }

    /* (non-Javadoc)
     * @see cf.ui.dialogs.OkCancelDialog#handleOk()
     */
    protected void handleOk() {
        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog.showMessage(parentFrame, "gen.error",
                    TextResource.getInstance().getString("gen.invalidDate"));
            return;
        }

        Date dueDate = dueDateModel.getDate();
        if (dueDate == null) {
            MessageDialog.showMessage(parentFrame, "gen.error",
                    TextResource.getInstance().getString("gen.invalidDate"));
            return;
        }
        
        generateInvoices(date, tfConcerning.getText(), tfOurReference.getText(), dueDate);
        hideDialog();
    }

    private void generateInvoices(Date date, String concerning, String ourReference,
            Date dueDate) {
        TextResource tr = TextResource.getInstance();
        
        try {
            readTemplate(tfTemplateFileName.getText());
        }
        catch (IOException e) {
            MessageDialog.showMessage(parentFrame, "gen.titleError", 
                tr.getString("id.cantReadTemplateFile",
                new Object[] {tfTemplateFileName.getText()}));
            return;
        }
        
        File pdfFile = new File(tfPdfFileName.getText());
        File pdfFileDirectory = new File(pdfFile.getParent());
        File texFile = new File(PdfLatex.getTexFileName(tfPdfFileName.getText()));
        try {
            writeTexHeader(texFile);
        }
        catch (IOException e) {
            MessageDialog md = new MessageDialog(parentFrame, 
                "gen.titleError", tr.getString("id.cantCreateTexFile",
                new Object[] {texFile.getAbsolutePath()}));
            md.showDialog();
            return;
        }
        
        Database db = Database.getInstance();
        Journal[] journals = db.getJournals();
        Party[] debtors = db.getDebtors(date);
        for (int i = 0; i < debtors.length; i++) {
            Amount balance = Amount.getZero(Database.getInstance().getCurrency());
            Party debtor = debtors[i];
            Invoice invoice = new Invoice();
            invoice.party = debtor;
            invoice.date = tr.formatDate("gen.dateFormatFull", date);
            invoice.concerning = concerning;
            invoice.ourReference = ourReference;
            invoice.dueDate = tr.formatDate("gen.dateFormatFull", dueDate);
            for (int j=0; j<journals.length; j++) {
                Journal journal = journals[j];
                JournalItem[] items = journal.getItems();
                if (journal.hasItemWithParty(debtor)) {
                    invoice.itemDescriptions.addElement(journal.getDescription());
                    invoice.itemDates.addElement(
                            tr.formatDate("gen.dateFormat", journal.getDate()));
                    invoice.itemAmounts.addElement(AMOUNT_TO_BE_IGNORED);
                    
	                for (int k=0; k<items.length; k++) {
	                    JournalItem item = items[k];
	                    if (!debtor.equals(item.getParty())) {
	                        invoice.itemDescriptions.addElement(item.getAccount().getName());
	                        invoice.itemDates.addElement(
	                                tr.formatDate("gen.dateFormat", journal.getDate()));
	                        if (item.isCredit()) {
	                            balance = balance.add(item.getAmount());
	                            invoice.itemAmounts.addElement(item.getAmount());
	                        } else {
	                            Amount negAmount = item.getAmount().negate();
	                            balance = balance.add(negAmount);
	                            invoice.itemAmounts.addElement(negAmount);
	                        }
	                    }
	                }
                }
            }
            invoice.totalAmount = balance;
            removePaidAmounts(invoice);
            writeInvoiceToTexFile(invoice);
        }
        
        try
        {
            writeTexTail();
        }
        catch (IOException e)
        {
            MessageDialog md = new MessageDialog(parentFrame, 
                "gen.titleError", tr.getString("id.cantCompleteTexFile",
                new Object[] {texFile.getAbsolutePath()}));
            md.showDialog();
            return;
        }

        templateContents.clear();
        templateContents = null;
        
        try
        {
            PdfLatex.convertTexToPdf(texFile, pdfFileDirectory);
        }
        catch (IOException e) 
        {
            e.printStackTrace();
            MessageDialog.showMessage(parentFrame, "gen.titleError", 
                tr.getString("id.cantConvertTexToPdf",
                new Object[] {texFile.getAbsolutePath()}));
            return;
        }
        catch (InterruptedException e) 
        {
            e.printStackTrace();
            MessageDialog.showMessage(parentFrame, "gen.titleError", 
                tr.getString("id.cantConvertTexToPdf",
                new Object[] {texFile.getAbsolutePath()}));
            return;
        }
    }

    /** 
     * Formats an amount for tex. E.g., EUR is replaced by the
     * euro sign.
     * 
     * @param amount the amount
     * @return the amount for tex
     */
    private String formatAmountForTex(Amount amount) {
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        String currency = amount.getCurrency().getSymbol();
        if (currency.equals("EUR")) {
            currency = "\\euro";
        }
        return af.formatAmount(amount, currency);
    }
    
    /**
     * Writes an invoice to the tex file.
     * @param invoice the invoice to be written
     */
    private void writeInvoiceToTexFile(Invoice invoice) {
        StringBuffer sb = new StringBuffer();
        for (int lineIndex=letterStart; lineIndex<letterEnd; lineIndex++) {
            String line = (String)templateContents.get(lineIndex);
            sb.setLength(0);
            sb.append(line);
            
            // apply substitutions
            replace(sb, "$date$", invoice.date);
            replace(sb, "$party-name$", invoice.party.getName());
            replace(sb, "$party-id$", invoice.party.getId());
            replace(sb, "$party-address$", invoice.party.getAddress());
            replace(sb, "$party-zip$", invoice.party.getZipCode());
            replace(sb, "$party-city$", invoice.party.getCity());
            replace(sb, "$concerning$", invoice.concerning);
            replace(sb, "$our-reference$", invoice.ourReference);
            replace(sb, "$due-date$", invoice.dueDate);
            replace(sb, "$total-amount$", formatAmountForTex(invoice.totalAmount));
            int index = sb.indexOf("$item-line$");
            if (index != -1) {
                sb.delete(index, index + "$item-line$".length());
                StringBuffer itemLineTemplate = new StringBuffer(sb.toString());
                sb.setLength(0);
                for (int i=0; i<invoice.itemDescriptions.size(); i++) {
                    StringBuffer tempBuffer = new StringBuffer();
                    tempBuffer.append(itemLineTemplate);
                    replace(tempBuffer, "$item-description$", 
                            (String)invoice.itemDescriptions.elementAt(i));
                    Amount amount = (Amount)invoice.itemAmounts.elementAt(i);
                    String formattedAmount = formatAmountForTex(amount);
                    String date = (String)invoice.itemDates.elementAt(i);
                    if (amount == AMOUNT_TO_BE_IGNORED) {
                        formattedAmount = "";
                    } else {
                        date = "";
                    }
                    replace(tempBuffer, "$item-amount$", formattedAmount);
                    replace(tempBuffer, "$item-date$", date);
                    sb.append(tempBuffer);
                }
            }

            texPrintWriter.println(PdfLatex.convertUnicodeToTex(sb.toString()));
        }
    }
    
    /**
     * Replaces all occurrences of <code>oldValue</code> with <code>newValue</code>
     * in the specified string buffer. 
     * @param sb the string buffer
     * @param oldValue the old value
     * @param newValue the new value
     */
    private void replace(StringBuffer sb, String oldValue, String newValue) {
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
        
        templateContents = new ArrayList();
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
     * Writes the header of the tex file. <code>texPrintWriter</code> will be
     * initialised and will stay open so that the letters and the tail can
     * be written to the file.
     * 
     * @param texFile the tex file to be created.
     * @throws IOException if an I/O exception occurs.
     */
    private void writeTexHeader(File texFile) throws IOException
    {
        // Write header of tex file.
        texPrintWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(texFile)));
        
        for (int i=0; i<letterStart; i++)
        {
            texPrintWriter.println(templateContents.get(i));
        }
    }
    
    /**
     * Writes the tail of the tex file. 
     * <code>texPrintWriter</code> will be closed and set to <code>null</code>.
     * @throws IOException if an I/O exception occurs.
     */
    private void writeTexTail() throws IOException
    {
        for (int i=letterEnd; i<templateContents.size(); i++)
        {
            texPrintWriter.println(templateContents.get(i));
        }
        texPrintWriter.close();
        texPrintWriter = null;
    }
    
    /** 
     * Removes all paid amounts from an invoice
     * @param the invoice
     * @return the invoice without paid amounts  
     */
    private void removePaidAmounts(Invoice invoice) {
        int index = -1;
        Amount sum = Amount.getZero(Database.getInstance().getCurrency());
        for (int i=0; i<invoice.itemAmounts.size(); i++) {
            Amount amount = (Amount)invoice.itemAmounts.get(i);
            if (amount != AMOUNT_TO_BE_IGNORED) {
	            sum = sum.add(amount);
	            if (sum.isZero()) {
	                index = i;
	            }
            }
        }
        
        for (int i=0; i<index+1; i++) {
            invoice.itemAmounts.remove(0);
            invoice.itemDates.remove(0);
            invoice.itemDescriptions.remove(0);
        }
    }
}
