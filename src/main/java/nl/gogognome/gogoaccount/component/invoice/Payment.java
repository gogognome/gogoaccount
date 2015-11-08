package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import java.util.Date;

/**
 * This class specifies a payment for an invoice.
 */
public class Payment {
    private String id;

    private String invoiceId;
    private Amount amount;
    private Date date;
    private String description;

    public Payment(String id) {
        this.id = id;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Payment) {
            Payment that = (Payment)o;
            return this.id.equals(that.id);
        }
        return false;
    }

    @Override
	public int hashCode() {
        return (int) id.hashCode() * 51;
    }

    @Override
    public String toString() {
    	return DateUtil.formatDateYYYYMMDD(date) + ' ' + id + ' ' + description;
    }
}
