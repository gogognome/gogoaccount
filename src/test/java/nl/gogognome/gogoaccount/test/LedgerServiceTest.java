package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetailBuilder;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.test.builders.AmountBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class LedgerServiceTest extends AbstractBookkeepingTest {

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
                JournalEntryDetailBuilder.debet().amount("15").account("100").invoiceId(invoice.getId()).build(),
                JournalEntryDetailBuilder.credit().amount("15").account("101").build());
        ledgerService.updateJournalEntry(document, journalEntry, journalEntryDetails);

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        assertEquals(1, payments.size());
        Payment payment = payments.get(0);
        assertEquals(AmountBuilder.build(15), payment.getAmount());
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
                JournalEntryDetailBuilder.debet().amount("15").account("100").build(),
                JournalEntryDetailBuilder.credit().amount("15").account("101").build());
        ledgerService.updateJournalEntry(document, journalEntry, journalEntryDetails);

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        assertEquals("[]", payments.toString());
    }

    @Test
    public void whenAJournalEntryDetailWithoutPaymentIsUpdatedWithInvoiceThenPaymentMustBeCreated() throws ServiceException {
        whenAJournalEntryDetailWithPaymentIsUpdatedWithoutInvoiceThenPaymentMustBeRemoved(); // now t2 has no payment
        whenAJournalEntryDetailWithPaymentIsUpdatedThePaymentMustBeUpdatedToo(); // now t2 has a payment
    }

    @Test
    public void removeJournalThatCreatesInvoice() throws Exception {
        assertNotNull(findJournalEntry("t1"));
        assertTrue(invoiceService.existsInvoice(document, "inv1"));

        ledgerService.removeJournalEntry(document, findJournalEntry("t2")); // must remove journal with payment too to prevent foreign key violation
        ledgerService.removeJournalEntry(document, findJournalEntry("t1"));

        assertNull(findJournalEntry("t1"));
        assertFalse(invoiceService.existsInvoice(document, "inv1"));
    }

    @Test
    public void removeJournalWithPayment() throws Exception {
        assertNotNull(findJournalEntry("t2"));
        Invoice invoice = invoiceService.getInvoice(document, "inv1");
        assertFalse(invoiceService.findPayments(document, invoice).isEmpty());

        ledgerService.removeJournalEntry(document, findJournalEntry("t2"));

        assertNull(findJournalEntry("t2"));
        assertTrue(invoiceService.findPayments(document, invoice).isEmpty());
    }

    @Test
    public void updateJournalEntry_journalEntryNotInBalance_shouldFail() throws ServiceException {
        JournalEntry journalEntry = ledgerService.findJournalEntry(document, "t2");
        List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                JournalEntryDetailBuilder.debet().amount("10").account("100").build(),
                JournalEntryDetailBuilder.credit().amount("20").account("101").build());

        assertThrows(ServiceException.class, () -> ledgerService.updateJournalEntry(document, journalEntry, journalEntryDetails));
    }

}
