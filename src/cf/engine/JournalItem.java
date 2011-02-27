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

import nl.gogognome.text.Amount;
import nl.gogognome.util.ComparatorUtil;

/**
 * This class represents a single item of a journal.
 *
 * @author Sander Kooijmans
 */
public class JournalItem
{
    private Amount amount;

    private Account account;

    /**
     * The id of the invoice to which the amount is booked as payment;
     * <code>null</code> if no payment is involved.
     */
    private String invoiceId;

    /**
     * The id of the payment within the invoice that corresponds to this item;
     * <code>null</code> if no payment is involved.
     */
    private String paymentId;

    /**
     * Indicates whether the amount in this items is booked on the
     * debet side (true) or credit side (false).
     */
    private boolean debet;

    /**
     * Constructs a journal item.
     * @param amount the amount
     * @param account the account on which the amount is booked
     * @param debet true if the amount is booked on the debet side; false if
     *              the amount is booked on the credit side
     * @param invoiceId the id of the invoice to which the amount is booked as payment;
     *              <code>null</code> if no invoice is involved.
     * @param paymentId the id of the payment; <code>null</code> if no payment is involved
     */
    public JournalItem(Amount amount, Account account, boolean debet, String invoiceId, String paymentId)  {
        this.amount = amount;
        this.account = account;
        this.debet = debet;
        this.invoiceId = invoiceId;
        this.paymentId = paymentId;
    }

    public Account getAccount() {
        return account;
    }

    public Amount getAmount() {
        return amount;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public boolean isDebet() {
        return debet;
    }

    public boolean isCredit() {
        return !debet;
    }

    /**
     * Checks whether this instance is equal to another instance.
     * @param o the other instance
     * @return <code>true</code> if this instance is equal to <code>o</code>;
     *          <code>false</code> otherwise
     */
    @Override
	public boolean equals(Object o) {
        if (o instanceof JournalItem) {
            JournalItem that = (JournalItem) o;
            boolean payments = ComparatorUtil.equals(this.invoiceId, that.invoiceId)
                && ComparatorUtil.equals(this.paymentId, that.paymentId);
            return this.account.equals(that.account) && this.amount.equals(that.amount)
            	&& this.debet == that.debet && payments;
        } else {
            return false;
        }
    }

    @Override
	public int hashCode() {
        return amount.hashCode() + account.hashCode();
    }
}
