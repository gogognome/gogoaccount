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

    public JournalEntry(long uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Creates a journal.
     * @param id the id of the journal
     * @param description the description of the journal
     * @param date the date of the journal
     * @param items the items of the journal. The sums of the debet
     *        and credit amounts must be equal!
     * @param idOfCreatedInvoice if not <code>null</code>, then this contains the id of the invoice
     *        that is created by this journal
     * @throws IllegalArgumentException if the sum of debet and credit amounts differ
     *          or if more than one item with a party has been specified.
     */
    private JournalEntry(String id, String description, Date date, JournalEntryDetail[] items, String idOfCreatedInvoice) {
        this.id = id;
        this.description = description;
        this.date = date;
        this.items = items;
        this.idOfCreatedInvoice = idOfCreatedInvoice;

        Amount totalDebet = null;
        Amount totalCredit = null;
        for (int i=0; i<items.length; i++) {
            if (items[i].isDebet()) {
                totalDebet = addNullable(totalDebet, items[i].getAmount());
            } else {
                totalCredit = addNullable(totalCredit, items[i].getAmount());
            }
        }

        if (!nullableAmountsEqual(totalDebet, totalCredit)) {
            AmountFormat af = new AmountFormat(Locale.getDefault());
            throw new IllegalArgumentException(
                    "The sum of debet and credit amounts differ for journal " + id
                    + "! debet: " + af.formatAmount(totalDebet) +
                    "; credit: " + af.formatAmount(totalCredit));
        }
    }

    private Amount addNullable(Amount a, Amount b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            return a.add(b);
        }
    }

    private boolean nullableAmountsEqual(Amount a, Amount b) {
        if (a == null && b == null) {
            return true;
        } else if (a != null && b != null) {
            return a.equals(b);
        } else {
            return false;
        }
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
