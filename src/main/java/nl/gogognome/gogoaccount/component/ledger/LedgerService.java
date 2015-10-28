package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LedgerService {

    public List<JournalEntry> findJournalEntries(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            return new JournalEntryDAO(document).findAll("date");
        });
    }

    /**
     * Gets the journal that creates the specified invoice.
     * @param invoiceId the id of the invoice
     * @return the journal or null if no creating journal exists. The latter
     *         typically happens when the invoice was created in the previous
     *         year.
     */
    public JournalEntry findJournalThatCreatesInvoice(Document document, String invoiceId) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            JournalEntryDetail journalEntryDetail = new JournalEntryDetailDAO(document).findByInvoiceId(invoiceId);
            return journalEntryDetail != null ? new JournalEntryDAO(document).get(journalEntryDetail.getJournalEntryUniqueId()) : null;
        });
    }

    public JournalEntry createJournalEntry(Document document, JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            JournalEntry createdJournalEntry = new JournalEntryDAO(document).create(journalEntry);
            JournalEntryDetailDAO journalEntryDetailDAO = new JournalEntryDetailDAO(document);
            for (JournalEntryDetail journalEntryDetail : journalEntryDetails) {
                journalEntryDetailDAO.create(journalEntryDetail);
            }
            document.notifyChange();
            return createdJournalEntry;
        });
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
     */
    public void addJournal(Document document, JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails, boolean createPayments)
            throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
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
                AmountFormat af = new AmountFormat(Locale.getDefault());
                throw new IllegalArgumentException(
                        "The sum of debet and credit amounts differ for journal " + journalEntry.getId()
                                + "! debet: " + af.formatAmount(totalDebet) +
                                "; credit: " + af.formatAmount(totalCredit));
            }

            if (createPayments) {
                createPaymentsForItemsOfJournal(document, journalEntry, journalEntryDetails);
            }
            new JournalEntryDAO(document).create(journalEntry);
            for (JournalEntryDetail detail : journalEntryDetails) {
                new JournalEntryDetailDAO(document).create(detail);
            }
            document.notifyChange();
        });
    }

    /**
     * Adds payments to invoices that are referred to by the journal.
     * To each invoice (referred to by the journal) a new payment is added for the
     * corresponding journal item.
     */
    private void createPaymentsForItemsOfJournal(Document document, JournalEntry journalEntry, List<JournalEntryDetail> journalEntryDetails)
            throws ServiceException {
        InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
        for (JournalEntryDetail detail : journalEntryDetails) {
            if (detail.getInvoiceId() != null) {
                Payment payment = createPaymentForJournalEntryDetail(document, journalEntry, detail);
                payment = invoiceService.createPayment(document, payment);
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
        return payment;
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
            for (JournalEntryDetail oldJournalEntryDetail : journalEntryDetailDAO.findByJournalEntry(journalEntry.getUniqueId())) {
                if (oldJournalEntryDetail.getPaymentId() != null) {
                    invoiceService.removePayment(document, oldJournalEntryDetail.getPaymentId());
                }
            }
            for (JournalEntryDetail newJournalEntryDetail : journalEntryDetails) {
                if (newJournalEntryDetail.getInvoiceId() != null) {
                    Payment payment = createPaymentForJournalEntryDetail(document, journalEntry, newJournalEntryDetail);
                    invoiceService.createPayment(document, payment);
                }
            }

            // Update journal entry and details in database
            new JournalEntryDAO(document).update(journalEntry);
            journalEntryDetailDAO.deleteByJournalEntry(journalEntry.getUniqueId());
            for (JournalEntryDetail journalEntryDetail : journalEntryDetails) {
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
            List<JournalEntryDetail> journalEntryDetails = new JournalEntryDetailDAO(document).findByJournalEntry(journalEntry.getUniqueId());
            for (JournalEntryDetail journalEntryDetail : journalEntryDetails) {
                if (journalEntryDetail.getPaymentId() != null) {
                    invoiceService.removePayment(document, journalEntryDetail.getPaymentId());
                }
            }
            new JournalEntryDAO(document).delete(journalEntry.getUniqueId());
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

}
