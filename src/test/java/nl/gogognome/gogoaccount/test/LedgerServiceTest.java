package nl.gogognome.gogoaccount.test;

import static java.util.Collections.*;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate.Type.*;
import static nl.gogognome.lib.util.DateUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.configuration.*;
import nl.gogognome.gogoaccount.component.invoice.*;
import nl.gogognome.gogoaccount.component.ledger.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.lib.util.*;

public class LedgerServiceTest extends AbstractBookkeepingTest {

    private final Date someDate = DateUtil.createDate(2011, 5, 9);

    @Test
    public void findPayments_createInvoiceAndJournalWithPayment_returnsPaymentForInvoice() throws ServiceException {
        Invoice invoice = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = createJournalEntry(createDate(2011, 3, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice, debtors, null);
        assertEquals(1, invoiceService.findPayments(document, invoice).size());

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        journalEntry = ledgerService.findJournalEntry(document, journalEntry.getId());
        assertEquals(invoice.getId(), ledgerService.findJournalEntryDetails(document, journalEntry).get(0).getInvoiceId());
        assertEquals(payments.get(0).getId(), ledgerService.findJournalEntryDetails(document, journalEntry).get(0).getPaymentId());
    }

    @Test
    public void whenAJournalEntryDetailWithPaymentIsUpdatedThePaymentMustBeUpdatedToo() throws Exception {
        Invoice invoice = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = createJournalEntry(createDate(2011, 5, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, invoice, debtors, null);

        List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                JournalEntryDetailBuilder.debet().amount("15").account(bankAccount.getId()).invoiceId(invoice.getId()).build(),
                JournalEntryDetailBuilder.credit().amount("15").account(debtors.getId()).build());
        ledgerService.updateJournalEntry(document, journalEntry, journalEntryDetails);

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        assertEquals(1, payments.size());
        Payment payment = payments.get(0);
        assertAmountEquals(15, payment.getAmount());

        Account account = configurationService.getAccount(document, journalEntryDetails.get(0).getAccountId());
        assertEquals(account.getName(), payment.getDescription());
        assertEquals(journalEntry.getDate(), payment.getDate());
        assertEquals(invoice.getId(), payment.getInvoiceId());
    }

    @Test
    public void whenAJournalEntryDetailWithPaymentIsUpdatedWithoutInvoiceThenPaymentMustBeRemoved() throws ServiceException {
        Invoice invoice = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
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
        Invoice invoice = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = createJournalEntry(createDate(2011, 5, 25), "p1", "Payment subscription Jan Pieterszoon", 123, bankAccount, null, debtors, null);

        List<JournalEntryDetail> journalEntryDetails = Arrays.asList(
                JournalEntryDetailBuilder.debet().amount("15").account(cash.getId()).invoiceId(invoice.getId()).build(),
                JournalEntryDetailBuilder.credit().amount("15").account(debtors.getId()).build());
        ledgerService.updateJournalEntry(document, journalEntry, journalEntryDetails);

        List<Payment> payments = invoiceService.findPayments(document, invoice);
        assertEquals(1, payments.size());
        Payment payment = payments.get(0);
        assertAmountEquals(15, payment.getAmount());

    }

    @Test
    public void removeJournal_journalCreatesInvoice_journalAndInvoiceRemoved() throws Exception {
        Invoice invoice = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
        JournalEntry journalEntry = findJournalEntry(invoice.getId());

        ledgerService.removeJournalEntry(document, journalEntry);

        assertNull(findJournalEntry(journalEntry.getId()));
        assertFalse(invoiceService.existsInvoice(document, invoice.getId()));
    }

    @Test
    public void removeJournal_journalHasPayment_journalAndPaymentRemoved() throws Exception {
        Invoice invoice = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);
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
    public void createInvoiceAndJournalEntryForParties_createSalesInvoiceWithoutDebtor_isNotAllowed() {
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
    public void createInvoiceAndJournalEntryForParties_createPurchaseInvoiceWithoutCreditor_isNotAllowed() {
        assertCreatingPurchaseInvoiceForAccountFails(null, "resource-id: InvoiceService.accountMustHaveDebtorOrCreditorType");
        assertCreatingPurchaseInvoiceForAccountFails(bankAccount, "resource-id: InvoiceService.accountMustHaveDebtorOrCreditorType");
        assertCreatingPurchaseInvoiceForAccountFails(debtors, "resource-id: InvoiceService.purchaseInvoiceMustHaveCreditor");
    }

    private void assertCreatingPurchaseInvoiceForAccountFails(Account creditorAccount, String expectedMessage) {
        ServiceException serviceException = assertThrows(ServiceException.class,
                () -> createInvoiceAndJournalEntry(PURCHASE, DateUtil.createDate(2011, 5, 9), pietPuk, "Subscription", subscription, creditorAccount, 123));
        assertEquals(expectedMessage, serviceException.getMessage());
    }

    @Test
    public void createInvoiceAndJournalEntryForParties_createSalesInvoiceWithDebtor_debtorAndInvoiceAmountToBePaidAreEqual() throws Exception {
        createInvoiceAndJournalEntry(SALE, someDate, pietPuk, "Subscription", subscription, debtors, 123);

        Report report = bookkeepingService.createReport(document, someDate);
        assertAmountEquals(123, report.getBalanceForDebtor(pietPuk));
        assertAmountEquals(123, report.getInvoices().get(0).getAmountToBePaid());
    }

    @Test
    public void createInvoiceAndJournalEntryForParties_createPurchaseInvoiceWithCreditor_creditorAndInvoiceAmountToBePaidAreEqual() throws Exception {
        createInvoiceAndJournalEntry(PURCHASE, someDate, pietPuk, "Subscription", subscription, creditors, 123);

        Report report = bookkeepingService.createReport(document, someDate);
        assertAmountEquals(123, report.getBalanceForCreditor(pietPuk));
        assertAmountEquals(-123, report.getInvoices().get(0).getAmountToBePaid());
    }

    @Test
    public void createSaleInvoiceAndPartialPayment_debtorAndInvoiceAmountToBePaidAreEqual() throws Exception {
        Invoice invoice = createInvoiceAndJournalEntry(SALE, someDate, pietPuk, "Subscription", subscription, debtors, 123);

        createJournalEntry(someDate, "p1", "partial payment", 100, bankAccount, invoice, debtors, null);

        Report report = bookkeepingService.createReport(document, someDate);
        assertAmountEquals(23, report.getBalanceForDebtor(pietPuk));
        assertAmountEquals(23, report.getRemaingAmountForInvoice(invoice));
    }

    @Test
    public void createPurchaseInvoiceAndPartialPayment_creditorAndInvoiceAmountToBePaidAreEqual() throws Exception {
        Invoice invoice = createInvoiceAndJournalEntry(PURCHASE, someDate, pietPuk, "Subscription", subscription, creditors, 123);

        createJournalEntry(someDate, "p1", "partial payment", 100, creditors, null, bankAccount, invoice);

        Report report = bookkeepingService.createReport(document, someDate);
        assertAmountEquals(23, report.getBalanceForCreditor(pietPuk));
        assertAmountEquals(-23, report.getRemaingAmountForInvoice(invoice));
    }

    @Test
    public void createSaleInvoice_addPaymentUsingNonDebtorAccount_shouldFail() throws Exception {
        Invoice invoice = createInvoiceAndJournalEntry(SALE, someDate, pietPuk, "Subscription", subscription, debtors, 123);

        assertPaymentAmountNotEqualsDebtorAmount(invoice, cash);
        assertPaymentAmountNotEqualsDebtorAmount(invoice, creditors);
    }

    private void assertPaymentAmountNotEqualsDebtorAmount(Invoice invoice, Account creditAccount) {
        ServiceException exception = assertThrows(PaymentAmountDoesNotMatchDebtorOrCreditorAmountException.class,
                () -> createJournalEntry(someDate, "p1", "partial payment", 123, bankAccount, invoice, creditAccount, null));
        assertEquals("resource-id: LedgerService.debtorAmountNotEqualToAmountPaidForSaleInvoice", exception.getMessage());
    }

    @Test
    public void createPurchaseInvoice_addPaymentUsingNonCreditorAccount_shouldFail() throws Exception {
        Invoice invoice = createInvoiceAndJournalEntry(PURCHASE, someDate, pietPuk, "Subscription", subscription, creditors, 123);

        assertPaymentAmountNotEqualsCreditorAmount(invoice, cash);
        assertPaymentAmountNotEqualsCreditorAmount(invoice, debtors);
    }

    private void assertPaymentAmountNotEqualsCreditorAmount(Invoice invoice, Account debetAccount) {
        ServiceException exception = assertThrows(PaymentAmountDoesNotMatchDebtorOrCreditorAmountException.class,
                () -> createJournalEntry(someDate, "p1", "partial payment", 123, debetAccount, invoice, bankAccount, null));
        assertEquals("resource-id: LedgerService.creditorAmountNotEqualToAmountPaidForPurchaseInvoice", exception.getMessage());
    }


}
