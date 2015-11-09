package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetailBuilder;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class LedgerServiceTest extends AbstractBookkeepingTest {

    private final BookkeepingService bookkeepingService = ObjectFactory.create(BookkeepingService.class);
    private final ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final LedgerService ledgerService = ObjectFactory.create(LedgerService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

    @Test
    public void checkThatOneInvoiceExistsAndJournalEntry_t2_generatedAPaymentForTheInvoice() throws ServiceException {
        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        assertEquals(1, invoices.size());
        Invoice invoice = invoices.get(0);
        List<Payment> payments = invoiceService.findPayments(document, invoice);
        JournalEntry journalEntry = ledgerService.findJournalEntry(document, "t2");
        assertEquals(invoice.getId(), ledgerService.findJournalEntryDetails(document, journalEntry).get(0).getInvoiceId());
        assertEquals(payments.get(0).getId(), ledgerService.findJournalEntryDetails(document, journalEntry).get(0).getPaymentId());
    }

    @Test
    public void whenAJournalEntryDetailWithPaymentIsUpdatedThePaymentMustBeUpdatedToo() throws ServiceException {
        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        Invoice invoice = invoices.get(0);

        JournalEntry journalEntry = ledgerService.findJournalEntry(document, "t2");
        List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                JournalEntryDetailBuilder.debet().amount("EUR 15").account("100").invoiceId(invoice.getId()).build(),
                JournalEntryDetailBuilder.credit().amount("EUR 15").account("101").build());
        ledgerService.updateJournal(document, journalEntry, journalEntryDetails);

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        assertEquals(1, payments.size());
        Payment payment = payments.get(0);
        assertEquals(createAmount(15), payment.getAmount());
        Account account = configurationService.getAccount(document, journalEntryDetails.get(0).getAccountId());
        assertEquals(account.getName(), payment.getDescription());
        assertEquals(journalEntry.getDate(), payment.getDate());
        assertEquals(invoice.getId(), payment.getInvoiceId());
    }

    @Test
    public void whenAJournalEntryDetailWithPaymentIsUpdatedWithoutInvoiceThenPaymentMustBeRemoved() throws ServiceException {
        List<Invoice> invoices = invoiceService.findAllInvoices(document);
        Invoice invoice = invoices.get(0);

        JournalEntry journalEntry = ledgerService.findJournalEntry(document, "t2");
        List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                JournalEntryDetailBuilder.debet().amount("EUR 15").account("100").build(),
                JournalEntryDetailBuilder.credit().amount("EUR 15").account("101").build());
        ledgerService.updateJournal(document, journalEntry, journalEntryDetails);

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        assertEquals("[]", payments.toString());
    }

    @Test
    public void whenAJournalEntryDetailWithoutPaymentIsUpdatedWithInvoiceThenPaymentMustBeCreated() throws ServiceException {
        whenAJournalEntryDetailWithPaymentIsUpdatedWithoutInvoiceThenPaymentMustBeRemoved(); // now t2 has no payment
        whenAJournalEntryDetailWithPaymentIsUpdatedThePaymentMustBeUpdatedToo(); // now t2 has a payment
    }

}
