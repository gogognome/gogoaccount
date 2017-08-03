package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.ledger.DebetAndCreditAmountsDifferException;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetailBuilder;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate.Type.PURCHASE;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate.Type.SALE;
import static nl.gogognome.lib.util.DateUtil.createDate;
import static org.junit.Assert.assertEquals;

public class LedgerServiceTest extends AbstractBookkeepingTest {

    @Test
    public void findPayments_createInvoiceAndJournalWithPayment_returnsPaymentForInvoice() throws ServiceException {
        Invoice invoice = createInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = createJournalEntry(createDate(2011, 3, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice, debtors, null);
        assertEquals(1, invoiceService.findPayments(document, invoice).size());

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        journalEntry = ledgerService.findJournalEntry(document, journalEntry.getId());
        assertEquals(invoice.getId(), ledgerService.findJournalEntryDetails(document, journalEntry).get(0).getInvoiceId());
        assertEquals(payments.get(0).getId(), ledgerService.findJournalEntryDetails(document, journalEntry).get(0).getPaymentId());
    }

    @Test
    public void whenAJournalEntryDetailWithPaymentIsUpdatedThePaymentMustBeUpdatedToo() throws Exception {
        Invoice invoice = createInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = createJournalEntry(createDate(2011, 5, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice, debtors, null);

        List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                JournalEntryDetailBuilder.debet().amount("15").account(bankAccount.getId()).invoiceId(invoice.getId()).build(),
                JournalEntryDetailBuilder.credit().amount("15").account(debtors.getId()).build());
        ledgerService.updateJournalEntry(document, journalEntry, journalEntryDetails);

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        assertEquals(1, payments.size());
        Payment payment = payments.get(0);
        checkAmount(15, payment.getAmount());

        Account account = configurationService.getAccount(document, journalEntryDetails.get(0).getAccountId());
        assertEquals(account.getName(), payment.getDescription());
        assertEquals(journalEntry.getDate(), payment.getDate());
        assertEquals(invoice.getId(), payment.getInvoiceId());
    }

    @Test
    public void whenAJournalEntryDetailWithPaymentIsUpdatedWithoutInvoiceThenPaymentMustBeRemoved() throws ServiceException {
        Invoice invoice = createInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = createJournalEntry(createDate(2011, 5, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice, debtors, null);

        List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                JournalEntryDetailBuilder.debet().amount("15").account(cash.getId()).build(),
                JournalEntryDetailBuilder.credit().amount("15").account(bankAccount.getId()).build());
        ledgerService.updateJournalEntry(document, journalEntry, journalEntryDetails);

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        assertEquals(emptyList(), payments);
    }

    @Test
    public void whenAJournalEntryDetailWithoutPaymentIsUpdatedWithInvoiceThenPaymentMustBeCreated() throws Exception {
        Invoice invoice = createInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = createJournalEntry(createDate(2011, 5, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, null, debtors, null);

        List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                JournalEntryDetailBuilder.debet().amount("15").account(cash.getId()).invoiceId(invoice.getId()).build(),
                JournalEntryDetailBuilder.credit().amount("15").account(debtors.getId()).build());
        ledgerService.updateJournalEntry(document, journalEntry, journalEntryDetails);

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        assertEquals(1, payments.size());
        Payment payment = payments.get(0);
        checkAmount(15, payment.getAmount());

    }

    @Test
    public void removeJournal_journalCreatesInvoice_journalAndInvoiceRemoved() throws Exception {
        Invoice invoice = createInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = findJournalEntry(invoice.getId());

        ledgerService.removeJournalEntry(document, journalEntry);

        assertNull(findJournalEntry(journalEntry.getId()));
        assertFalse(invoiceService.existsInvoice(document, invoice.getId()));
    }

    @Test
    public void removeJournal_journalHasPayment_journalAndPaymentRemoved() throws Exception {
        Invoice invoice = createInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = createJournalEntry(createDate(2011, 3, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice, debtors, null);
        assertEquals(1, invoiceService.findPayments(document, invoice).size());

        ledgerService.removeJournalEntry(document, journalEntry);

        assertNull(findJournalEntry(journalEntry.getId()));
        assertEquals(emptyList(), invoiceService.findPayments(document, invoice));
    }

    @Test
    public void updateJournalEntry_journalEntryNotInBalance_shouldFail() throws ServiceException {
        JournalEntry journalEntry = createJournalEntry(createDate(2011, 3, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, null, debtors, null);
        List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                JournalEntryDetailBuilder.debet().amount("10").account(cash.getId()).build(),
                JournalEntryDetailBuilder.credit().amount("20").account(bankAccount.getId()).build());

        assertThrows(DebetAndCreditAmountsDifferException.class, () -> ledgerService.updateJournalEntry(document, journalEntry, journalEntryDetails));
    }

    @Test
    public void createInvoiceAndJournalForParties_createSalesInvoiceWithoutDebtor_isNotAllwed() {
        assertCreatingSalesInvoiceForAccountFails(null, "resource-id: InvoiceService.accountMustHaveDebtorOrCreditorType");
        assertCreatingSalesInvoiceForAccountFails(bankAccount, "resource-id: InvoiceService.accountMustHaveDebtorOrCreditorType");
        assertCreatingSalesInvoiceForAccountFails(creditors, "resource-id: InvoiceService.salesInvoiceMustHaveDebtor");
    }

    private void assertCreatingSalesInvoiceForAccountFails(Account debtorAccount, String expectedMessage) {
        ServiceException serviceException = assertThrows(ServiceException.class,
                () -> createInvoiceAndJournalEntry(SALE, DateUtil.createDate(2011, 5, 9), pietPuk, "Subscription", subscription, debtorAccount, 123));
        assertEquals(expectedMessage, serviceException.getMessage());
    }

    @Test
    public void createInvoiceAndJournalForParties_createPurchaseInvoiceWithoutCreditor_isNotAllwed() {
        assertCreatingPurchaseInvoiceForAccountFails(null, "resource-id: InvoiceService.accountMustHaveDebtorOrCreditorType");
        assertCreatingPurchaseInvoiceForAccountFails(bankAccount, "resource-id: InvoiceService.accountMustHaveDebtorOrCreditorType");
        assertCreatingPurchaseInvoiceForAccountFails(debtors, "resource-id: InvoiceService.purchaseInvoiceMustHaveCreditor");
    }

    private void assertCreatingPurchaseInvoiceForAccountFails(Account creditorAccount, String expectedMessage) {
        ServiceException serviceException = assertThrows(ServiceException.class,
                () -> createInvoiceAndJournalEntry(PURCHASE, DateUtil.createDate(2011, 5, 9), pietPuk, "Subscription", subscription, creditorAccount, 123));
        assertEquals(expectedMessage, serviceException.getMessage());
    }
}
