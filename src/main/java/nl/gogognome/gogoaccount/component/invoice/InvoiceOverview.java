package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.lib.text.Amount;

public class InvoiceOverview extends Invoice {

    private Amount amountToBePaid;
    private Amount amountPaid;
    private String payingPartyName;

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
}
