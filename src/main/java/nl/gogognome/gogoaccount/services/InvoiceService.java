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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.businessobjects.Party;
import nl.gogognome.gogoaccount.businessobjects.Payment;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.database.DatabaseModificationFailedException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.lib.util.StringUtil;

/**
 * This class offers methods for handling invoices.
 *
 * @author Sander Kooijmans
 */
public class InvoiceService {

    /** Private constructor to avoid instantiation. */
    private InvoiceService() {
        throw new IllegalStateException();
    }

    /**
     * Creates invoices and journals for a number of parties.
     * @param database the database to which the invoices are to be added.
     * @param id the id of the invoices
     * @param parties the parties
     * @param issueDate the date of issue of the invoices
     * @param description an optional description for the invoices
     * @param invoiceLineDefinitions the lines of a single invoice
     * @throws ServiceException if a problem occurs while creating invoices for one or more of the parties
     */
    public static void createInvoiceAndJournalForParties(Database database, String id, List<Party> parties,
            Date issueDate, String description, List<InvoiceLineDefinition> invoiceLineDefinitions) throws ServiceException {
        // Validate the input.
        if (issueDate == null) {
            throw new ServiceException("No date has been specified!");
        }

        boolean amountToBePaidSelected = false;
        boolean changedDatabase = false;
        TextResource tr = Factory.getInstance(TextResource.class);
		for (InvoiceLineDefinition line : invoiceLineDefinitions) {
            if (!amountToBePaidSelected) {
                amountToBePaidSelected = line.isAmountToBePaid();
            } else {
                if (line.isAmountToBePaid()) {
                    throw new ServiceException(tr.getString("InvoiceService.moreThanOneAmountToBePaid"));
                }
            }
            if (line.getDebet() == null && line.getCredit() == null) {
                throw new ServiceException(tr.getString("InvoiceService.lineWithoutAmount"));
            }
            if (line.getDebet() != null && line.getCredit() != null) {
                throw new ServiceException(tr.getString("InvoiceService.lineWithTwoAmounts"));
            }

            if (line.getAccount() == null) {
                throw new ServiceException(tr.getString("InvoiceService.lineWithoutAccount"));
            }
        }

        if (!amountToBePaidSelected) {
            throw new ServiceException(tr.getString("InvoiceService.noAmountToBePaied"));
        }

        List<Party> partiesForWhichCreationFailed = new LinkedList<Party>();

        for (Party party : parties) {
            String specificId = replaceKeywords(id, party);
            String specificDescription =
                !StringUtil.isNullOrEmpty(description) ? replaceKeywords(description, party) : null;

            // First create the invoice instance. It is needed when the journal is created.
            int size = invoiceLineDefinitions.size() - 1;
            if (specificDescription != null) {
                size++;
            }
            String[] descriptions = new String[size];
            Amount[] amounts = new Amount[size];
            int descriptionIndex = 0;
            if (specificDescription != null) {
                descriptions[0] = specificDescription;
                descriptionIndex++;
            }

            Amount amountToBePaid = null;
            for (InvoiceLineDefinition line : invoiceLineDefinitions) {
                Amount amount = line.getDebet();
                boolean debet = amount != null;
                if (line.isAmountToBePaid()) {
                    amountToBePaid = amount;
                }
                if (amount == null) {
                    amount = line.getCredit();
                    if (line.isAmountToBePaid()) {
                        amountToBePaid = amount.negate();
                    }
                }

                assert amount != null; // has been checked before

                if (!line.isAmountToBePaid()) {
                    descriptions[descriptionIndex] = line.getAccount().getName();
                    amounts[descriptionIndex] = debet ? amount.negate() : amount;
                    descriptionIndex++;
                }
            }

            assert amountToBePaid != null; // has been checked before

            if (database.getInvoice(specificId) != null) {
                specificId = database.suggestNewInvoiceId(specificId);
            }
            Invoice invoice = new Invoice(specificId, party, party, amountToBePaid, issueDate, descriptions, amounts);

            // Create the journal.
            JournalItem[] items = new JournalItem[invoiceLineDefinitions.size()];
            int n = 0;
            for (InvoiceLineDefinition line : invoiceLineDefinitions) {
                Account account = line.getAccount();
                assert account != null; // has been checked before
                Amount amount = line.getDebet();
                boolean debet = amount != null;
                if (amount == null) {
                    amount = line.getCredit();
                }



                assert amount != null; // has been checked before
                items[n] = new JournalItem(amount, account, debet, null, null);
                n++;
            }

            Journal journal;
            try {
                journal = new Journal(specificId, specificDescription, issueDate, items, specificId);
            } catch (IllegalArgumentException e) {
                throw new ServiceException("The debet and credit amounts are not in balance!", e);
            }

            try {
                database.addInvoicAndJournal(invoice, journal);
                changedDatabase = true;
            } catch (DatabaseModificationFailedException e) {
                partiesForWhichCreationFailed.add(party);
            }
        }

        if (changedDatabase) {
            database.notifyChange();
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
    }

    /**
     * Replaces the keywords <code>{id}</code> and <code>{name}</code> with the corresponding
     * attributes of the specified party.
     * @param s the string in which the replacement has to be made
     * @param party the party
     * @return the string after the replacements have taken place
     */
    private static String replaceKeywords(String s, Party party) {
        StringBuilder sb = new StringBuilder(s);
        String[] keywords = new String[] { "{id}", "{name}" };
        String[] values = new String[] {
                party.getId(), party.getName()
        };

        for (int k=0; k<keywords.length; k++) {
            String keyword = keywords[k];
            String value = values[k];
            for (int index=sb.indexOf(keyword); index != -1; index=sb.indexOf(keyword)) {
                sb.replace(index, index+keyword.length(), value);
            }
        }
        return sb.toString();
    }

    /**
     * Gets the amount that has to be paid for this invoice minus
     * the payments that have been made.
     * @param database database containing the bookkeeping
     * @param invoiceId the id of the invoice
     * @param date the date for which the amount has to be determined.
     * @return the remaining amount that has to be paid
     */
    public static Amount getRemainingAmountToBePaid(Database database, String invoiceId, Date date) {
    	Invoice invoice = database.getInvoice(invoiceId);
        Amount result = invoice.getAmountToBePaid();
        for (Payment p : database.getPayments(invoiceId)) {
            if (DateUtil.compareDayOfYear(p.getDate(), date) <= 0) {
                result = result.subtract(p.getAmount());
            }
        }
        return result;
    }

    /**
     * Checks whether an invoice has been paid.
     * @param database database containing the bookkeeping
     * @param invoiceId the id of the invoice
     * @param date the date for which the amount has to be determined.
     * @return true if the invoice has been paid; false otherwise
     */
    public static boolean isPaid(Database database, String invoiceId, Date date) {
    	return getRemainingAmountToBePaid(database, invoiceId, date).isZero();
    }

    /**
     * Gets all payments for the specified invoice.
     * @param database database containing the bookkeeping
     * @param invoiceId the ID of the invoice
     * @return the payments
     */
    public static List<Payment> getPayments(Database database, String invoiceId) {
        return database.getPayments(invoiceId);
    }

}
