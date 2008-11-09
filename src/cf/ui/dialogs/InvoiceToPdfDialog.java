/*
 * $Id: InvoiceToPdfDialog.java,v 1.2 2008-11-09 13:59:13 sanderk Exp $
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
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.ViewDialog;
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
import cf.engine.Invoice;
import cf.engine.Payment;
import cf.pdf.PdfLatex;
import cf.ui.views.InvoiceEditAndSelectionView;

/**
 * This class implements the invoice dialog. This dialog can generate
 * invoices (PDF files) for debtors.
 *
 * TODO: Change this dialog into a View.
 * 
 * @author Sander Kooijmans
 */
public class InvoiceToPdfDialog extends OkCancelDialog {
    
    /** The database used to determine the invoices. */
    private Database database;
    
    private JTextField tfTemplateFileName;
    
    private JTextField tfPdfFileName;
    
    private DateModel dateModel;
    
    private JTextField tfConcerning;
    
    private JTextField tfOurReference;

    private DateModel dueDateModel;
    
    private Frame parentFrame;
    
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
    
    /** The <code>PrintWriter</code> used to write the tex file. */
    private PrintWriter texPrintWriter;
    
    /**
     * Constructor.
     * @param frame the parent of this dialog
     */
    public InvoiceToPdfDialog(final Frame frame, Database database) {
        super(frame, "id.title");
        this.parentFrame = frame;
        this.database = database;
        
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

        hideDialog();

        // Let the user select the invoices that should be added to the PDF file.
        InvoiceEditAndSelectionView invoicesView = new InvoiceEditAndSelectionView(database, true, true);
        ViewDialog dialog = new ViewDialog(parentFrame, invoicesView);
        dialog.showDialog();
        if (invoicesView.getSelectedInvoices() != null) {
            generateInvoices(invoicesView.getSelectedInvoices(), date, tfConcerning.getText(), 
                tfOurReference.getText(), dueDate);
        }
    }

    /**
     * TODO: move this functionality to the engine
     * @param invoices the invoices to be added to the PDF file 
     * @param date
     * @param concerning
     * @param ourReference
     * @param dueDate
     */
    private void generateInvoices(Invoice[] invoices, Date date, String concerning, String ourReference,
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

        for (int i = 0; i < invoices.length; i++) {
            if (!invoices[i].hasBeenPaid()) {
                writeInvoiceToTexFile(invoices[i], dueDate);
            }
        }
        
        writeTexTail();

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
    private void writeInvoiceToTexFile(Invoice invoice, Date dueDate) {
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
            replace(sb, "$concerning$", tfConcerning.getText());
            replace(sb, "$our-reference$", invoice.getId());
            replace(sb, "$due-date$", tr.formatDate("gen.dateFormat", dueDate));
            replace(sb, "$total-amount$", formatAmountForTex(invoice.getRemainingAmountToBePaid()));
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
                    String formattedAmount = amounts[i] != null ? formatAmountForTex(amounts[i]) : "";
                    replace(tempBuffer, "$item-amount$", formattedAmount);
                    replace(tempBuffer, "$item-date$", tr.formatDate("gen.dateFormat", invoice.getIssueDate()));
                    sb.append(tempBuffer);
                }

                List<Payment> payments = invoice.getPayments();
                for (Payment payment : payments) {
                    StringBuilder tempBuffer = new StringBuilder();
                    tempBuffer.append(itemLineTemplate);
                    replace(tempBuffer, "$item-description$", payment.getDescription());
                    String formattedAmount = formatAmountForTex(payment.getAmount().negate());
                    replace(tempBuffer, "$item-amount$", formattedAmount);
                    replace(tempBuffer, "$item-date$", tr.formatDate("gen.dateFormat", payment.getDate()));
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
     */
    private void writeTexTail()
    {
        for (int i=letterEnd; i<templateContents.size(); i++)
        {
            texPrintWriter.println(templateContents.get(i));
        }
        texPrintWriter.close();
        texPrintWriter = null;
    }
}
