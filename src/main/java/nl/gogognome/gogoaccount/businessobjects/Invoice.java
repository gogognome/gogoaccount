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

import java.util.Date;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.text.Amount;

/**
 * This class represents an invoice. An invoice consists of an amount that has to be paid
 * by a debitor. A negative amount represents an amount to be paid to a creditor.
 *
 * <p>Further an invoice has a list of payments that should sum up to the amount to be paid.
 * Negative payments represent payments to the creditor.
 */
public class Invoice implements Comparable<Invoice> {

    private String id;

    /** The party that has to pay the invoice. */
    private Party payingParty;

    /**
     * The party to whom this invoice is concerned. If this party is a minor, then typically
     * the paying party will be the parent of this party.
     */
    private Party concerningParty;

    /**
     * The amount to be paid. Negative amounts represent amounts to be received.
     *
     * <p>This is the actual amount to be paid. This amount may differ from the sum
     * of the {@link #amounts}.
     */
    private Amount amountToBePaid;

    private Date issueDate;

    /**
     * Contains details about this invoice. None of the elements may be <code>null</code>.
     * Descriptions may be associated to amounts.
     */
    private String[] descriptions;

    /**
     * Contains amounts that belongs to the descriptions.
     * If an element is <code>null</code>, then the description has no corresponding amount.
     *
     * <p>These amounts can be used to explain the total amount to be paid. Not necessarily
     * will the sum of these amounts be equal to {@link #amountToBePaid}.
     *
     * <p>Invariant: <code>amounts.length == descriptions.length</code>
     */
    private Amount[] amounts;

    public Invoice(String id, Party payingParty, Party concerningParty, Amount amountToBePaid,
            Date issueDate, String[] descriptions, Amount[] amounts) {
        if (id == null) {
            throw new IllegalArgumentException("The id must not be null");
        }
        if (payingParty == null) {
            throw new IllegalArgumentException("The paying party must not be null");
        }
        if (amountToBePaid == null) {
            throw new IllegalArgumentException("The amount to be paid must not be null");
        }

        this.id = id;
        this.payingParty = payingParty;
        this.concerningParty = concerningParty;
        this.amountToBePaid = amountToBePaid;
        this.issueDate = issueDate;
        this.descriptions = descriptions;
        this.amounts = amounts;
    }

    public Amount getAmountToBePaid() {
        return amountToBePaid;
    }

    public Amount[] getAmounts() {
        return amounts;
    }

    public Party getConcerningParty() {
        return concerningParty;
    }

    public String[] getDescriptions() {
        return descriptions;
    }

    public String getId() {
        return id;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public Party getPayingParty() {
        return payingParty;
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
}
