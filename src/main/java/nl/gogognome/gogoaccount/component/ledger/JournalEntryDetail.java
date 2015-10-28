package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.lib.text.Amount;

public class JournalEntryDetail {

    private final long id;

    private long journalEntryUniqueId;

    private Amount amount;

    private String accountId;

    /**
     * The id of the invoice to which the amount is booked as payment;
     * <code>null</code> if no payment is involved.
     */
    private String invoiceId;

    /**
     * The id of the payment within the invoice that corresponds to this item;
     * <code>null</code> if no payment is involved.
     */
    private String paymentId;

    /**
     * Indicates whether the amount in this items is booked on the
     * debet side (true) or credit side (false).
     */
    private boolean debet;

    public JournalEntryDetail() {
        this(-1);
    }

    public JournalEntryDetail(long id) {
        this.id = id;
    }

    public long getJournalEntryUniqueId() {
        return journalEntryUniqueId;
    }

    public void setJournalEntryUniqueId(long journalEntryUniqueId) {
        this.journalEntryUniqueId = journalEntryUniqueId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public long getId() {
        return id;
    }

    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public boolean isDebet() {
        return debet;
    }

    public void setDebet(boolean debet) {
        this.debet = debet;
    }

    public boolean isCredit() {
        return !isDebet();
    }

    /**
     * Checks whether this instance is equal to another instance.
     * @param o the other instance
     * @return <code>true</code> if this instance is equal to <code>o</code>;
     *          <code>false</code> otherwise
     */
    @Override
	public boolean equals(Object o) {
        if (o instanceof JournalEntryDetail) {
            JournalEntryDetail that = (JournalEntryDetail) o;
            return this.getId() == that.getId();
        } else {
            return false;
        }
    }

    @Override
	public int hashCode() {
        return Long.hashCode(id);
    }
}
