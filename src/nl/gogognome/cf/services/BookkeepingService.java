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
package nl.gogognome.cf.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.businessobjects.ReportBuilder;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Payment;


/**
 * This class offers methods to manipulate the bookkeeping.
 *
 * @author Sander Kooijmans
 */
public class BookkeepingService {

    /** Private constructor to avoid instantiation. */
    private BookkeepingService() {
        throw new IllegalStateException();
    }

    public static Database closeBookkeeping(Database database, String description, Date date,
    		Account equity) throws ServiceException {
        if (database.hasUnsavedChanges()) {
            throw new ServiceException("The bookkeeping contains unsaved changes. " +
            		"First save the changes before closing the bookkeeping.");
        }

        Database newDatabase = new Database();
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
        try {
            newDatabase.setAssets(database.getAssets());
            newDatabase.setLiabilities(database.getLiabilities());
            newDatabase.setExpenses(database.getExpenses());
            newDatabase.setRevenues(database.getRevenues());
        } catch (DatabaseModificationFailedException e) {
            throw new ServiceException("Can't set accounts for the new bookkeeping.", e);
        }

        // Create start balance
        Date dayBeforeStart = DateUtil.addDays(date, -1);

        List<JournalItem> journalItems = new ArrayList<JournalItem>(20);
        for (Account account : database.getAssets()) {
            JournalItem item = new JournalItem(getAccountBalance(database, account, dayBeforeStart),
                newDatabase.getAccount(account.getId()), true, null, null);
            if (!item.getAmount().isZero()) {
            	journalItems.add(item);
            }
        }
        for (Account account : database.getLiabilities()) {
            JournalItem item = new JournalItem(getAccountBalance(database, account, dayBeforeStart),
                newDatabase.getAccount(account.getId()), false, null, null);
            if (!item.getAmount().isZero()) {
            	journalItems.add(item);
            }
        }

        // Add the result of operations to the specified account.
        Report report = createReport(newDatabase, dayBeforeStart);
        Amount resultOfOperations = report.getResultOfOperations();
        if (resultOfOperations.isPositive()) {
            journalItems.add(new JournalItem(resultOfOperations, newDatabase.getAccount(equity.getId()), false, null, null));
        } else if (resultOfOperations.isNegative()) {
            journalItems.add(new JournalItem(resultOfOperations.negate(), newDatabase.getAccount(equity.getId()), true, null, null));
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
            if (!invoice.hasBeenPaid(dayBeforeStart)) {
                newInvoices.add(new Invoice(invoice.getId(), invoice.getPayingParty(), invoice.getConcerningParty(),
                    invoice.getAmountToBePaid(), invoice.getIssueDate(), invoice.getDescriptions(), invoice.getAmounts()));
            }
        }
        try {
            newDatabase.setInvoices(newInvoices.toArray(new Invoice[newInvoices.size()]));
            for (Invoice invoice : newInvoices) {
                for (Payment payment : database.getInvoice(invoice.getId()).getPayments()) {
                    newDatabase.addPayment(invoice.getId(), payment);
                }
            }
        } catch (DatabaseModificationFailedException e) {
            throw new ServiceException("Failed to copy open invoices to the new bookkeeping.", e);
        }

        // Notify unsaved changes in the new database.
        newDatabase.notifyChange();

        return newDatabase;
    }

    /**
     * Copies an array of accounts.
     * @param accounts the accounts to be copied
     * @param newDatabase the new database to which the new accounts belong
     * @return the copied accounts
     */
    private static Account[] copyAccounts(Account[] accounts, Database newDatabase) {
        Account[] newAccounts = new Account[accounts.length];
        for (int i=0; i<accounts.length; i++) {
            newAccounts[i] = new Account(accounts[i].getId(), accounts[i].getName(),
                accounts[i].getType());
        }
        return newAccounts;
    }

    private static Journal copyJournal(Journal journal, Database newDatabase) {
        return new Journal(journal.getId(), journal.getDescription(), journal.getDate(),
            copyJournalItems(journal.getItems(), newDatabase), journal.getIdOfCreatedInvoice());
    }

    private static JournalItem[] copyJournalItems(JournalItem[] items, Database newDatabase) {
        JournalItem[] newItems = new JournalItem[items.length];
        for (int i=0; i<items.length; i++) {
            newItems[i] = new JournalItem(items[i].getAmount(), newDatabase.getAccount(items[i].getAccount().getId()),
                items[i].isDebet(), items[i].getInvoiceId(), items[i].getPaymentId());
        }
        return newItems;
    }


    /**
     * Removes a journal from the database. Payments booked in the journal or invoices created
     * by the journal are also removed.
     * @param database the database from which the journal has to be removed
     * @param journal the journal to be deleted
     * @throws DeleteException if a problem occurs while deleting the journal
     */
    public static void removeJournal(Database database, Journal journal) throws DeleteException {
        // Check for payments without payment ID.
        for (JournalItem item : journal.getItems()) {
            if (item.getInvoiceId() != null && item.getPaymentId() == null) {
                throw new DeleteException("The journal has a payment without an id. Therefore, it cannot be removed.");
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
                        throw new DeleteException("Could not delete payment " + paymentId, e);
                    }
                }
            }

            // Check if the journal created an invoice. If so, remove the invoice too.
            if (journal.createsInvoice()) {
                try {
                    database.removeInvoice(journal.getIdOfCreatedInvoice());
                } catch (DatabaseModificationFailedException e) {
                    throw new DeleteException("Could not delete the invoice " + journal.getIdOfCreatedInvoice()
                        + " created by the journal.", e);
                }
            }

            try {
                database.removeJournal(journal);
            } catch (DatabaseModificationFailedException e) {
                throw new DeleteException("Could not delete journal.", e);
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
     * @throws CreationException if a problem occurs while creating the account
     */
	public static void addAccount(Database database, Account account) throws CreationException {
		try {
			database.addAccount(account);
    	} catch (DatabaseModificationFailedException e) {
    		throw new CreationException("Could not add account " + account.getId() + " to the database", e);
    	}
	}

	/**
	 * Updates an existing account.
	 * @param database the database
	 * @param account the account
	 * @throws ServiceException if a problem occurs while updating the account
	 */
	public static void updateAccount(Database database, Account account) throws ServiceException {
		try {
			database.updateAccount(account);
    	} catch (DatabaseModificationFailedException e) {
    		throw new ServiceException("Could not update account " + account.getId() + " in the database", e);
		}

	}

    /**
     * Deletes an account from the database. Only unused accounts can be deleted.
     * @param database the database
     * @param account the account
     * @throws DeleteException if a problem occurs while deleting the account
     */
    public static void deleteAccount(Database database, Account account) throws DeleteException {
    	try {
    		database.removeAccount(account.getId());
    	} catch (DatabaseModificationFailedException e) {
    		throw new DeleteException("Could not delete account " + account.getId() + " from the database", e);
    	}
    }

    /**
     * Creates a report for the specified date.
     * @param database contains the bookkeeping
     * @param date the date
     * @return the report
     * @throws ServiceException if a problem occurs
     */
    public static Report createReport(Database database, Date date) throws ServiceException {
    	ReportBuilder rb = new ReportBuilder(database, date);
    	rb.setAssets(database.getAssets());
    	rb.setLiabilities(database.getLiabilities());
    	rb.setExpenses(database.getExpenses());
    	rb.setRevenues(database.getRevenues());

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
    }
}
