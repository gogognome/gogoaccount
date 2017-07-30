package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import java.util.Date;

/**
 * This class represents an invoice. An invoice consists of an amount that has to be paid
 * by a debitor. A negative amount represents an amount to be paid to a creditor.
 *
 * <p>Further an invoice has a list of payments that should sum up to the amount to be paid.
 * Negative payments represent payments to the creditor.
 */
public class Invoice implements Comparable<Invoice> {

    private final String id;

    /** Reference of the invoice as known by the party. */
    private String partyReference;

    private String partyId;

    /**
     * The amount to be paid. Negative amounts represent amounts to be received.
     *
     * <p>This is the actual amount to be paid. This amount may differ from the sum
     * of the amounts.
     */
    private Amount amountToBePaid;

    private Date issueDate;
    private String description;

    public Invoice() {
        this(null);
    }

    Invoice(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getPartyReference() {
        return partyReference;
    }

    public void setPartyReference(String partyReference) {
        this.partyReference = partyReference;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String payingPartyId) {
        this.partyId = payingPartyId;
    }

    public Amount getAmountToBePaid() {
        return amountToBePaid;
    }

    public void setAmountToBePaid(Amount amountToBePaid) {
        this.amountToBePaid = amountToBePaid;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    /**
     * Compares another invoice to this invoice. Invoices are ordered by their identifier.
     * @param that the other invoice
     * @return a negative number, zero or a positive number if this invoice is smaller than,
     *         equal to or larger than the other invoice.
     */
    @Override
	public int compareTo(Invoice that) {
        return this.id.compareTo(that.id);
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Invoice) {
            Invoice that = (Invoice) o;
            return this.id.equals(that.id);
        }
        return false;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return DateUtil.formatDateYYYYMMDD(issueDate) + ' ' + id + ' ' + description;
    }
}
