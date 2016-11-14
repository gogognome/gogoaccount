package nl.gogognome.gogoaccount.component.invoice;

import java.util.Date;

public class InvoiceSending {

    public enum Type {
        EMAIL,
        PDF,
        PRINT
    }

    private final long id;
    private String invoiceId;
    private Date date;
    private Type type;

    public InvoiceSending() {
        this(0);
    }

    public InvoiceSending(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
