package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.lib.text.Amount;

public class InvoiceDetail {

    private final long id;
    private String invoiceId;
    private String description;
    private Amount amount;

    public InvoiceDetail() {
        this(-1);
    }

    public InvoiceDetail(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getInvoiceId() { return invoiceId; }

    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }
}
