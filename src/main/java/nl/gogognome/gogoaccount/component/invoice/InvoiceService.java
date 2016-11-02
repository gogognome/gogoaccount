package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.criterion.ObjectCriterionMatcher;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.lib.collections.DefaultValueMap;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.textsearch.criteria.Criterion;

import java.math.BigInteger;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate.Type.SALE;

public class InvoiceService {

    private final ObjectCriterionMatcher objectCriterionMatcher = new ObjectCriterionMatcher();

    private final AmountFormat amountFormat;
    private final PartyService partyService;
    private final TextResource textResource;

    public InvoiceService(AmountFormat amountFormat, PartyService partyService, TextResource textResource) {
        this.amountFormat = amountFormat;
        this.partyService = partyService;
        this.textResource = textResource;
    }

    public Invoice getInvoice(Document document, String invoiceId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDAO(document).get(invoiceId));
    }

    public List<Invoice> findAllInvoices(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDAO(document).findAll("id"));
    }

    public Invoice create(Document document, InvoiceDefinition invoiceDefinition) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            validateInvoice(invoiceDefinition);

            InvoiceDAO invoiceDAO = new InvoiceDAO(document);
            InvoiceDetailDAO invoiceDetailsDAO = new InvoiceDetailDAO(document);

            String specificId = invoiceDefinition.getId();
            if (invoiceDAO.exists(specificId)) {
                specificId = suggestNewInvoiceId(document, specificId);
            }

            Invoice invoice = new Invoice(specificId);
            invoice.setDescription(invoiceDefinition.getDescription());
            invoice.setConcerningPartyId(invoiceDefinition.getParty().getId());
            invoice.setPayingPartyId(invoiceDefinition.getParty().getId());
            Amount totalAmount = invoiceDefinition.getTotalAmount();
            invoice.setAmountToBePaid(invoiceDefinition.getType() == SALE ? totalAmount : totalAmount.negate());
            invoice.setIssueDate(invoiceDefinition.getIssueDate());

            invoice = invoiceDAO.create(invoice);
            List<String> descriptions = invoiceDefinition.getLines().stream().map(InvoiceDefinitionLine::getDescription).collect(toList());
            List<Amount> amounts = invoiceDefinition.getLines().stream().map(InvoiceDefinitionLine::getAmount).collect(toList());
            invoiceDetailsDAO.createDetails(invoice.getId(), descriptions, amounts);

            document.notifyChange();
            return invoice;
        });
    }

    private void validateInvoice(InvoiceDefinition invoiceDefinition) throws ServiceException {
        if (invoiceDefinition.getIssueDate() == null) {
            throw new ServiceException(textResource.getString("InvoiceService.issueDateNull"));
        }
        if (invoiceDefinition.getId() == null) {
            throw new ServiceException(textResource.getString("InvoiceService.idIsNull"));
        }
        if (invoiceDefinition.getLines().isEmpty()) {
            throw new ServiceException(textResource.getString("InvoiceService.invoiceWithZeroLines"));
        }
        for (InvoiceDefinitionLine line : invoiceDefinition.getLines()) {
            if (line.getAccount() == null) {
                throw new ServiceException(textResource.getString("InvoiceService.lineWithoutAccount"));
            }
            if (line.getAmount() == null) {
                throw new ServiceException("Amount must be filled in for all lines.");
            }
        }
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

    public List<InvoiceDetail> findDetails(Document document, Invoice invoice) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDetailDAO(document).findForInvoice(invoice.getId()));
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

    public DefaultValueMap<String, List<Payment>> getInvoiceIdToPaymentsMap(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            DefaultValueMap<String, List<Payment>> result = new DefaultValueMap<>(new HashMap<>(), emptyList());
            new PaymentDAO(document).findAll().forEach(p -> {
                List<Payment> list;
                if (result.containsKey(p.getInvoiceId())) {
                    list = result.get(p.getInvoiceId());
                } else {
                    list = new ArrayList<>();
                    result.put(p.getInvoiceId(), list);
                }
                list.add(p);
            });
            return result;
        });
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
            List<Invoice> invoices = new InvoiceDAO(document).findAll();
            Map<String, List<Payment>> invoiceIdToPayments = new PaymentDAO(document).findAll()
                    .stream()
                    .collect(groupingBy(Payment::getInvoiceId));
            Map<String, Party> partyIdToParty = new PartyService().findAllParties(document)
                    .stream()
                    .collect(toMap(Party::getId, party -> party));
            return invoices.stream()
                    .map(invoice -> buildInvoiceOverview(invoice, invoiceIdToPayments, partyIdToParty))
                    .filter(overview -> includeClosedInvoices || !overview.getAmountToBePaid().equals(overview.getAmountPaid()))
                    .filter(overview -> matches(criterion, overview, amountFormat))
                    .collect(toList());
        });
    }

    private InvoiceOverview buildInvoiceOverview(Invoice invoice, Map<String, List<Payment>> invoiceIdToPayments, Map<String, Party> partyIdToParty) {
        InvoiceOverview overview = new InvoiceOverview(invoice.getId());
        overview.setDescription(invoice.getDescription());
        overview.setIssueDate(invoice.getIssueDate());
        overview.setAmountToBePaid(invoice.getAmountToBePaid());
        overview.setPayingPartyId(invoice.getPayingPartyId());
        overview.setConcerningPartyId(invoice.getConcerningPartyId());
        List<Payment> payments = invoiceIdToPayments.getOrDefault(invoice.getId(), emptyList());
        overview.setAmountPaid(payments.stream()
                .map(Payment::getAmount)
                .reduce(new Amount(BigInteger.ZERO), (a, b) -> a.add(b)));
        overview.setPayingPartyName(partyIdToParty.get(invoice.getConcerningPartyId()).getName());
        return overview;
    }

    protected boolean matches(Criterion criterion, InvoiceOverview invoiceOverview, AmountFormat amountFormat) {
        if (criterion == null) {
            return true;
        }

        return objectCriterionMatcher.matches(criterion,
                amountFormat.formatAmount(invoiceOverview.getAmountPaid().toBigInteger()),
                amountFormat.formatAmount(invoiceOverview.getAmountToBePaid().toBigInteger()),
                invoiceOverview.getDescription(),
                invoiceOverview.getId(),
                invoiceOverview.getIssueDate(),
                invoiceOverview.getPayingPartyId(),
                invoiceOverview.getPayingPartyName());
    }

    public DefaultValueMap<String,List<InvoiceDetail>> getIdToInvoiceDetails(Document document, List<String> invoiceIds) throws ServiceException {
        return ServiceTransaction.withResult(() -> new InvoiceDetailDAO(document).getIdToInvoiceDetails(invoiceIds));
    }

    public DefaultValueMap<String,List<Payment>> getIdToPayments(Document document, List<String> invoiceIds) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PaymentDAO(document).getIdToPayments(invoiceIds));
    }

}
