package nl.gogognome.test.tools;

import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.gui.Start;
import nl.gogognome.gogoaccount.reportgenerators.OdtInvoiceGeneratorTask;
import nl.gogognome.gogoaccount.reportgenerators.OdtInvoiceParameters;
import nl.gogognome.gogoaccount.services.XMLFileReader;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.util.DateUtil;

import java.io.File;
import java.util.Date;
import java.util.Locale;

/**
 * Tests generation of ODT invoices.
 */
public class ODTInvoicesGenerator {

    public static void main(String[] args) throws Exception {
        new Start().initFactory(new Locale("nl"));

        File bookkeepingFile = new File(args[0]);
        File templateFile = new File(args[1]);
        File reportFile = new File(args[2]);

        XMLFileReader reader = new XMLFileReader(bookkeepingFile);
        Document document = reader.createDatabaseFromFile();

        InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
        OdtInvoiceParameters parameters = new OdtInvoiceParameters(document, invoiceService.findAllInvoices(document));
        parameters.setConcerning("Contributie seizoen 2011-2011");
        parameters.setDate(new Date());
        parameters.setDueDate(DateUtil.addMonths(new Date(), 1));
        parameters.setOurReference("co2053");

        OdtInvoiceGeneratorTask task = new OdtInvoiceGeneratorTask(parameters, reportFile, templateFile);
        task.execute(new TaskProgressListener() {
            @Override
            public void onProgressUpdate(int percentageCompleted) {
            }
        });

        System.out.println("Done!");
    }

}
