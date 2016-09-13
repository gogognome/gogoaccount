package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.test.AbstractBookkeepingTest;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.*;

public class SepaFileGeneratorTest extends AbstractBookkeepingTest {

    private final AutomaticCollectionService automaticCollectionService = new AutomaticCollectionService();
    private final ConfigurationService configurationService = new ConfigurationService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final LedgerService ledgerService = new LedgerService();
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

    @Test
    public void whenJournalEntryIsCreatedForSepaFileThenJournalEntryIsCreatedIncludingPayments() throws ServiceException {
        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        assertFalse("Without invoices this test has no purpoese", invoices.isEmpty());
        Date date = DateUtil.createDate(2015, 11, 24);
        String journalEntryId = "RBC123";
        String journalEntryDescription = "Automatic collection";
        String bankAccountId = "100";
        String debtorAccountId = "190";

        automaticCollectionService.createJournalEntryForAutomaticCollection(document, date, journalEntryId, journalEntryDescription,
                invoices, configurationService.getAccount(document, bankAccountId), configurationService.getAccount(document, debtorAccountId));

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
            assertEquals(journalEntryDetail.getAccountId(), bankAccountId);
            assertEquals(journalEntryDetail.getAmount(), invoices.get(i).getAmountToBePaid());
            assertEquals(journalEntryDetail.getInvoiceId(), invoices.get(i).getId());
            assertNotNull(journalEntryDetail.getPaymentId());
            assertTrue(journalEntryDetail.isDebet());

            journalEntryDetail = journalEntryDetails.get(2 * i + 1);
            assertEquals(journalEntryDetail.getAccountId(), debtorAccountId);
            assertEquals(journalEntryDetail.getAmount(), invoices.get(i).getAmountToBePaid());
            assertNull(journalEntryDetail.getInvoiceId());
            assertNull(journalEntryDetail.getPaymentId());
            assertFalse(journalEntryDetail.isDebet());
        }
    }
}
