package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.services.ServiceException;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SepaFileGeneratorTest extends AbstractBookkeepingTest {

    private final AutomaticCollectionService automaticCollectionService = new AutomaticCollectionService();
    private final ConfigurationService configurationService = new ConfigurationService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final LedgerService ledgerService = new LedgerService();

    @Test
    public void generateSepaFile() throws Exception {
        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        assertFalse("Without invoices this test has no purpoese", invoices.isEmpty());

        File file = File.createTempFile("sepa-test", "xml");
        try {
            automaticCollectionService.createSepaAutomaticCollectionFile(document, file, invoices, DateUtil.createDate(2015, 11, 24));
            automaticCollectionService.validateSepaAutomaticCollectionFile(file);
        } finally {
            file.delete();
        }
    }

    @Test
    public void createJournalEntryForSepaFile() throws ServiceException {
        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        assertFalse("Without invoices this test has no purpoese", invoices.isEmpty());
        Date date = DateUtil.createDate(2015, 11, 24);
        String journalEntryId = "RBC123";
        String journalEntryDescription = "Automatic collection";
        String bankAccountId = "100";
        String debtorAccountId = "190";

        automaticCollectionService.createJournalEntryForAutomaticCollection(document, date, journalEntryId, journalEntryDescription,
                invoices, configurationService.getAccount(document, bankAccountId), configurationService.getAccount(document, debtorAccountId));

        JournalEntry journalEntry = ledgerService.findJournalEntry(document, journalEntryId);
        assertNotNull(journalEntry);
        assertEquals(date, journalEntry.getDate());
        assertEquals(journalEntryDescription, journalEntry.getDescription());
        assertNull(journalEntry.getIdOfCreatedInvoice());

        List<JournalEntryDetail> journalEntryDetails = ledgerService.findJournalEntryDetails(document, journalEntry);
        assertEquals(2 * invoices.size(), journalEntryDetails.size());
        for (int i=0; i<invoices.size(); i++) {
            JournalEntryDetail journalEntryDetail = journalEntryDetails.get(2 * i);
            assertEquals(journalEntryDetail.getAccountId(), bankAccountId);
            assertEquals(journalEntryDetail.getAmount(), invoices.get(i).getAmountToBePaid());
            assertEquals(journalEntryDetail.getInvoiceId(), invoices.get(i).getId());
            assertTrue(journalEntryDetail.isDebet());

            journalEntryDetail = journalEntryDetails.get(2 * i + 1);
            assertEquals(journalEntryDetail.getAccountId(), debtorAccountId);
            assertEquals(journalEntryDetail.getAmount(), invoices.get(i).getAmountToBePaid());
            assertNull(journalEntryDetail.getInvoiceId());
            assertFalse(journalEntryDetail.isDebet());
        }
    }
}
