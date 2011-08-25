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
package nl.gogognome.gogoaccount.businessobjects;

import java.util.Calendar;
import java.util.Date;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.StringUtil;

/**
 * This class specifies a payment.
 *
 *  @author Sander Kooijmans
 */
public class Payment {
    /** The id of this payment. Ids must be unique within an invoice. */
    private String id;

    /** The amount that has been paid. */
    private Amount amount;

    /** The date of the payment. */
    private Date date;

    /** A description of the payment. */
    private String description;

    /**
     * Constructor.
     * None of the arguments can be <code>null</code>
     * @param id the id of this payment
     * @param amount the amount
     * @param date the date
     * @param description the description
     */
    public Payment(String id, Amount amount, Date date, String description) {
        if (StringUtil.isNullOrEmpty(id)) {
            throw new IllegalArgumentException("ID must not be null or empty!");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null!");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null!");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description must not be null!");
        }
        this.id = id;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public Amount getAmount() {
        return amount;
    }


    public Date getDate() {
        return date;
    }


    public String getDescription() {
        return description;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
	public boolean equals(Object o) {
        if (o instanceof Payment) {
            Payment that = (Payment)o;
            return this.id.equals(that.id)
                && this.amount.equals(that.amount)
                && DateUtil.compareDayOfYear(this.date, that.date) == 0
                && this.description.equals(that.description);

        }
        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
	public int hashCode() {
        return id.hashCode() * 51 + amount.hashCode() + DateUtil.getField(date, Calendar.YEAR) * 23
            + DateUtil.getField(date, Calendar.DAY_OF_YEAR) + description.hashCode();
    }

    @Override
    public String toString() {
    	return DateUtil.formatDateYYYYMMDD(date) + ' ' + id + ' ' + description;
    }
}
