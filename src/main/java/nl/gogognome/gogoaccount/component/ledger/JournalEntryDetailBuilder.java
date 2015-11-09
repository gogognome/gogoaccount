package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;

import java.text.ParseException;
import java.util.Locale;

public class JournalEntryDetailBuilder {

    private final JournalEntryDetail detail = new JournalEntryDetail();

    public static JournalEntryDetailBuilder debet() {
        JournalEntryDetailBuilder builder = new JournalEntryDetailBuilder();
        builder.detail.setDebet(true);
        return builder;
    }

    public static JournalEntryDetailBuilder credit() {
        JournalEntryDetailBuilder builder = new JournalEntryDetailBuilder();
        builder.detail.setDebet(false);
        return builder;
    }

    public JournalEntryDetail build() {
        return detail;
    }

    public JournalEntryDetailBuilder amount(String amount) {
        try {
            detail.setAmount(Factory.getInstance(AmountFormat.class).parse(amount));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Could not parse amount " + amount, e);
        }
        return this;
    }

    public JournalEntryDetailBuilder account(String accountId) {
        detail.setAccountId(accountId);
        return this;
    }

    public JournalEntryDetailBuilder invoiceId(String invoiceId) {
        detail.setInvoiceId(invoiceId);
        return this;
    }

    public JournalEntryDetailBuilder paymentId(String paymentId) {
        detail.setPaymentId(paymentId);
        return this;
    }
}
