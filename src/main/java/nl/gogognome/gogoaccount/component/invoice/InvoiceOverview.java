package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.lib.text.Amount;

public class InvoiceOverview extends Invoice {

    private Amount amountToBePaid;
    private Amount amountPaid;
    private String payingPartyName;
    private String payingPartyEmailAddress;
    private String payingPartyRemarks;
    private InvoiceSending lastSending;

    public InvoiceOverview(String id) {
        super(id);
    }

    public Amount getAmountToBePaid() {
        return amountToBePaid;
    }

    public void setAmountToBePaid(Amount amountToBePaid) {
        this.amountToBePaid = amountToBePaid;
    }

    public Amount getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Amount amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPayingPartyName() {
        return payingPartyName;
    }

    public void setPayingPartyName(String payingPartyName) {
        this.payingPartyName = payingPartyName;
    }

    public boolean isSalesInvoice() {
        return !amountToBePaid.isNegative();
    }

    public String getPayingPartyEmailAddress() {
        return payingPartyEmailAddress;
    }

    public void setPayingPartyEmailAddress(String payingPartyEmailAddress) {
        this.payingPartyEmailAddress = payingPartyEmailAddress;
    }

    public String getPayingPartyRemarks() {
        return payingPartyRemarks;
    }

    public void setPayingPartyRemarks(String payingPartyRemarks) {
        this.payingPartyRemarks = payingPartyRemarks;
    }

    public void setLastSending(InvoiceSending lastSending) {
        this.lastSending = lastSending;
    }

    public InvoiceSending getLastSending() {
        return lastSending;
    }
}
