package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.TextResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static nl.gogognome.gogoaccount.component.configuration.AccountType.CREDITOR;
import static nl.gogognome.gogoaccount.component.configuration.AccountType.DEBTOR;

public class PaymentAmountAgainstDebtorAndCreditorValidator {

    private final ConfigurationService configurationService;
    private final InvoiceService invoiceService;
    private final TextResource textResource;

    private Amount expectedDebtorsAmount;
    private Amount expectedCreditorAmount;
    private Amount actualDebtorsAmount;
    private Amount actualCreditorsAmount;

    public PaymentAmountAgainstDebtorAndCreditorValidator(ConfigurationService configurationService, InvoiceService invoiceService, TextResource textResource) {
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.textResource = textResource;
    }

    void validate(Document document, List<JournalEntryDetail> journalEntryDetails) throws ServiceException {
        resetAmounts();

        Map<String, Invoice> idToInvoice = getIdToInvoice(document, journalEntryDetails);

        Set<String> debtorIds = configurationService.findAccountsOfType(document, DEBTOR).stream().map(account -> account.getId()).collect(toSet());
        Set<String> creditorIds = configurationService.findAccountsOfType(document, CREDITOR).stream().map(account -> account.getId()).collect(toSet());

        addAmountsFromJournalEntryDetails(journalEntryDetails, idToInvoice, debtorIds, creditorIds);

        validateAmounts();
    }

    private Map<String, Invoice> getIdToInvoice(Document document, List<JournalEntryDetail> journalEntryDetails) throws ServiceException {
        Set<String> invoiceIds = journalEntryDetails.stream()
                .filter(d -> d.getInvoiceId() != null)
                .map(d -> d.getInvoiceId())
                .collect(toSet());
        Map<String, Invoice> idToInvoice = new HashMap<>();
        for (String invoiceId : invoiceIds) {
            idToInvoice.put(invoiceId, invoiceService.getInvoice(document, invoiceId));
        }
        return idToInvoice;
    }

    private void addAmountsFromJournalEntryDetails(List<JournalEntryDetail> journalEntryDetails, Map<String, Invoice> idToInvoice, Set<String> debtorIds, Set<String> creditorIds) {
        for (JournalEntryDetail journalEntryDetail : journalEntryDetails) {
            Amount amountAsIfDebet = journalEntryDetail.isDebet() ? journalEntryDetail.getAmount() : journalEntryDetail.getAmount().negate();
            if (journalEntryDetail.getInvoiceId() != null) {
                addExpectedAmount(idToInvoice, journalEntryDetail, amountAsIfDebet);
            } else {
                addActualAmount(debtorIds, creditorIds, journalEntryDetail, amountAsIfDebet);
            }
        }
    }

    private void addExpectedAmount(Map<String, Invoice> idToInvoice, JournalEntryDetail journalEntryDetail, Amount amountAsIfDebet) {
        Invoice invoice = idToInvoice.get(journalEntryDetail.getInvoiceId());
        if (invoice.getAmountToBePaid().isPositive()) {
            expectedDebtorsAmount = expectedDebtorsAmount.add(amountAsIfDebet.negate());
        } else {
            expectedCreditorAmount = expectedCreditorAmount.add(amountAsIfDebet);
        }
    }

    private void addActualAmount(Set<String> debtorIds, Set<String> creditorIds, JournalEntryDetail journalEntryDetail, Amount amountAsIfDebet) {
        if (debtorIds.contains(journalEntryDetail.getAccountId())) {
            actualDebtorsAmount = actualDebtorsAmount.add(amountAsIfDebet);
        } else if (creditorIds.contains(journalEntryDetail.getAccountId())) {
            actualCreditorsAmount = actualCreditorsAmount.add(amountAsIfDebet.negate());
        }
    }

    private void validateAmounts() throws PaymentAmountDoesNotMatchDebtorOrCreditorAmountException {
        if (!expectedDebtorsAmount.equals(actualDebtorsAmount)) {
            throw new PaymentAmountDoesNotMatchDebtorOrCreditorAmountException(textResource.getString("LedgerService.debtorAmountNotEqualToAmountPaidForSaleInvoice"));
        } else if (!expectedCreditorAmount.equals(actualCreditorsAmount)) {
            throw new PaymentAmountDoesNotMatchDebtorOrCreditorAmountException(textResource.getString("LedgerService.creditorAmountNotEqualToAmountPaidForPurchaseInvoice"));
        }
    }

    private void resetAmounts() {
        expectedDebtorsAmount = Amount.ZERO;
        expectedCreditorAmount = Amount.ZERO;
        actualDebtorsAmount = Amount.ZERO;
        actualCreditorsAmount = Amount.ZERO;
    }
}
