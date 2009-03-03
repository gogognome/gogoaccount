/*
 * $RCSfile: BookkeepingService.java,v $
 * Copyright (c) PharmaPartners BV
 */

package nl.gogognome.cf.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.gogognome.text.Amount;
import nl.gogognome.util.DateUtil;
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

    public static Database closeBookkeeping(Database database, String description, Date date, Account equity) throws CreationException {
        if (database.hasUnsavedChanges()) {
            throw new CreationException("The bookkeeping contains unsaved changes. " +
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
            throw new CreationException("Failed to copy parties to the new bookkeeping.", e);
        }

        // Copy the accounts
        try {
            newDatabase.setAssets(copyAccounts(database.getAssets(), newDatabase));
            newDatabase.setLiabilities(copyAccounts(database.getLiabilities(), newDatabase));
            newDatabase.setExpenses(copyAccounts(database.getExpenses(), newDatabase));
            newDatabase.setRevenues(copyAccounts(database.getRevenues(), newDatabase));
        } catch (DatabaseModificationFailedException e) {
            throw new CreationException("Can't set accounts for the new bookkeeping.", e);
        }

        // Create start balance
        Date dayBeforeStart = DateUtil.addDays(date, -1);

        List<JournalItem> journalItems = new ArrayList<JournalItem>(20);
        for (Account account : database.getAssets()) {
            JournalItem item = new JournalItem(account.getBalance(dayBeforeStart), 
                newDatabase.getAccount(account.getId()), true, null, null);
            journalItems.add(item);
        }
        for (Account account : database.getLiabilities()) {
            JournalItem item = new JournalItem(account.getBalance(dayBeforeStart), 
                newDatabase.getAccount(account.getId()), false, null, null);
            journalItems.add(item);
        }
        
        // Add the result of operations to the specified account.
        Amount resultOfOperations = database.getBalance(dayBeforeStart).getResultOfOperations();
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
            throw new CreationException("Failed to create journal for start balance.", e);
        } catch (DatabaseModificationFailedException e) {
            throw new CreationException("Failed to create journal for start balance.", e);
        }

        // Copy journals starting from the specified date
        for (Journal journal : database.getJournals()) {
            if (DateUtil.compareDayOfYear(date, journal.getDate()) <= 0) {
                try {
                    newDatabase.addJournal(copyJournal(journal, newDatabase), false);
                } catch (DatabaseModificationFailedException e) {
                    throw new CreationException("Failed to copy a journal to the new bookkeeping.", e);
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
            throw new CreationException("Failed to copy open invoices to the new bookkeeping.", e);
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
            newAccounts[i] = new Account(accounts[i].getId(), accounts[i].getName(), accounts[i].isDebet(), newDatabase);
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
}
