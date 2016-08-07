package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.lib.text.Amount;

import java.util.Date;

public class InvoiceOverview {

    private String invoiceId;
    private String description;
    private Date issueDate;
    private Amount amountToBePaid;
    private Amount amountPaid;
    private String partyId;
    private String partyName;

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
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
