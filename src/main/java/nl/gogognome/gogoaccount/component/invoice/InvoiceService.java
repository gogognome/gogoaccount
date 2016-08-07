package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.criterion.ObjectCriterionMatcher;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.lib.util.StringUtil;
import nl.gogognome.textsearch.criteria.Criterion;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static nl.gogognome.gogoaccount.component.configuration.AccountType.CREDITOR;
import static nl.gogognome.gogoaccount.component.configuration.AccountType.DEBTOR;

public class InvoiceService {

    private final ObjectCriterionMatcher objectCriterionMatcher = new ObjectCriterionMatcher();

    private final PartyService partyService = ObjectFactory.create(PartyService.class);

    public Invoice getInvoice(Document document, String invoiceId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDAO(document).get(invoiceId));
    }

    public List<Invoice> findAllInvoices(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDAO(document).findAll("id"));
    }

    /**
     * Creates invoices and journals for a number of parties.
     * @param document the database to which the invoices are to be added.
     * @param debtorOrCreditorAccount a debtor account for a sales invoice, a creditor account for a purchase invoice
     * @param id the id of the invoices
     * @param parties the parties
     * @param issueDate the date of issue of the invoices
     * @param description an optional description for the invoices
     * @param invoiceLineDefinitions the lines of a single invoice
     * @throws ServiceException if a problem occurs while creating invoices for one or more of the parties
     */
    // TODO: move creation of journals to ledger component
    public void createInvoiceAndJournalForParties(Document document, Account debtorOrCreditorAccount, String id, List<Party> parties,
            Date issueDate, String description, List<InvoiceLineDefinition> invoiceLineDefinitions) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            validateInvoice(issueDate, id, debtorOrCreditorAccount, invoiceLineDefinitions);
            boolean changedDatabase = false;

            InvoiceDAO invoiceDAO = new InvoiceDAO(document);
            InvoiceDetailDAO invoiceDetailsDAO = new InvoiceDetailDAO(document);
            List<Party> partiesForWhichCreationFailed = new LinkedList<>();

            for (Party party : parties) {
                String specificId = replaceKeywords(id, party);
                String specificDescription =
                        !StringUtil.isNullOrEmpty(description) ? replaceKeywords(description, party) : null;

                List<String> descriptions = new ArrayList<>();
                List<Amount> amounts = new ArrayList<>();

                Amount totalAmount = new Amount(BigInteger.ZERO);
                for (InvoiceLineDefinition line : invoiceLineDefinitions) {
                    Amount amount = line.getAmount();
                    if (amount == null) {
                        throw new ServiceException("Amount must be filled in for all lines.");
                    }
                    totalAmount = totalAmount.add(amount);

                    descriptions.add(line.getDescription());
                    amounts.add(debtorOrCreditorAccount.getType() == DEBTOR ? amount : amount.negate());
                }

                if (invoiceDAO.exists(specificId)) {
                    specificId = suggestNewInvoiceId(document, specificId);
                }
                Invoice invoice = new Invoice(specificId);
                invoice.setDescription(specificDescription);
                invoice.setConcerningPartyId(party.getId());
                invoice.setPayingPartyId(party.getId());
                invoice.setAmountToBePaid(debtorOrCreditorAccount.getType() == DEBTOR ? totalAmount : totalAmount.negate());
                invoice.setIssueDate(issueDate);

                // Create the journal.
                List<JournalEntryDetail> journalEntryDetails = new ArrayList<>();
                for (InvoiceLineDefinition line : invoiceLineDefinitions) {
                    JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
                    journalEntryDetail.setAmount(line.getAmount());
                    journalEntryDetail.setAccountId(line.getAccount().getId());
                    journalEntryDetail.setDebet(debtorOrCreditorAccount.getType() != DEBTOR);
                    journalEntryDetails.add(journalEntryDetail);
                }

                JournalEntryDetail totalJournalEntryDetail = new JournalEntryDetail();
                totalJournalEntryDetail.setAmount(totalAmount);
                totalJournalEntryDetail.setAccountId(debtorOrCreditorAccount.getId());
                totalJournalEntryDetail.setDebet(debtorOrCreditorAccount.getType() == DEBTOR);
                if (debtorOrCreditorAccount.getType() == DEBTOR) {
                    journalEntryDetails.add(0, totalJournalEntryDetail);
                } else {
                    journalEntryDetails.add(totalJournalEntryDetail);
                }

                JournalEntry journalEntry;
                try {
                    journalEntry = new JournalEntry();
                    journalEntry.setId(specificId);
                    journalEntry.setDescription(specificDescription);
                    journalEntry.setDate(issueDate);
                    journalEntry.setIdOfCreatedInvoice(specificId);
                } catch (IllegalArgumentException e) {
                    throw new ServiceException("The debet and credit amounts are not in balance!", e);
                }

                try {
                    invoice = invoiceDAO.create(invoice);
                    invoiceDetailsDAO.createDetails(invoice.getId(), descriptions, amounts);
                    LedgerService ledgerService = ObjectFactory.create(LedgerService.class);
                    journalEntry.setIdOfCreatedInvoice(invoice.getId());
                    ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails);
                    changedDatabase = true;
                } catch (SQLException e) {
                    partiesForWhichCreationFailed.add(party);
                }
            }

            if (changedDatabase) {
                document.notifyChange();
            }

            if (!partiesForWhichCreationFailed.isEmpty()) {
                if (partiesForWhichCreationFailed.size() == 1) {
                    Party party = partiesForWhichCreationFailed.get(0);
                    throw new ServiceException("Failed to create journal for " + party.getId() + " - " + party.getName());
                } else {
                    StringBuilder sb = new StringBuilder(1000);
                    for (Party party : partiesForWhichCreationFailed) {
                        sb.append('\n').append(party.getId()).append(" - ").append(party.getName());
                    }
                    throw new ServiceException("Failed to create journal for the parties:" + sb.toString());
                }
            }
        });
    }

    private void validateInvoice(Date issueDate, String id, Account debtorOrCreditorAccount, List<InvoiceLineDefinition> invoiceLineDefinitions) throws ServiceException {
        TextResource tr = Factory.getInstance(TextResource.class);
        if (issueDate == null) {
            throw new ServiceException(tr.getString("InvoiceService.issueDateNull"));
        }
        if (id == null) {
            throw new ServiceException(tr.getString("InvoiceService.idIsNull"));
    }
        if (debtorOrCreditorAccount == null || debtorOrCreditorAccount.getType() != DEBTOR && debtorOrCreditorAccount.getType() != CREDITOR) {
            throw new ServiceException(tr.getString("InvoiceService.accountMustHaveDebtorOrCreditorType"));
        }

        if (invoiceLineDefinitions.isEmpty()) {
            throw new ServiceException(tr.getString("InvoiceService.invoiceWithZeroLines"));
        }
        for (InvoiceLineDefinition line : invoiceLineDefinitions) {
            if (line.getAmount() == null) {
                throw new ServiceException(tr.getString("InvoiceService.lineWithoutAmount"));
            }

            if (line.getAccount() == null) {
                throw new ServiceException(tr.getString("InvoiceService.lineWithoutAccount"));
            }
        }
    }

    /**
     * Replaces the keywords <code>{id}</code> and <code>{name}</code> with the corresponding
     * attributes of the specified party.
     * @param s the string in which the replacement has to be made
     * @param party the party
     * @return the string after the replacements have taken place
     */
    private static String replaceKeywords(String s, Party party) {
        StringBuilder sb = new StringBuilder(s);
        String[] keywords = new String[] { "{id}", "{name}" };
        String[] values = new String[] {
                party.getId(), party.getName()
        };

        for (int k=0; k<keywords.length; k++) {
            String keyword = keywords[k];
            String value = values[k];
            for (int index=sb.indexOf(keyword); index != -1; index=sb.indexOf(keyword)) {
                sb.replace(index, index+keyword.length(), value);
            }
        }
        return sb.toString();
    }

    /**
     * Gets the amount that has to be paid for this invoice minus
     * the payments that have been made.
     * @param document database containing the bookkeeping
     * @param invoiceId the id of the invoice
     * @param date the date for which the amount has to be determined.
     * @return the remaining amount that has to be paid
     */
    public Amount getRemainingAmountToBePaid(Document document, String invoiceId, Date date) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Invoice invoice = new InvoiceDAO(document).get(invoiceId);
            Amount result = invoice.getAmountToBePaid();
            for (Payment p : findPayments(document, invoice)) {
                if (DateUtil.compareDayOfYear(p.getDate(), date) <= 0) {
                    result = result.subtract(p.getAmount());
                }
            }
            return result;
        });
    }

    /**
     * Checks whether an invoice has been paid.
     * @param document database containing the bookkeeping
     * @param invoiceId the id of the invoice
     * @param date the date for which the amount has to be determined.
     * @return true if the invoice has been paid; false otherwise
     */
    public boolean isPaid(Document document, String invoiceId, Date date) throws ServiceException {
    	return getRemainingAmountToBePaid(document, invoiceId, date).isZero();
    }


    /**
     * Gets a suggestion for an unused invoice id.
     *
     * @param document database containing the bookkeeping
     * @param id a possibly existing invoice id
     * @return an invoice id that does not exist yet in this database
     */
    public String suggestNewInvoiceId(Document document, String id) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            String newId;
            Set<String> existingInvoiceIds = new InvoiceDAO(document).findExistingInvoiceIds();
            int serialNumber = 1;
            do {
                newId = id + "-" + serialNumber;
                serialNumber++;
            } while (existingInvoiceIds.contains(newId));
            return newId;
        });
    }

    public Invoice createInvoice(Document document, Invoice invoice) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDAO(document).create(invoice));
    }

    public boolean existsInvoice(Document document, String invoiceId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDAO(document).exists(invoiceId));
    }

    /**
     * Gets the invoices that match the search criteria.

     * @param document database containing the bookkeeping
     * @param searchCriteria the search criteria
     * @return the matching invoices. Will never return <code>null</code>.
     */
    public List<Invoice> findInvoices(Document document, InvoiceSearchCriteria searchCriteria) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            List<Invoice> invoices = new InvoiceDAO(document).findAll("id");
            List<Invoice> matchingInvoices = new ArrayList<>();
            for (Invoice invoice : invoices) {
                if (matches(document, searchCriteria, invoice)) {
                    matchingInvoices.add(invoice);
                }
            }
            return matchingInvoices;
        });
    }

    public void createDetails(Document document, Invoice invoice, List<String> descriptions, List<Amount> amounts) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            if (descriptions.size() != amounts.size()) {
                throw new IllegalArgumentException("Descriptions and amounts must have the same size");
            }
            InvoiceDetailDAO invoiceDetailDAO = new InvoiceDetailDAO(document);
            for (int i=0; i<descriptions.size(); i++) {
                InvoiceDetail invoiceDetail = new InvoiceDetail();
                invoiceDetail.setInvoiceId(invoice.getId());
                invoiceDetail.setDescription(descriptions.get(i));
                invoiceDetail.setAmount(amounts.get(i));
                invoiceDetailDAO.create(invoiceDetail);
            }
        });
    }

    public void updateInvoice(Document document, Invoice invoice, List<String> newDescriptions, List<Amount> newAmounts) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            new InvoiceDAO(document).update(invoice);
            new InvoiceDetailDAO(document).updateDetails(invoice.getId(), newDescriptions, newAmounts);
            document.notifyChange();
        });
    }

    public void deleteInvoice(Document document, String invoiceId) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            new InvoiceDAO(document).delete(invoiceId);
            document.notifyChange();
        });
    }

    public List<String> findDescriptions(Document document, Invoice invoice) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDetailDAO(document).findForInvoice(invoice.getId())
                .stream().map(InvoiceDetail::getDescription).collect(toList()));
    }

    public List<Amount> findAmounts(Document document, Invoice invoice) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDetailDAO(document).findForInvoice(invoice.getId())
                .stream().map(InvoiceDetail::getAmount).collect(toList()));
    }

    /**
     * Checks whether the specified <code>Invoice</code> matches these criteria.
     * @param invoice the invoice
     * @return <code>true</code> if the invoice matches the criteria,
     *          <code>false</code> otherwise
     */
    private boolean matches(Document document, InvoiceSearchCriteria searchCriteria, Invoice invoice) throws ServiceException {
        boolean matches = true;
        if (searchCriteria.getId() != null) {
            matches = matches && matches(searchCriteria.getId(), invoice.getId());
        }
        if (searchCriteria.getName() != null) {
            matches = matches && matches(searchCriteria.getName(), partyService.getParty(document, invoice.getPayingPartyId()).getName());
        }
        if (!searchCriteria.areClosedInvoicesIncluded()) {
            matches = matches && !isPaid(document, invoice.getId(), new Date());
        }
        return matches;
    }

    /**
     * Checks whether a specified criteria matches a specified value.
     * @param criteria the criteria
     * @param value the value
     * @return <code>true</code> if the criteria matches;
     *          <code>false</code> otherwise
     */
    private boolean matches(String criteria, String value) {
        return value != null && value.toLowerCase().contains(criteria.toLowerCase());
    }

    public List<Payment> findPayments(Document document, Invoice invoice) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PaymentDAO(document).findForInvoice(invoice.getId()));
    }

    public void createPayments(Document document, List<Payment> payments) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            PaymentDAO paymentDAO = new PaymentDAO(document);
            for (Payment payment : payments) {
                paymentDAO.create(payment);
            }
        });
    }

    public Payment createPayment(Document document, Payment payment) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Payment createdPayment = new PaymentDAO(document).create(payment);
            document.notifyChange();
            return createdPayment;
        });
    }

    public void removePayment(Document document, String paymentId) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            new PaymentDAO(document).delete(paymentId);
            document.notifyChange();
        });
    }

    public boolean hasPayments(Document document, String invoiceId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PaymentDAO(document).hasPayments(invoiceId));
    }

    public List<InvoiceOverview> findInvoiceOverviews(Document document, Criterion criterion, boolean includeClosedInvoices) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Currency currency = new ConfigurationService().getBookkeeping(document).getCurrency();
            AmountFormat amountFormat = new AmountFormat(document.getLocale(), currency);
            List<Invoice> invoices = new InvoiceDAO(document).findAll();
            Map<String, List<Payment>> invoiceIdToPayments = new PaymentDAO(document).findAll()
                    .stream()
                    .collect(groupingBy(payment -> payment.getInvoiceId()));
            Map<String, Party> partyIdToParty = new PartyService().findAllParties(document)
                    .stream()
                    .collect(toMap(party -> party.getId(), party -> party));
            return invoices.stream()
                    .map(invoice -> buildInvoiceOverview(invoice, invoiceIdToPayments, partyIdToParty))
                    .filter(overview -> includeClosedInvoices || !overview.getAmountToBePaid().equals(overview.getAmountPaid()))
                    .filter(overview -> matches(criterion, overview, amountFormat))
                    .collect(toList());
        });
    }

    private InvoiceOverview buildInvoiceOverview(Invoice invoice, Map<String, List<Payment>> invoiceIdToPayments, Map<String, Party> partyIdToParty) {
        InvoiceOverview overview = new InvoiceOverview();
        overview.setInvoiceId(invoice.getId());
        overview.setDescription(invoice.getDescription());
        overview.setIssueDate(invoice.getIssueDate());
        overview.setAmountToBePaid(invoice.getAmountToBePaid());
        List<Payment> payments = invoiceIdToPayments.getOrDefault(invoice.getId(), emptyList());
        overview.setAmountPaid(payments.stream()
                .map(payment -> payment.getAmount())
                .reduce(new Amount(BigInteger.ZERO), (a, b) -> a.add(b)));
        overview.setPartyId(invoice.getConcerningPartyId());
        overview.setPartyName(partyIdToParty.get(invoice.getConcerningPartyId()).getName());
        return overview;
    }

    private boolean matches(Criterion criterion, InvoiceOverview invoiceOverview, AmountFormat amountFormat) {
        if (criterion == null) {
            return true;
        }

        return objectCriterionMatcher.matches(criterion,
                amountFormat.formatAmount(invoiceOverview.getAmountPaid().toBigInteger()),
                amountFormat.formatAmount(invoiceOverview.getAmountToBePaid().toBigInteger()),
                invoiceOverview.getDescription(),
                invoiceOverview.getInvoiceId(),
                invoiceOverview.getIssueDate(),
                invoiceOverview.getPartyId(),
                invoiceOverview.getPartyName());
    }

}
