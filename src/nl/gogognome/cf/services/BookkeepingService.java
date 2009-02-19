/*
 * $RCSfile: BookkeepingService.java,v $
 * Copyright (c) PharmaPartners BV
 */

package nl.gogognome.cf.services;

import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import nl.gogognome.text.Amount;
import nl.gogognome.util.DateUtil;


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

    public static Database closeBookkeeping(Database database, Date date, Account eigenVermogen) throws CreationException {
        if (database.hasUnsavedChanges()) {
            throw new CreationException("The bookkeeping contains unsaved changes. " +
            		"First save the changes before closing the bookkeeping.");
        }

        Database newDatabase = new Database();
        newDatabase.setCurrency(database.getCurrency());
        newDatabase.setDescription(database.getDescription());
        newDatabase.setFileName(null);
        newDatabase.setStartOfPeriod(date);

        try {
            newDatabase.setAssets(database.getAssets());
            newDatabase.setLiabilities(database.getLiabilities());
            newDatabase.setExpenses(database.getExpenses());
            newDatabase.setRevenues(database.getRevenues());
        } catch (DatabaseModificationFailedException e) {
            throw new CreationException("Can't set accounts for the new bookkeeping.", e);
        }

        // Create start balance
        Date dayBeforeStart = DateUtil.addDays(date, -1);

        List<JournalItem> journalItems = new ArrayList<JournalItem>(20);
        for (Account account : database.getAssets()) {
            JournalItem item = new JournalItem(account.getBalance(dayBeforeStart), account, true, null, null);
            journalItems.add(item);
        }
        for (Account account : database.getLiabilities()) {
            JournalItem item = new JournalItem(account.getBalance(dayBeforeStart), account, false, null, null);
            journalItems.add(item);
        }

        // Add the result of operations to the specified account.
        Amount resultOfOperations = database.getBalance(dayBeforeStart).getResultOfOperations();
        if (resultOfOperations.isPositive()) {
            journalItems.add(new JournalItem(resultOfOperations, eigenVermogen, false, null, null));
        } else if (resultOfOperations.isNegative()) {
            journalItems.add(new JournalItem(resultOfOperations.negate(), eigenVermogen, true, null, null));
        }

        // Copy journals starting from the specified date
        for (Journal journal : database.getJournals()) {
            if (DateUtil.compareDayOfYear(date, journal.getDate()) >= 0) {
                try {
                    newDatabase.addJournal(journal, false);
                } catch (DatabaseModificationFailedException e) {
                    throw new CreationException("Failed to copy a journal to the new bookkeeping.", e);
                }
            }
        }

        // Copy the open invoices including their payments
        List<Invoice> newInvoices = new ArrayList<Invoice>(100);
        for (Invoice invoice : database.getInvoices()) {
            if (!invoice.hasBeenPaid(dayBeforeStart)) {
                newInvoices.add(invoice);
            }
        }
        try {
            newDatabase.setInvoices(newInvoices.toArray(new Invoice[newInvoices.size()]));
        } catch (DatabaseModificationFailedException e) {
            throw new CreationException("Failed to copy open invoices to the new bookkeeping.", e);
        }

        // Copy the parties
        try {
            newDatabase.setParties(database.getParties());
        } catch (DatabaseModificationFailedException e) {
            throw new CreationException("Failed to copy parties to the new bookkeeping.", e);
        }

        // Notify unsaved changes in the new database.
        newDatabase.notifyChange();

        return newDatabase;
    }
}
