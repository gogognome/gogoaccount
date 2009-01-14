/*
 * $Id: InvoiceService.java,v 1.1 2009-01-14 21:32:15 sanderk Exp $
 */

package nl.gogognome.cf.services;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import nl.gogognome.swing.MessageDialog;
import nl.gogognome.text.Amount;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.StringUtil;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Invoice;
import cf.engine.Journal;
import cf.engine.JournalItem;
import cf.engine.Party;

/**
 * This class offers methods for handling invoices.
 * 
 * @author Sander Kooijmans
 */
public class InvoiceService {

 
    /**
     * Creates invoices and journals for a number of parties.
     * @param database the database to which the invoices are to be added.
     * @param id the id of the invoices
     * @param parties the parties
     * @param issueDate the date of issue of the invoices
     * @param description an optional description for the invoices 
     * @param invoiceDefinitions the lines of a single invoice 
     * @throws CreationException if a problem occurs while creating invoices for one or more of the parties
     */
    public static void createInvoiceAndJournalForParties(Database database, String id, List<Party> parties, 
            Date issueDate, String description, List<InvoiceLineDefinition> invoiceDefinitions) throws CreationException {
        // Validate the input.
        if (issueDate == null) {
            throw new CreationException("No date has been specified!"); 
        }

        boolean amountToBePaidSelected = false;
        boolean changedDatabase = false;
        for (InvoiceLineDefinition line : invoiceDefinitions) {
            if (!amountToBePaidSelected) {
                amountToBePaidSelected = line.isAmountToBePaid();
            } else {
                if (line.isAmountToBePaid()) {
                    throw new CreationException(TextResource.getInstance().getString("More than one amount to be paid has been specified!"));
                }
            }
            if (line.getDebet() == null && line.getCredit() == null) {
                throw new CreationException(TextResource.getInstance().getString("A line without amount has been found!"));
            }
            if (line.getDebet() != null && line.getCredit() != null) {
                throw new CreationException(TextResource.getInstance().getString("A line with two amounts has been found!"));
            }
            
            if (line.getAccount() == null) {
                throw new CreationException(TextResource.getInstance().getString("A line without an account has been found"));
            }
        }
        
        if (!amountToBePaidSelected) {
            throw new CreationException(TextResource.getInstance().getString("No amount to be paid has been specified"));
        }

        List<Party> partiesForWhichCreationFailed = new LinkedList<Party>();
        
        for (Party party : parties) {
            String specificId = replaceKeywords(id, party);
            String specificDescription = 
                !StringUtil.isNullOrEmpty(description) ? replaceKeywords(description, party) : null;
            
            // First create the invoice instance. It is needed when the journal is created.
            int size = invoiceDefinitions.size() - 1;
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
            for (InvoiceLineDefinition line : invoiceDefinitions) {
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
            JournalItem[] items = new JournalItem[invoiceDefinitions.size()];
            int n = 0;
            for (InvoiceLineDefinition line : invoiceDefinitions) {
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
                throw new CreationException("The debet and credit amounts are not in balance!", e);
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
                throw new CreationException("Failed to create journal for " + party.getId() + " - " + party.getName());
            } else {
                StringBuilder sb = new StringBuilder(1000);
                for (Party party : partiesForWhichCreationFailed) {
                    sb.append('\n').append(party.getId()).append(" - ").append(party.getName());
                }
                throw new CreationException("Failed to create journal for the parties:" + sb.toString());
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

}
