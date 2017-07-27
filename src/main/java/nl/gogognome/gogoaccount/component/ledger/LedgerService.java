package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.*;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.lib.collections.DefaultValueMap;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.textsearch.criteria.Criterion;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static nl.gogognome.gogoaccount.component.configuration.AccountType.CREDITOR;
import static nl.gogognome.gogoaccount.component.configuration.AccountType.DEBTOR;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate.Type.PURCHASE;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate.Type.SALE;

public class LedgerService {

    private final TextResource textResource;
    private final ConfigurationService configurationService;
    private final InvoiceService invoiceService;
    private final PartyService partyService;

    public LedgerService(TextResource textResource, ConfigurationService configurationService, InvoiceService invoiceService, PartyService partyService) {
        this.textResource = textResource;
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.partyService = partyService;
    }

    public JournalEntry findJournalEntry(Document document, String id) throws ServiceException {
        return ServiceTransaction.withResult(() -> new JournalEntryDAO(document).findById(id));
    }

    public List<JournalEntry> findJournalEntries(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new JournalEntryDAO(document).findAll("date"));
    }

    public DefaultValueMap<Long, List<JournalEntryDetail>> getJournalEntryIdToDetailsMap(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            DefaultValueMap<Long, List<JournalEntryDetail>> result = new DefaultValueMap<>(new HashMap<>(), emptyList());
            new JournalEntryDetailDAO(document).findAll().forEach(e -> {
                List<JournalEntryDetail> list;
                if (result.containsKey(e.getJournalEntryUniqueId())) {
                    list = result.get(e.getJournalEntryUniqueId());
                } else {
                    list = new ArrayList<>();
                    result.put(e.getJournalEntryUniqueId(), list);
                }
                list.add(e);
            });
            return result;
        });
    }

    public List<FormattedJournalEntry> findFormattedJournalEntries(Document document, Criterion criterion) throws ServiceException {
        FormattedJournalEntryFinder formattedJournalEntryFinder = new FormattedJournalEntryFinder(this, invoiceService);
        return ServiceTransaction.withResult(() -> new JournalEntryDAO(document).findAll("date")
            .stream()
            .map(journalEntry -> formattedJournalEntryFinder.format(document, journalEntry))
            .filter(journalEntry -> formattedJournalEntryFinder.matches(criterion, journalEntry))
            .collect(Collectors.toList()));
    }

    /**
     * Creates invoices and journals for a number of parties.
     * @param document the database to which the invoices are to be added.
     * @param debtorOrCreditorAccount a debtor account for a sales invoice, a creditor account for a purchase invoice
     * @param invoiceTemplate the definition of the invoice
     * @param parties the parties
     * @throws ServiceException if a problem occurs while creating invoices for one or more of the parties
     * @return the created invoices
     */
    public List<Invoice> createInvoiceAndJournalForParties(Document document, Account debtorOrCreditorAccount, InvoiceTemplate invoiceTemplate,
                                                  List<Party> parties) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            document.ensureDocumentIsWriteable();
            validateDebtorOrCreditorAccount(debtorOrCreditorAccount, invoiceTemplate);
            validateInvoice(invoiceTemplate);

            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            List<Party> partiesForWhichCreationFailed = new LinkedList<>();
            Map<String, List<String>> partyIdToTags = partyService.findPartyIdToTags(document);
            List<Invoice> createdInvoices = new ArrayList<>();
            for (Party party : parties) {
                List<String> tags = partyIdToTags.getOrDefault(party.getId(), emptyList());
                InvoiceDefinition invoiceDefinition = invoiceTemplate.getInvoiceDefinitionFor(party, tags);

                try {
                    Invoice invoice = invoiceService.create(document, bookkeeping.getInvoiceIdFormat(), invoiceDefinition);
                    createdInvoices.add(invoice);
                    JournalEntry journalEntry = buildJournalEntry(invoice);
                    List<JournalEntryDetail> journalEntryDetails = buildJournalEntryDetails(debtorOrCreditorAccount, invoiceDefinition);
                    addJournalEntry(document, journalEntry, journalEntryDetails);
                } catch (ServiceException e) {
                    partiesForWhichCreationFailed.add(party);
                }
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
            return createdInvoices;
        });
    }

    private void validateDebtorOrCreditorAccount(Account debtorOrCreditorAccount, InvoiceTemplate invoiceTemplate) throws ServiceException {
        if (debtorOrCreditorAccount == null || debtorOrCreditorAccount.getType() != DEBTOR && debtorOrCreditorAccount.getType() != CREDITOR) {
            throw new ServiceException(textResource.getString("InvoiceService.accountMustHaveDebtorOrCreditorType"));
        }
        if (debtorOrCreditorAccount.getType() == DEBTOR && invoiceTemplate.getType() != SALE) {
            throw new ServiceException(textResource.getString("InvoiceService.salesInvoiceMustHaveDebtor"));
        }
        if (debtorOrCreditorAccount.getType() == CREDITOR && invoiceTemplate.getType() != PURCHASE) {
            throw new ServiceException(textResource.getString("InvoiceService.purchaseInvoiceMustHaveCreditor"));
        }
    }

    private JournalEntry buildJournalEntry(Invoice invoice) {
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setDescription(invoice.getDescription());
        journalEntry.setDate(invoice.getIssueDate());
        journalEntry.setId(invoice.getId());
        journalEntry.setIdOfCreatedInvoice(invoice.getId());
        return journalEntry;
    }

    private List<JournalEntryDetail> buildJournalEntryDetails(Account debtorOrCreditorAccount, InvoiceDefinition invoiceDefinition) {
        List<JournalEntryDetail> journalEntryDetails = new ArrayList<>();
        for (InvoiceDefinitionLine line : invoiceDefinition.getLines()) {
            JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
            journalEntryDetail.setAmount(line.getAmount());
            journalEntryDetail.setAccountId(line.getAccount().getId());
            journalEntryDetail.setDebet(invoiceDefinition.getType() != SALE);
            journalEntryDetails.add(journalEntryDetail);
        }

        JournalEntryDetail totalJournalEntryDetail = new JournalEntryDetail();
        totalJournalEntryDetail.setAmount(invoiceDefinition.getTotalAmount());
        totalJournalEntryDetail.setAccountId(debtorOrCreditorAccount.getId());
        totalJournalEntryDetail.setDebet(invoiceDefinition.getType() == SALE);
        if (invoiceDefinition.getType() == SALE) {
            journalEntryDetails.add(0, totalJournalEntryDetail);
        } else {
            journalEntryDetails.add(totalJournalEntryDetail);
        }
        return journalEntryDetails;
    }

    private void validateInvoice(InvoiceTemplate invoiceTemplate) throws ServiceException {
        TextResource tr = Factory.getInstance(TextResource.class);
        if (invoiceTemplate.getIssueDate() == null) {
            throw new ServiceException(tr.getString("InvoiceService.issueDateNull"));
        }
        if (invoiceTemplate.getLines().isEmpty()) {
            throw new ServiceException(tr.getString("InvoiceService.invoiceWithZeroLines"));
        }
        for (InvoiceTemplateLine line : invoiceTemplate.getLines()) {
            if (line.getAccount() == null) {
                throw new ServiceException(tr.getString("InvoiceService.lineWithoutAccount"));
            }
            if (line.getAmountFormula() == null) {
                throw new ServiceException("Amount must be filled in for all lines.");
            }
        }
    }

    /**
     * Gets the journal that creates the specified invoice.
     * @param invoiceId the id of the invoice
     * @return the journal or null if no creating journal exists. The latter
     *         typically happens when the invoice was created in the previous
     *         year.
     */
    public JournalEntry findJournalThatCreatesInvoice(Document document, String invoiceId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new JournalEntryDAO(document).findByInvoiceId(invoiceId));
    }

    public JournalEntry addJournalEntry(Document document, JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails) throws ServiceException {
        return addJournalEntry(document, journalEntry, journalEntryDetails, false);
    }

    public List<JournalEntryDetail> findJournalEntryDetails(Document document, JournalEntry journalEntry) throws ServiceException {
        return ServiceTransaction.withResult(() -> new JournalEntryDetailDAO(document).findByJournalEntry(journalEntry.getUniqueId()));
    }

    /**
     * Adds an entry to the journal.
     *
     * <p>Optionally, this method can update invoices that are referred to by the journal entry.
     * To each invoice (referred to by the journal entry) a new payment is added for the
     * corresponding journal entry detail.
     *
     * @param document the document
     * @param journalEntry the journal entry to be added
     * @param journalEntryDetails the journal entry details to be added
     * @param createPayments <code>true</code> if payments have to be added for invoices referred
     *        to by the journal entry; <code>false</code> if no payments are to be created.
     * @return the created journal entry
     */
    public JournalEntry addJournalEntry(Document document, JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails, boolean createPayments)
            throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            document.ensureDocumentIsWriteable();
            validateDebetAndCreditSumsAreEqual(journalEntry, journalEntryDetails);

            if (createPayments) {
                createPaymentsForItemsOfJournal(document, journalEntry, journalEntryDetails);
            }

            JournalEntry createdJournalEntry = new JournalEntryDAO(document).create(journalEntry);
            for (JournalEntryDetail detail : journalEntryDetails) {
                detail.setJournalEntryUniqueId(createdJournalEntry.getUniqueId());
                new JournalEntryDetailDAO(document).create(detail);
            }
            document.notifyChange();
            return createdJournalEntry;
        });
    }

    private void validateDebetAndCreditSumsAreEqual(JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails) throws DebetAndCreditAmountsDifferException {
        Amount totalDebet = null;
        Amount totalCredit = null;
        for (JournalEntryDetail journalEntryDetail : journalEntryDetails) {
            if (journalEntryDetail.isDebet()) {
                totalDebet = Amount.add(totalDebet, journalEntryDetail.getAmount());
            } else {
                totalCredit = Amount.add(totalCredit, journalEntryDetail.getAmount());
            }
        }

        if (!Amount.areEqual(totalDebet, totalCredit)) {
            throw new DebetAndCreditAmountsDifferException("The sum of debet and credit amounts differ for journal " + journalEntry.getId() + "!");
        }
    }

    /**
     * Adds payments to invoices that are referred to by the journal.
     * To each invoice (referred to by the journal) a new payment is added for the
     * corresponding journal item.
     */
    private void createPaymentsForItemsOfJournal(Document document, JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails)
            throws ServiceException {
        for (JournalEntryDetail detail : journalEntryDetails) {
            if (detail.getInvoiceId() != null) {
                Payment payment = createPaymentForJournalEntryDetail(document, journalEntry, detail);
                detail.setPaymentId(payment.getId());
            }
        }
    }

    private Payment createPaymentForJournalEntryDetail(Document document, JournalEntry journalEntry, JournalEntryDetail journalEntryDetail)
            throws ServiceException {
        Amount amount;
        if (journalEntryDetail.isDebet()) {
            amount = journalEntryDetail.getAmount();
        } else {
            amount = journalEntryDetail.getAmount().negate();
        }
        Date date = journalEntry.getDate();
        String description = configurationService.getAccount(document, journalEntryDetail.getAccountId()).getName();
        Payment payment = new Payment(journalEntryDetail.getPaymentId());
        payment.setDescription(description);
        payment.setAmount(amount);
        payment.setDate(date);
        payment.setInvoiceId(journalEntryDetail.getInvoiceId());
        return invoiceService.createPayment(document, payment);
    }

    /**
     * Updates a journal entry. Payments that are modified by the update of the journal entry
     * are updated in the corresponding invoice.
     */
    public void updateJournalEntry(Document document, JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            document.ensureDocumentIsWriteable();
            validateDebetAndCreditSumsAreEqual(journalEntry, journalEntryDetails);

            // Update payments. Remove payments from old journal and add payments of the new journal.
            JournalEntryDetailDAO journalEntryDetailDAO = new JournalEntryDetailDAO(document);
            List<JournalEntryDetail> oldJournalEntryDetails = journalEntryDetailDAO.findByJournalEntry(journalEntry.getUniqueId());
            journalEntryDetailDAO.deleteByJournalEntry(journalEntry.getUniqueId());
            for (JournalEntryDetail oldJournalEntryDetail : oldJournalEntryDetails) {
                if (oldJournalEntryDetail.getPaymentId() != null) {
                    invoiceService.removePayment(document, oldJournalEntryDetail.getPaymentId());
                }
            }
            for (JournalEntryDetail newJournalEntryDetail : journalEntryDetails) {
                if (newJournalEntryDetail.getInvoiceId() != null) {
                    Payment payment = createPaymentForJournalEntryDetail(document, journalEntry, newJournalEntryDetail);
                    newJournalEntryDetail.setPaymentId(payment.getId());
                }
            }

            // Update journal entry and details in database
            new JournalEntryDAO(document).update(journalEntry);
            for (JournalEntryDetail journalEntryDetail : journalEntryDetails) {
                journalEntryDetail.setJournalEntryUniqueId(journalEntry.getUniqueId());
                journalEntryDetailDAO.create(journalEntryDetail);
            }

            document.notifyChange();
        });
    }

    /**
     * Removes a journal entry from the database. Payments booked in the journal entry are also removed.
     */
    public void removeJournalEntry(Document document, JournalEntry journalEntry) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            document.ensureDocumentIsWriteable();
            JournalEntryDetailDAO journalEntryDetailDAO = new JournalEntryDetailDAO(document);
            List<JournalEntryDetail> journalEntryDetails = journalEntryDetailDAO.findByJournalEntry(journalEntry.getUniqueId());
            for (JournalEntryDetail journalEntryDetail : journalEntryDetails) {
                journalEntryDetailDAO.delete(journalEntryDetail.getId());
                if (journalEntryDetail.getPaymentId() != null) {
                    invoiceService.removePayment(document, journalEntryDetail.getPaymentId());
                }
            }
            new JournalEntryDAO(document).delete(journalEntry.getUniqueId());
            if (journalEntry.getIdOfCreatedInvoice() != null) {
                invoiceService.deleteInvoice(document, journalEntry.getIdOfCreatedInvoice());
            }
            document.notifyChange();
        });
    }

    /**
     * Checks whether an account is used in the database. If it is unused, the account
     * can be removed from the database without destroying its integrity.
     * @param document the document
     * @param accountId the ID of the account
     * @return <code>true</code> if the account is used; <code>false</code> if the account is unused
     */
    public boolean isAccountUsed(Document document, String accountId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new JournalEntryDetailDAO(document).isAccountUsed(accountId));
    }

    /**
     * Gets the balance of the specified account at the specified date.
     * @param document the database from which to take the data
     * @param account the account
     * @param date the date
     * @return the balance of this account at the specified date
     */
    public Amount getAccountBalance(Document document, Account account, Date date) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            List<JournalEntry> journalEntries = new JournalEntryDAO(document).findAll();
            Amount result = new Amount("0");
            JournalEntryDetailDAO journalEntryDetailDAO = new JournalEntryDetailDAO(document);
            for (JournalEntry journalEntry : journalEntries) {
                if (DateUtil.compareDayOfYear(journalEntry.getDate(), date) <= 0) {
                    List<JournalEntryDetail> journalEntryDetails = journalEntryDetailDAO.findByJournalEntry(journalEntry.getUniqueId());
                    for (JournalEntryDetail detail : journalEntryDetails) {
                        if (detail.getAccountId().equals(account.getId())) {
                            if (account.isDebet() == detail.isDebet()) {
                                result = result.add(detail.getAmount());
                            } else {
                                result = result.subtract(detail.getAmount());
                            }
                        }
                    }
                }
            }
            return result;
        });
    }

    /**
     * Gets the balance of the specified account at start of the bookkeeping.
     * @param document the database from which to take the data
     * @param account the account
     * @return the balance of this account at start of the bookkeeping
     */
    public Amount getStartBalance(Document document, Account account) throws ServiceException {
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        Date date = bookkeeping.getStartOfPeriod();

        // Subtract one day of the period start date, because otherwise the changes
        // made on that day will be taken into account too.
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        date = cal.getTime();

        return getAccountBalance(document, account, date);
    }

}
