package nl.gogognome.gogoaccount.gui.invoice;

import com.itextpdf.text.DocumentException;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoicePreviewTemplate;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

import static nl.gogognome.gogoaccount.component.invoice.InvoiceSending.Type.PDF;

public class ExportPdfsInvoicesView extends SendInvoicesView {

    private final Document document;
    private final InvoiceService invoiceService;
    private final PdfGenerator pdfGenerator;

    public ExportPdfsInvoicesView(Document document, InvoiceService invoiceService,
                                  InvoicePreviewTemplate invoicePreviewTemplate, SettingsService settingsService,
                                  PdfGenerator pdfGenerator) {
        super(document, invoicePreviewTemplate, settingsService);
        this.document = document;
        this.invoiceService = invoiceService;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    protected String getButtonResourceId() {
        return "ExportPdfsInvoicesView.exportPdf";
    }

    @Override
    protected boolean send() throws Exception {
        File directory = selectDirectory();
        if (directory == null) {
            return false;
        }

        exportInvoicesToPdf(directory);
        return true;
    }

    private File selectDirectory() {
        JFileChooser fc = new JFileChooser(templateFileModel.getFile().getParentFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int choice = fc.showDialog(this, textResource.getString("SendInvoicesView.titleExportDirectoryDialog"));
        if (choice == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        return null;
    }

    private void exportInvoicesToPdf(File directory) throws ParserConfigurationException, SAXException, IOException, DocumentException {
        TaskWithProgressDialog progressDialog = new TaskWithProgressDialog(this, textResource, "SendInvoicesView.generatingPdfFiles");
        progressDialog.execute(taskProgressListener -> {
            int progress = 0;
            for (Invoice invoice : invoicesToSend) {
                progress += 100 / invoicesToSend.size();
                taskProgressListener.onProgressUpdate(progress);
                String html = fillInParametersInTemplate(templateModel.getString(), invoice);
                try (FileOutputStream outputStream = new FileOutputStream(new File(directory, invoice.getId() + "_" + invoiceIdToParty.get(invoice.getId()).getName() + ".pdf"))) {
                    pdfGenerator.writePdfToStream(html, templateFileModel.getFile().toURI().toString(), outputStream);
                }

                invoiceService.createInvoiceSending(document, invoice, PDF);
            }
            taskProgressListener.onProgressUpdate(100);
            return null;
        });
    }

}
