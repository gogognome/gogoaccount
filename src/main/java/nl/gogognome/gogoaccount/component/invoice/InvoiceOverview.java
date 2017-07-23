package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.lib.text.Amount;

public class InvoiceOverview extends Invoice {

    private Amount amountToBePaid;
    private Amount amountPaid;
    private String partyReference;
    private String partyName;
    private String partyEmailAddress;
    private String partyRemarks;
    private InvoiceSending lastSending;

    public InvoiceOverview(String id) {
        super(id);
    }

    @Override
    public String getPartyReference() {
        return partyReference;
    }

    @Override
    public void setPartyReference(String partyReference) {
        this.partyReference = partyReference;
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

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public boolean isSalesInvoice() {
        return !amountToBePaid.isNegative();
    }

    public String getPartyEmailAddress() {
        return partyEmailAddress;
    }

    public void setPartyEmailAddress(String partyEmailAddress) {
        this.partyEmailAddress = partyEmailAddress;
    }

    public String getPartyRemarks() {
        return partyRemarks;
    }

    public void setPartyRemarks(String partyRemarks) {
        this.partyRemarks = partyRemarks;
    }

    public void setLastSending(InvoiceSending lastSending) {
        this.lastSending = lastSending;
    }

    public InvoiceSending getLastSending() {
        return lastSending;
    }
}
