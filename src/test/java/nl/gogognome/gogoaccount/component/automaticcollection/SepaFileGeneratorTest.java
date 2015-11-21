package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.test.AbstractBookkeepingTest;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
            automaticCollectionService.createSepaAutomaticCollectionFile(document, file, invoices,
                    DateUtil.createDate(2015, 11, 24));

            automaticCollectionService.validateSepaAutomaticCollectionFile(file);
        } finally {
            file.delete();
        }
    }
}
