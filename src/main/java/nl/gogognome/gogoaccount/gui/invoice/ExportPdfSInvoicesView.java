package nl.gogognome.gogoaccount.gui.invoice;

import com.itextpdf.text.DocumentException;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoicePreviewTemplate;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.Charset;

import static nl.gogognome.gogoaccount.component.invoice.InvoiceSending.Type.PDF;

public class ExportPdfsInvoicesView extends SendInvoicesView {

    private final Document document;
    private final InvoiceService invoiceService;

    public ExportPdfsInvoicesView(Document document, InvoiceService invoiceService, InvoicePreviewTemplate invoicePreviewTemplate) {
        super(invoicePreviewTemplate);
        this.document = document;
        this.invoiceService = invoiceService;
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
        TaskWithProgressDialog progressDialog = new TaskWithProgressDialog(this, textResource.getString("SendInvoicesView.sendingEmails"));
        progressDialog.execute(taskProgressListener -> {
            int progress = 0;
            for (Invoice invoice : invoicesToSend) {
                progress += 100 / invoicesToSend.size();
                taskProgressListener.onProgressUpdate(progress);
                try (OutputStream os = new FileOutputStream(new File(directory, invoice.getId() + "_" + invoiceIdToParty.get(invoice.getId()).getName() + ".pdf"))) {
                    ITextRenderer renderer = new ITextRenderer();
                    String xml = fillInParametersInTemplate(templateModel.getString(), invoice);
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))));
                    renderer.setDocument(doc, templateFileModel.getFile().toURI().toString());
                    renderer.layout();
                    renderer.createPDF(os);
                    invoiceService.createInvoiceSending(document, invoice, PDF);
                }
            }
            taskProgressListener.onProgressUpdate(100);
            return null;
        });
    }

}
