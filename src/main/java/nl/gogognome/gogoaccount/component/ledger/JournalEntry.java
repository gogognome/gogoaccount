package nl.gogognome.gogoaccount.component.ledger;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.DateUtil;

import com.google.common.base.Objects;

public class JournalEntry implements Comparable<JournalEntry> {

    private final long uniqueId;

    private String id;

    private String description;

    private Date date;

    /**
     * If not <code>null</code>, then this contains the id of the invoice that is created
     * by this journal. Journals that create an invoice can only be deleted as long as
     * no payments have been made to the invoice.
     */
    private String idOfCreatedInvoice;

    public JournalEntry() {
        this(-1);
    }

    public JournalEntry(long uniqueId) {
        this.uniqueId = uniqueId;
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

    public String getIdOfCreatedInvoice() {
        return idOfCreatedInvoice;
    }

    public void setIdOfCreatedInvoice(String idOfCreatedInvoice) {
        this.idOfCreatedInvoice = idOfCreatedInvoice;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    /**
     * Indicates whether this journal creates an invoice.
     * @return <code>true</code> if it creates an invoice; otherwise <code>false</code>
     */
    public boolean createsInvoice() {
        return getIdOfCreatedInvoice() != null;
    }

    @Override
    public int compareTo(JournalEntry that) {
        return this.date.compareTo(that.date);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JournalEntry) {
            JournalEntry that = (JournalEntry) o;
            return this.uniqueId == that.uniqueId;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(uniqueId);
    }

    @Override
    public String toString() {
        return DateUtil.formatDateYYYYMMDD(date) + ' ' + id + ' ' + description;
    }
}
