package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.textsearch.criteria.Criterion;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class LedgerService {

    public JournalEntry findJournalEntry(Document document, String id) throws ServiceException {
        return ServiceTransaction.withResult(() -> new JournalEntryDAO(document).findById(id));
    }

    public List<JournalEntry> findJournalEntries(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new JournalEntryDAO(document).findAll("date"));
    }

    public List<FormattedJournalEntry> findFormattedJournalEntries(Document document, Criterion criterion) throws ServiceException {
        FormattedJournalEntryFinder formattedJournalEntryFinder = new FormattedJournalEntryFinder();
        return ServiceTransaction.withResult(() -> new JournalEntryDAO(document).findAll("date")
            .stream()
            .map(journalEntry -> formattedJournalEntryFinder.format(document, journalEntry))
            .filter(journalEntry -> formattedJournalEntryFinder.matches(criterion, journalEntry))
            .collect(Collectors.toList()));
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

    private void validateDebetAndCreditSumsAreEqual(JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails) {
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
            throw new IllegalArgumentException("The sum of debet and credit amounts differ for journal " + journalEntry.getId() + "!");
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
        ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
        String description = configurationService.getAccount(document, journalEntryDetail.getAccountId()).getName();
        Payment payment = new Payment(journalEntryDetail.getPaymentId());
        payment.setDescription(description);
        payment.setAmount(amount);
        payment.setDate(date);
        payment.setInvoiceId(journalEntryDetail.getInvoiceId());
        return ObjectFactory.create(InvoiceService.class).createPayment(document, payment);
    }

    /**
     * Updates a journal entry. Payments that are modified by the update of the journal entry
     * are updated in the corresponding invoice.
     */
    public void updateJournal(Document document, JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            // Update payments. Remove payments from old journal and add payments of the new journal.
            InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
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
    public void removeJournal(Document document, JournalEntry journalEntry) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
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
        Bookkeeping bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
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
