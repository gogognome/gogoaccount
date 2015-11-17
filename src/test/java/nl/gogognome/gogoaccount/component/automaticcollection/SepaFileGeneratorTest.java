package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.test.AbstractBookkeepingTest;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class SepaFileGeneratorTest extends AbstractBookkeepingTest {

    private final AutomaticCollectionService automaticCollectionService = new AutomaticCollectionService();
    private final InvoiceService invoiceService = new InvoiceService();

    private SepaFileGenerator generator = new SepaFileGenerator();

    @Test
    public void generateSepaFile() throws Exception {
        File file = File.createTempFile("sepa-test", "xml");
        try {
            AutomaticCollectionSettings settings = automaticCollectionService.getSettings(document);
            List<Invoice> invoices = invoiceService.findAllInvoices(document);
            generator.generate(document, settings, invoices, file, DateUtil.createDate(2015, 11, 24));

            System.out.println(new String(Files.readAllBytes(file.toPath())));
        } finally {
            file.delete();
        }
    }
}
