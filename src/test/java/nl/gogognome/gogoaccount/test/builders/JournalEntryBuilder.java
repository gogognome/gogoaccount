package nl.gogognome.gogoaccount.test.builders;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.lib.text.Amount;

import java.util.Date;

public class JournalEntryBuilder {

    private static int nextId = 1;

    public static JournalEntryDetail debet(int amountInt, Account account) throws ServiceException {
        return buildDetail(amountInt, account.getId(), true);
    }

    public static JournalEntryDetail credit(int amountInt, Account account) throws ServiceException {
        return buildDetail(amountInt, account.getId(), false);
    }

    public static JournalEntryDetail buildDetail(int amountInt, String accountId, boolean debet) throws ServiceException {
        Amount amount = AmountBuilder.build(amountInt);
        JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
        journalEntryDetail.setAmount(amount);
        journalEntryDetail.setAccountId(accountId);
        journalEntryDetail.setDebet(debet);
        return journalEntryDetail;
    }

    public static JournalEntryDetail buildDetail(int amountInt, String accountId, boolean debet, String invoiceId, String paymentId) throws ServiceException {
        Amount amount = AmountBuilder.build(amountInt);
        JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
        journalEntryDetail.setAmount(amount);
        journalEntryDetail.setAccountId(accountId);
        journalEntryDetail.setDebet(debet);
        journalEntryDetail.setInvoiceId(invoiceId);
        journalEntryDetail.setPaymentId(paymentId);
        return journalEntryDetail;
    }

    public static JournalEntry build(Date date, String description) {
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setId(Integer.toString(nextId++));
        journalEntry.setDate(date);
        journalEntry.setDescription(description);
        return journalEntry;
    }
}
