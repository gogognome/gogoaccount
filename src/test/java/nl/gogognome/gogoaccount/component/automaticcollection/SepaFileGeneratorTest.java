package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.test.AbstractBookkeepingTest;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SepaFileGeneratorTest extends AbstractBookkeepingTest {

    private final AutomaticCollectionService automaticCollectionService = new AutomaticCollectionService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Test
    public void generateSepaFile() throws Exception {
        File file = File.createTempFile("sepa-test", "xml");
        try {
            List<Invoice> invoices = invoiceService.findAllInvoices(document);
            automaticCollectionService.createSepaAutomaticCollectionFile(document, file, invoices, DateUtil.createDate(2015, 11, 24));

            System.out.println(new String(Files.readAllBytes(file.toPath())));
        } finally {
            file.delete();
        }
    }
}
