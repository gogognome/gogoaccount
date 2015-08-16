/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package nl.gogognome.gogoaccount.services;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nl.gogognome.dataaccess.DataAccessException;
import nl.gogognome.dataaccess.transaction.RunTransaction;
import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.businessobjects.Payment;
import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.businessobjects.ReportBuilder;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.database.DatabaseModificationFailedException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;


/**
 * This class offers methods to manipulate the bookkeeping.
 *
 * @author Sander Kooijmans
 */
public class BookkeepingService {

    public boolean hasAccounts(Database database) throws ServiceException {
        try {
            return RunTransaction.withResult(() -> database.getAccountDAO().hasAny());
        } catch (DataAccessException e) {
            throw new ServiceException(e);
        }
    }

    public List<Account> findAllAccounts(Database database) throws ServiceException {
        try {
            return RunTransaction.withResult(() -> database.getAccountDAO().findAll("id"));
        } catch (DataAccessException e) {
            throw new ServiceException(e);
        }
    }

    public Database closeBookkeeping(Database database, String description, Date date, Account equity) throws ServiceException {
        try {
            return RunTransaction.withResult(() -> {
                if (database.hasUnsavedChanges()) {
                    throw new ServiceException("The bookkeeping contains unsaved changes. " +
                            "First save the changes before closing the bookkeeping.");
                }

                Database newDatabase;
                try {
                    newDatabase = new Database();
                } catch (SQLException e) {
                    throw new ServiceException("Failed to create new database: " + e.getMessage(), e);
                }
                newDatabase.setCurrency(database.getCurrency());
                newDatabase.setDescription(description);
                newDatabase.setFileName(null);
                newDatabase.setStartOfPeriod(date);

                // Copy the parties
                try {
                    newDatabase.setParties(database.getParties());
                } catch (DatabaseModificationFailedException e) {
                    throw new ServiceException("Failed to copy parties to the new bookkeeping.", e);
                }

                // Copy the accounts
                for (Account account : database.getAccountDAO().findAll()) {
                    newDatabase.getAccountDAO().create(account);
                }

                // Create start balance
                Date dayBeforeStart = DateUtil.addDays(date, -1);

                List<JournalItem> journalItems = new ArrayList<JournalItem>(20);
                for (Account account : database.getAccountDAO().findAssets()) {
                    JournalItem item = new JournalItem(getAccountBalance(database, account, dayBeforeStart),
                            newDatabase.getAccountDAO().get(account.getId()), true, null, null);
                    if (!item.getAmount().isZero()) {
                        journalItems.add(item);
                    }
                }
                for (Account account : database.getAccountDAO().findLiabilities()) {
                    JournalItem item = new JournalItem(getAccountBalance(database, account, dayBeforeStart),
                            newDatabase.getAccountDAO().get(account.getId()), false, null, null);
                    if (!item.getAmount().isZero()) {
                        journalItems.add(item);
                    }
                }

                // Add the result of operations to the specified account.
                Report report = createReport(database, dayBeforeStart);
                Amount resultOfOperations = report.getResultOfOperations();
                if (resultOfOperations.isPositive()) {
                    journalItems.add(new JournalItem(resultOfOperations, newDatabase.getAccountDAO().get(equity.getId()), false, null, null));
                } else if (resultOfOperations.isNegative()) {
                    journalItems.add(new JournalItem(resultOfOperations.negate(), newDatabase.getAccountDAO().get(equity.getId()), true, null, null));
                }
                try {
                    Journal startBalance = new Journal("start", "start balance", dayBeforeStart,
                            journalItems.toArray(new JournalItem[journalItems.size()]), null);
                    newDatabase.addJournal(startBalance, false);
                } catch (IllegalArgumentException e) {
                    throw new ServiceException("Failed to create journal for start balance.", e);
                } catch (DatabaseModificationFailedException e) {
                    throw new ServiceException("Failed to create journal for start balance.", e);
                }

                // Copy journals starting from the specified date
                for (Journal journal : database.getJournals()) {
                    if (DateUtil.compareDayOfYear(date, journal.getDate()) <= 0) {
                        try {
                            newDatabase.addJournal(copyJournal(journal, newDatabase), false);
                        } catch (DatabaseModificationFailedException e) {
                            throw new ServiceException("Failed to copy a journal to the new bookkeeping.", e);
                        }
                    }
                }

                // Copy the open invoices including their payments
                List<Invoice> newInvoices = new ArrayList<Invoice>(100);
                for (Invoice invoice : database.getInvoices()) {
                    if (!InvoiceService.isPaid(database, invoice.getId(), dayBeforeStart)) {
                        newInvoices.add(new Invoice(invoice.getId(), invoice.getPayingParty(), invoice.getConcerningParty(),
                                invoice.getAmountToBePaid(), invoice.getIssueDate(), invoice.getDescriptions(), invoice.getAmounts()));
                    }
                }
                try {
                    newDatabase.setInvoices(newInvoices.toArray(new Invoice[newInvoices.size()]));
                    for (Invoice invoice : newInvoices) {
                        for (Payment payment : database.getPayments(invoice.getId())) {
                            newDatabase.addPayment(invoice.getId(), payment);
                        }
                    }
                } catch (DatabaseModificationFailedException e) {
                    throw new ServiceException("Failed to copy open invoices to the new bookkeeping.", e);
                }

                // Notify unsaved changes in the new database.
                newDatabase.notifyChange();

                return newDatabase;
            });
        } catch (DataAccessException e) {
            throw new ServiceException(e);
        }
    }

