package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.test.AbstractBookkeepingTest;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.util.DateUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static nl.gogognome.lib.util.DateUtil.createDate;
import static org.junit.Assert.*;

public class SepaFileGeneratorTest extends AbstractBookkeepingTest {

    private final List<Integer> reportedProgress = new ArrayList<>();
    private final TaskProgressListener progressListener = reportedProgress::add;

    File file;

    @Before
    public void createTemporaryFile() throws IOException {
        file = File.createTempFile("sepa-test", "xml");
        assertTrue(file.delete());
    }

    @After
    public void deleteTemporaryFile() {
        if (file.exists()) {
            assertTrue(file.delete());
        }
    }

    @Test
    public void generateSepaFile() throws Exception {
        createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), pietPuk, "Subscription 2011 {name}", subscription, debtors, 123);
        generateAutomaticCollectionFile(file);

        automaticCollectionService.validateSepaAutomaticCollectionFile(file);

        assertEquals("[0, 100, 100]", reportedProgress.toString());
    }

    @Test
    public void generateSepaFileWithPartyWithoutAutomaticCollectionSettings() throws Exception {
        createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);

        ServiceException serviceException = assertThrows(ServiceException.class, () -> generateAutomaticCollectionFile(file));

        assertTrue(serviceException.getMessage(), serviceException.getMessage().contains("Invoices with incomplete or incorrect data: 201100001 (0002 Jan Pieterszoon)"));
        assertTrue(serviceException.getMessage(), serviceException.getMessage().contains("Party 0002 Jan Pieterszoon has no automatic collection settings. " +
                "Those settings are required to generate a SEPA file"));
        assertFalse(file.exists());
    }

    @Test
    public void generateSepaFileWithPartyWithIncompleteAutomaticCollectionSettings() throws Exception {
        createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), pietPuk, "Subscription 2011 {name}", subscription, debtors, 123);
        PartyAutomaticCollectionSettings partyAutomaticCollectionSettings =
                automaticCollectionService.findSettings(document, partyService.getParty(document, pietPuk.getId()));
        partyAutomaticCollectionSettings.setCountry(null);
        automaticCollectionService.setAutomaticCollectionSettings(document, partyAutomaticCollectionSettings);

        ServiceException serviceException = assertThrows(ServiceException.class, () -> generateAutomaticCollectionFile(file));

        assertTrue(serviceException.getMessage(), serviceException.getMessage().contains("Invoices with incomplete or incorrect data: 201100001 (0001 Pietje Puk)"));
        assertTrue(serviceException.getMessage(), serviceException.getMessage().contains("Value '' is not facet-valid with respect to pattern '[A-Z]{2,2}' for type 'CountryCode'."));
        assertFalse(file.exists());
    }

    @Test
    public void generateSepaFileWithNegativeInvoiceAmount() throws Exception {
        createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), pietPuk, "Subscription 2011 {name}", subscription, debtors, -123);

        ServiceException serviceException = assertThrows(ServiceException.class, () -> generateAutomaticCollectionFile(file));

        assertTrue(serviceException.getMessage(), serviceException.getMessage().contains("Invoices with incomplete or incorrect data: 201100001 (0001 Pietje Puk)"));
        assertTrue(serviceException.getMessage(), serviceException.getMessage().contains("Amount to be paid must be positive."));
        assertFalse(file.exists());
    }

    private void generateAutomaticCollectionFile(File file) throws ServiceException {
        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        automaticCollectionService.createSepaAutomaticCollectionFile(document, file, invoices,
                DateUtil.createDate(2015, 11, 24), progressListener);
    }

    @Test
    public void whenJournalEntryIsCreatedForSepaFileThenJournalEntryIsCreatedIncludingPayments() throws ServiceException {
        Invoice invoice1 = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), pietPuk, "Subscription 2011 {name}", subscription, debtors, 123);
        Invoice invoice2 = createSalesInvoiceAndJournalEntry(createDate(2011, 4, 10), pietPuk, "Extra subscription 2011 {name}", subscription, debtors, 456);
        List<Invoice> invoices = asList(invoice1, invoice2);

        Date date = DateUtil.createDate(2011, 5, 24);
        String journalEntryId = "RBC123";
        String journalEntryDescription = "Automatic collection";

        automaticCollectionService.createJournalEntryForAutomaticCollection(document, date, journalEntryId, journalEntryDescription,
                invoices, configurationService.getAccount(document, bankAccount.getId()), configurationService.getAccount(document, debtors.getId()));

        // Check created journal entry
        JournalEntry journalEntry = ledgerService.findJournalEntry(document, journalEntryId);
        assertNotNull(journalEntry);
        assertEquals(date, journalEntry.getDate());
        assertEquals(journalEntryDescription, journalEntry.getDescription());
        assertNull(journalEntry.getIdOfCreatedInvoice());

        // Check created journal entry details
        List<JournalEntryDetail> journalEntryDetails = ledgerService.findJournalEntryDetails(document, journalEntry);
        assertEquals(2 * invoices.size(), journalEntryDetails.size());
        for (int i=0; i<invoices.size(); i++) {
            JournalEntryDetail journalEntryDetail = journalEntryDetails.get(2 * i);
            assertEquals(journalEntryDetail.getAccountId(), bankAccount.getId());
            assertEquals(journalEntryDetail.getAmount(), invoices.get(i).getAmountToBePaid());
            assertEquals(journalEntryDetail.getInvoiceId(), invoices.get(i).getId());
            assertNotNull(journalEntryDetail.getPaymentId());
            assertTrue(journalEntryDetail.isDebet());

            journalEntryDetail = journalEntryDetails.get(2 * i + 1);
            assertEquals(journalEntryDetail.getAccountId(), debtors.getId());
            assertEquals(journalEntryDetail.getAmount(), invoices.get(i).getAmountToBePaid());
            assertNull(journalEntryDetail.getInvoiceId());
            assertNull(journalEntryDetail.getPaymentId());
            assertFalse(journalEntryDetail.isDebet());
        }
    }
}
