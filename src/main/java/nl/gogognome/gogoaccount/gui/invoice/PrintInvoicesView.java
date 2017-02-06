package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoicePreviewTemplate;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static nl.gogognome.gogoaccount.component.invoice.InvoiceSending.Type.PRINT;

public class PrintInvoicesView extends SendInvoicesView {

    private final Document document;
    private final InvoiceService invoiceService;
    private final PdfGenerator pdfGenerator;

    public PrintInvoicesView(Document document, InvoiceService invoiceService,
                             InvoicePreviewTemplate invoicePreviewTemplate, SettingsService settingsService,
                             PdfGenerator pdfGenerator) {
        super(document, invoicePreviewTemplate, settingsService);
        this.document = document;
        this.invoiceService = invoiceService;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    protected String getButtonResourceId() {
        return "gen.print";
    }

    @Override
    protected boolean send() throws Exception {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        if (printerJob.printDialog()) {
            PDDocument pdDocument = PDDocument.load(getMergedPdfsBytes());
            printerJob.setPageable(new PDFPageable(pdDocument));
            printerJob.print();

            for (Invoice invoice : invoicesToSend) {
                invoiceService.createInvoiceSending(document, invoice, PRINT);
            }
            return true;
        } else {
            return false;
        }
    }

    private byte[] getPdfBytes(Invoice invoice) throws Exception {
        String html = fillInParametersInTemplate(templateModel.getString(), invoice);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(16*1024);
        pdfGenerator.writePdfToStream(html, templateFileModel.getFile().toURI().toString(), baos);
        return baos.toByteArray();
    }

    private byte[] getMergedPdfsBytes() throws Exception {
        ByteArrayOutputStream mergedPdfsStream = new ByteArrayOutputStream(invoicesToSend.size() * 16 * 1024);
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        pdfMerger.setDestinationStream(mergedPdfsStream);
        for (Invoice invoice : invoicesToSend) {
            pdfMerger.addSource(new ByteArrayInputStream(getPdfBytes(invoice)));
        }
        pdfMerger.mergeDocuments(MemoryUsageSetting.setupMixed(Runtime.getRuntime().freeMemory() * 3 / 4));
        return mergedPdfsStream.toByteArray();
    }

}