    private Journal copyJournal(Journal journal, Database newDatabase) throws SQLException {
        return new Journal(journal.getId(), journal.getDescription(), journal.getDate(),
            copyJournalItems(journal.getItems(), newDatabase), journal.getIdOfCreatedInvoice());
    }

    private JournalItem[] copyJournalItems(JournalItem[] items, Database newDatabase) throws SQLException {
        JournalItem[] newItems = new JournalItem[items.length];
        for (int i=0; i<items.length; i++) {
            newItems[i] = new JournalItem(items[i].getAmount(), newDatabase.getAccountDAO().get(items[i].getAccount().getId()),
                items[i].isDebet(), items[i].getInvoiceId(), items[i].getPaymentId());
        }
        return newItems;
    }

    /**
     * Removes a journal from the database. Payments booked in the journal or invoices created
     * by the journal are also removed.
     * @param database the database from which the journal has to be removed
     * @param journal the journal to be deleted
     * @throws ServiceException if a problem occurs while deleting the journal
     */
    public static void removeJournal(Database database, Journal journal) throws ServiceException {
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
                    try {
                        database.removePayment(invoiceId, paymentId);
                    } catch (DatabaseModificationFailedException e) {
                        throw new ServiceException("Could not delete payment " + paymentId, e);
                    }
                }
            }

            // Check if the journal created an invoice. If so, remove the invoice too.
            if (journal.createsInvoice()) {
                try {
                    database.removeInvoice(journal.getIdOfCreatedInvoice());
                } catch (DatabaseModificationFailedException e) {
                    throw new ServiceException("Could not delete the invoice " + journal.getIdOfCreatedInvoice()
                        + " created by the journal.", e);
                }
            }

            try {
                database.removeJournal(journal);
            } catch (DatabaseModificationFailedException e) {
                throw new ServiceException("Could not delete journal.", e);
            }
        } finally {
            database.notifyChange();
        }
    }

    /**
     * Gets the balance of the specified account at the specified date.
     * @param database the database from which to take the data
     * @param account the account
     * @param date the date
     * @return the balance of this account at the specified date
     */
    public static Amount getAccountBalance(Database database, Account account, Date date) {
        List<Journal> journals = database.getJournals();
        Amount result = Amount.getZero(database.getCurrency());
        for (Journal journal : journals) {
            if (DateUtil.compareDayOfYear(journal.getDate(), date) <= 0) {
                JournalItem[] items = journal.getItems();
                for (int j = 0; j < items.length; j++) {
                    if (items[j].getAccount().equals(account)) {
                        if (account.isDebet() == items[j].isDebet()) {
                            result = result.add(items[j].getAmount());
                        } else {
                            result = result.subtract(items[j].getAmount());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Gets the balance of the specified account at start of the bookkeeping.
     * @param database the database from which to take the data
     * @param account the account
     * @return the balance of this account at start of the bookkeeping
     */
    public static Amount getStartBalance(Database database, Account account) {
        Date date = database.getStartOfPeriod();

        // Subtract one day of the period start date, because otherwise the changes
        // made on that day will be taken into account too.
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        date = cal.getTime();

        return getAccountBalance(database, account, date);
    }

    /**
     * Checks whether the specified account is used in the database. An account is considered
     * "in use" if it has a non-zero start balance or if one or more journals use the account.
     *
     * @param database the database
     * @param account the account
     * @return <code>true</code> if the account is used; <code>false</code> otherwise
     */
    public static boolean inUse(Database database, Account account) {
        if (!getStartBalance(database, account).isZero()) {
            return true;
        }

        for (Journal j : database.getJournals()) {
            for (JournalItem i : j.getItems()) {
                if (account.equals(i.getAccount())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds an account to the database.
     * @param database the database
     * @param account the account
     * @throws ServiceException if a problem occurs while creating the account
     */
	public void addAccount(Database database, Account account) throws ServiceException {
		try {
			RunTransaction.withoutResult(() -> database.getAccountDAO().create(account));
    	} catch (DataAccessException e) {
    		throw new ServiceException("Could not add account " + account.getId() + " to the database", e);
    	}
	}

	/**
	 * Updates an existing account.
	 * @param database the database
	 * @param account the account
	 * @throws ServiceException if a problem occurs while updating the account
	 */
	public void updateAccount(Database database, Account account) throws ServiceException {
		try {
            RunTransaction.withoutResult(() -> database.getAccountDAO().update(account));
        } catch (DataAccessException e) {
    		throw new ServiceException("Could not update account " + account.getId() + " in the database", e);
		}

	}

    /**
     * Deletes an account from the database. Only unused accounts can be deleted.
     * @param database the database
     * @param account the account
     * @throws ServiceException if a problem occurs while deleting the account
     */
    public void deleteAccount(Database database, Account account) throws ServiceException {
    	try {
            RunTransaction.withoutResult(() -> database.getAccountDAO().delete(account.getId()));
        } catch (DataAccessException e) {
    		throw new ServiceException("Could not delete account " + account.getId() + " from the database", e);
    	}
    }

    /**
     * Creates a report for the specified date.
     * @param database database containing the bookkeeping
     * @param date the date
     * @return the report
     * @throws ServiceException if a problem occurs
     */
    public Report createReport(Database database, Date date) throws ServiceException {
        try {
            return RunTransaction.withResult(() -> {
                ReportBuilder rb = new ReportBuilder(database, date);
                rb.setAssets(database.getAccountDAO().findAssets());
                rb.setLiabilities(database.getAccountDAO().findLiabilities());
                rb.setExpenses(database.getAccountDAO().findExpenses());
                rb.setRevenues(database.getAccountDAO().findRevenues());

                for (Journal j : database.getJournals()) {
                    if (DateUtil.compareDayOfYear(j.getDate(), date) <= 0) {
                        rb.addJournal(j);
                    }
                }

                for (Invoice invoice : database.getInvoices()) {
                    if (DateUtil.compareDayOfYear(invoice.getIssueDate(), date) <= 0) {
                        rb.addInvoice(invoice);
                    }
                }

                return rb.build();
            });
        } catch (DataAccessException e) {
            throw new ServiceException(e);
        }
    }
}
