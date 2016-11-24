package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoicePreviewTemplate;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.gogoaccount.services.ServiceException;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.XHTMLPrintable;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import static nl.gogognome.gogoaccount.component.invoice.InvoiceSending.Type.PRINT;

public class PrintInvoicesView extends SendInvoicesView {

    private final Document document;
    private final InvoiceService invoiceService;

    public PrintInvoicesView(Document document, InvoiceService invoiceService,
                             InvoicePreviewTemplate invoicePreviewTemplate, SettingsService settingsService) {
        super(document, invoicePreviewTemplate, settingsService);
        this.document = document;
        this.invoiceService = invoiceService;
    }

    @Override
    protected String getButtonResourceId() {
        return "gen.print";
    }

    @Override
    protected boolean send() throws Exception {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(new SelectedInvoicesPrintable());
        if (printJob.printDialog()) {
            printJob.print();
            return true;
        } else {
            return false;
        }
    }

    private class SelectedInvoicesPrintable implements Printable {

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex >= invoicesToSend.size()) {
                return Printable.NO_SUCH_PAGE;
            }
            XHTMLPanel pagePanel = new XHTMLPanel();
            updatePreview(templateModel.getString(), pageIndex, pagePanel);
            int result = new XHTMLPrintable(pagePanel).print(graphics, pageFormat, 0);
            try {
                invoiceService.createInvoiceSending(document, invoicesToSend.get(pageIndex), PRINT);
            } catch (ServiceException e) {
                throw new PrinterException("Failed to create invoice sending: " + e.getMessage());
            }
            return result;
        }
    }

}
