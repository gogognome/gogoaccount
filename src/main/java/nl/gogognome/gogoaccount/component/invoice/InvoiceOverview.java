package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.lib.text.Amount;

import java.util.Date;

public class InvoiceOverview extends Invoice {

    private Amount amountToBePaid;
    private Amount amountPaid;
    private String partyId;
    private String partyName;

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

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }
}
