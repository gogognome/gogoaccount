package nl.gogognome.gogoaccount.services;

import nl.gogognome.dataaccess.migrations.DatabaseMigratorDAO;
import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import java.util.*;


/**
 * This class offers methods to manipulate the bookkeeping.
 */
public class BookkeepingService {

    private final ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);

    public Document closeBookkeeping(Document document, String description, Date date, Account equity) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            if (document.hasUnsavedChanges()) {
                throw new ServiceException("The bookkeeping contains unsaved changes. " +
                        "First save the changes before closing the bookkeeping.");
            }

            Document newDocument = createNewDatabase("New bookkeeping");
            newDocument.setFileName(null);
            ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            Bookkeeping newBookkeeping = configurationService.getBookkeeping(newDocument);
            newBookkeeping.setDescription(description);
            newBookkeeping.setCurrency(bookkeeping.getCurrency());
            newBookkeeping.setStartOfPeriod(date);
            configurationService.updateBookkeeping(newDocument, newBookkeeping);

            // Copy the parties
            PartyService partyService = ObjectFactory.create(PartyService.class);
            for (Party party : partyService.findAllParties(document)) {
                partyService.createParty(newDocument, party);
            }

            // Copy the accounts
            for (Account account : this.configurationService.findAllAccounts(document)) {
                this.configurationService.createAccount(newDocument, account);
            }

            // Create start balance
            Date dayBeforeStart = DateUtil.addDays(date, -1);

            List<JournalItem> journalItems = new ArrayList<>(20);
            for (Account account : this.configurationService.findAssets(document)) {
                JournalItem item = new JournalItem(getAccountBalance(document, account, dayBeforeStart),
                        this.configurationService.getAccount(newDocument, account.getId()), true, null, null);
                if (!item.getAmount().isZero()) {
                    journalItems.add(item);
                }
            }
            for (Account account : this.configurationService.findLiabilities(document)) {
                JournalItem item = new JournalItem(getAccountBalance(document, account, dayBeforeStart),
                        this.configurationService.getAccount(newDocument, account.getId()), false, null, null);
                if (!item.getAmount().isZero()) {
                    journalItems.add(item);
                }
            }

            // Add the result of operations to the specified account.
            Report report = createReport(document, dayBeforeStart);
            Amount resultOfOperations = report.getResultOfOperations();
            if (resultOfOperations.isPositive()) {
                journalItems.add(new JournalItem(resultOfOperations, this.configurationService.getAccount(newDocument, equity.getId()), false, null, null));
            } else if (resultOfOperations.isNegative()) {
                journalItems.add(new JournalItem(resultOfOperations.negate(), this.configurationService.getAccount(newDocument, equity.getId()), true, null, null));
            }
            try {
                Journal startBalance = new Journal("start", "start balance", dayBeforeStart,
                        journalItems.toArray(new JournalItem[journalItems.size()]), null);
                newDocument.addJournal(startBalance, false);
            } catch (IllegalArgumentException | DocumentModificationFailedException e) {
                throw new ServiceException("Failed to create journal for start balance.", e);
            }

            // Copy journals starting from the specified date
            for (Journal journal : document.getJournals()) {
                if (DateUtil.compareDayOfYear(date, journal.getDate()) <= 0) {
                    try {
                        newDocument.addJournal(copyJournal(journal, newDocument), false);
                    } catch (DocumentModificationFailedException e) {
                        throw new ServiceException("Failed to copy a journal to the new bookkeeping.", e);
                    }
                }
            }

            // Copy the open invoices including their payments
            for (Invoice invoice : invoiceService.findAllInvoices(document)) {
                if (!invoiceService.isPaid(document, invoice.getId(), dayBeforeStart)) {
                    invoiceService.createInvoice(newDocument, invoice);
                    invoiceService.createDetails(newDocument, invoice,
                            invoiceService.findDescriptions(document, invoice), invoiceService.findAmounts(document, invoice));
                    invoiceService.createPayments(newDocument, invoiceService.findPayments(document, invoice));
                }
            }

            // Notify unsaved changes in the new database.
            newDocument.notifyChange();

            return newDocument;
        });
    }

    private Journal copyJournal(Journal journal, Document newDocument) throws ServiceException {
        return new Journal(journal.getId(), journal.getDescription(), journal.getDate(),
            copyJournalItems(journal.getItems(), newDocument), journal.getIdOfCreatedInvoice());
    }

    private JournalItem[] copyJournalItems(JournalItem[] items, Document newDocument) throws ServiceException {
        JournalItem[] newItems = new JournalItem[items.length];
        for (int i=0; i<items.length; i++) {
            newItems[i] = new JournalItem(items[i].getAmount(), configurationService.getAccount(newDocument, items[i].getAccount().getId()),
                items[i].isDebet(), items[i].getInvoiceId(), items[i].getPaymentId());
        }
        return newItems;
    }

    /**
     * Removes a journal from the database. Payments booked in the journal or invoices created
     * by the journal are also removed.
     * @param document the database from which the journal has to be removed
     * @param journal the journal to be deleted
     * @throws ServiceException if a problem occurs while deleting the journal
     */
    public void removeJournal(Document document, Journal journal) throws ServiceException {
        // Check for payments without payment ID.
        for (JournalItem item : journal.getItems()) {
            if (item.getInvoiceId() != null && item.getPaymentId() == null) {
                throw new ServiceException("The journal has a payment without an id. Therefore, it cannot be removed.");
            }
        }

        try {
            // Remove payments.
            for (JournalItem item : journal.getItems()) {
                String invoiceId = item.getInvoiceId();
                String paymentId = item.getPaymentId();
                if (invoiceId != null && paymentId != null) {
                    invoiceService.removePayment(document, paymentId);
                }
            }

            // Check if the journal created an invoice. If so, remove the invoice too.
            if (journal.createsInvoice()) {
                invoiceService.deleteInvoice(document, journal.getIdOfCreatedInvoice());
            }

            try {
                document.removeJournal(journal);
            } catch (DocumentModificationFailedException e) {
                throw new ServiceException("Could not delete journal.", e);
            }
        } finally {
            document.notifyChange();
        }
    }

    /**
     * Gets the balance of the specified account at the specified date.
     * @param document the database from which to take the data
     * @param account the account
     * @param date the date
     * @return the balance of this account at the specified date
     */
    public static Amount getAccountBalance(Document document, Account account, Date date) throws ServiceException {
        List<Journal> journals = document.getJournals();
        Bookkeeping bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
        Amount result = Amount.getZero(bookkeeping.getCurrency());
        for (Journal journal : journals) {
            if (DateUtil.compareDayOfYear(journal.getDate(), date) <= 0) {
                JournalItem[] items = journal.getItems();
                for (JournalItem item : items) {
                    if (item.getAccount().equals(account)) {
                        if (account.isDebet() == item.isDebet()) {
                            result = result.add(item.getAmount());
                        } else {
                            result = result.subtract(item.getAmount());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Gets the balance of the specified account at start of the bookkeeping.
     * @param document the database from which to take the data
     * @param account the account
     * @return the balance of this account at start of the bookkeeping
     */
    public static Amount getStartBalance(Document document, Account account) throws ServiceException {
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

    /**
     * Checks whether the specified account is used in the database. An account is considered
     * "in use" if it has a non-zero start balance or if one or more journals use the account.
     *
     * @param document the database
     * @param account the account
     * @return <code>true</code> if the account is used; <code>false</code> otherwise
     */
    public boolean inUse(Document document, Account account) throws ServiceException {
        if (!getStartBalance(document, account).isZero()) {
            return true;
        }

        for (Journal j : document.getJournals()) {
            for (JournalItem i : j.getItems()) {
                if (account.equals(i.getAccount())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Report createReport(Document document, Date date) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            ReportBuilder rb = new ReportBuilder(document, date);
            rb.setAssets(configurationService.findAssets(document));
            rb.setLiabilities(configurationService.findLiabilities(document));
            rb.setExpenses(configurationService.findExpenses(document));
            rb.setRevenues(configurationService.findRevenues(document));

            document.getJournals().stream().filter(j -> DateUtil.compareDayOfYear(j.getDate(), date) <= 0).forEach(rb::addJournal);

            for (Invoice invoice : invoiceService.findAllInvoices(document)) {
                if (DateUtil.compareDayOfYear(invoice.getIssueDate(), date) <= 0) {
                    rb.addInvoice(invoice);
                }
            }

            return rb.build();
        });
    }

    public Document createNewDatabase(String description) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Document document = new Document();
            new DatabaseMigratorDAO(document.getBookkeepingId()).applyMigrationsFromResource("/database/_migrations.txt");
            ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            bookkeeping.setDescription(description);
            bookkeeping.setStartOfPeriod(getFirstDayOfYear(new Date()));
            ObjectFactory.create(ConfigurationService.class).updateBookkeeping(document, bookkeeping);
            return document;
        });
    }

    private static Date getFirstDayOfYear(Date date) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
