/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package cf.engine;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

/**
 * This class represents a journal.
 *
 * @author Sander Kooijmans
 */
public class Journal implements Comparable<Journal> {

    private String id;

    private String description;

    private JournalItem[] items;

    private Date date;

    /**
     * If not <code>null</code>, then this contains the id of the invoice that is created
     * by this journal. Journals that create an invoice can only be deleted as long as
     * no payments have been made to the invoice.
     */
    private String idOfCreatedInvoice;

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
    public Journal(String id, String description, Date date, List<JournalItem> items, String idOfCreatedInvoice) {
    	this(id, description, date, items.toArray(new JournalItem[items.size()]),
    			idOfCreatedInvoice);
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
    public Journal(String id, String description, Date date, JournalItem[] items, String idOfCreatedInvoice) {
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


	/**
     * Gets the date of this journal.
     * @return the date of this journal.
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * Gets the description of this journal
     * @return the description of this journal.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Gets the id of this journal.
     * @return the id of this journal.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Gets the items of this journal.
     * @return the items of this journal.
     */
    public JournalItem[] getItems()
    {
        return items;
    }

    /**
     * Gets the id of the invoice created by this journal. If no invoice was created, then
     * <code>null</code> is returned
     * @return the id or <code>null</code>
     */
    public String getIdOfCreatedInvoice() {
        return idOfCreatedInvoice;
    }

    /**
     * Indicates whether this journal creates an invoice.
     * @return <code>true</code> if it creates an invoice; otherwise <code>false</code>
     */
    public boolean createsInvoice() {
        return getIdOfCreatedInvoice() != null;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
	public int compareTo(Journal that) {
        return this.date.compareTo(that.date);
    }

    /**
     * Checks whether this instance is equal to another instance.
     * @param o the other instance
     * @return <code>true</code> if this instance is equal to <code>o</code>;
     *          <code>false</code> otherwise
     */
    @Override
	public boolean equals(Object o) {
        if (o instanceof Journal) {
            Journal that = (Journal) o;
            return this.id.equals(that.id) && this.date.equals(that.date) &&
            	this.description.equals(that.description) &&
            	Arrays.equals(this.items, that.items);
        } else {
            return false;
        }
    }

    @Override
	public int hashCode() {
        return id.hashCode() + date.hashCode() + description.hashCode();
    }

}
