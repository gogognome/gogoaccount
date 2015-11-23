package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.test.AbstractBookkeepingTest;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SepaFileGeneratorTest extends AbstractBookkeepingTest {

    private final AutomaticCollectionService automaticCollectionService = new AutomaticCollectionService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final PartyService partyService = new PartyService();

    private final List<Integer> reportedProgress = new ArrayList<>();
    private final TaskProgressListener progressListener = p -> reportedProgress.add(p);

    File file;

    @Before
    public void createTemporaryFile() throws IOException {
        file = File.createTempFile("sepa-test", "xml");
        file.delete();
    }

    @After
    public void deleteTemporaryFile() {
        file.delete();
    }

    @Test
    public void generateSepaFile() throws Exception {
        generateAutomaticCollectionFile(file);

        automaticCollectionService.validateSepaAutomaticCollectionFile(file);

        assertEquals("[0, 100, 100]", reportedProgress.toString());
    }

    @Test
    public void generateSepaFileWithPartyWithoutAutomaticCollectionSettings() throws Exception {
        ServiceTransaction.withoutResult(() -> new PartyAutomaticCollectionSettingsDAO(document).delete("1101"));
        try {
            generateAutomaticCollectionFile(file);
        } catch (ServiceException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Invoices with incomplete or incorrect data: inv1 (1101 Pietje Puk)"));
            assertTrue(e.getMessage(), e.getMessage().contains("Party 1101 Pietje Puk has no automatic collection settings. " +
                    "Those settings are required to generate a SEPA file"));
            assertFalse(file.exists());
        }
    }

    @Test
    public void generateSepaFileWithPartyWithIncompleteAutomaticCollectionSettings() throws Exception {
        PartyAutomaticCollectionSettings partyAutomaticCollectionSettings =
                automaticCollectionService.findSettings(document, partyService.getParty(document, "1101"));
        partyAutomaticCollectionSettings.setCountry(null);
        automaticCollectionService.setAutomaticCollectionSettings(document, partyAutomaticCollectionSettings);

        try {
            generateAutomaticCollectionFile(file);
        } catch (ServiceException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Invoices with incomplete or incorrect data: inv1 (1101 Pietje Puk)"));
            assertTrue(e.getMessage(), e.getMessage().contains("Value '' is not facet-valid with respect to pattern '[A-Z]{2,2}' for type 'CountryCode'."));
            assertFalse(file.exists());
        }
    }

    @Test
    public void generateSepaFileWithNegativeIncoiceAmount() throws Exception {
        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        Invoice invoice = invoices.get(0);
        invoice.setAmountToBePaid(invoice.getAmountToBePaid().negate());
        List<Amount> amounts = invoiceService.findAmounts(document, invoice);
        List<Amount> negativeAmounts = amounts.stream().map(a -> a != null ? a.negate(): null).collect(toList());
        invoiceService.updateInvoice(document, invoice, invoiceService.findDescriptions(document, invoice), negativeAmounts);

        try {
            generateAutomaticCollectionFile(file);
        } catch (ServiceException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Invoices with incomplete or incorrect data: inv1 (1101 Pietje Puk)"));
            assertTrue(e.getMessage(), e.getMessage().contains("Amount to be paid must be positive."));
            assertFalse(file.exists());
        }
    }

    private void generateAutomaticCollectionFile(File file) throws ServiceException {
        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        automaticCollectionService.createSepaAutomaticCollectionFile(document, file, invoices,
                DateUtil.createDate(2015, 11, 24), progressListener);
    }
}
