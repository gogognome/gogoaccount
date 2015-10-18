package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;

import java.util.Date;
import java.util.List;

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
    public JournalEntry findCreatingJournal(Document document, String invoiceId) {
        return ServiceTransaction.withoutResult(() -> {
            JournalEntryDetail journalEntryDetail = new JournalEntryDetailDAO(document).findByInvoiceId(invoiceId);
            return journalEntryDetail != null ? new JournalEntryDAO(document).get(journalEntryDetail.getJournalEntryUniqueId()) : null;
        });
    }

    public JournalEntry createJournalEntry(Document document, JournalEntry journalEntry) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            JournalEntry createdJournalEntry = new JournalEntryDAO(document).create(journalEntry);
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

}
