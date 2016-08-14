package nl.gogognome.lib.text;

import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;

/**
 * This class represents amounts. It should be used instead of floats, doubles,
 * ints or longs, since floats and doubles suffer from rounding differences
 * and all of them lack checks for overflows.
 */
public class Amount implements Comparable<Amount>
{
    /** Represents the amount in cents. */
    private BigInteger amount;

    /**
     * Constructs an amount.
     * @param amount the amount in cents
     */
    public Amount(BigInteger amount) {
        this.amount = amount;
    }

    /**
     * Constructs an amount.
     * @param amount a string representation of the amount in cents
     */
    public Amount(String amount) {
        this.amount = new BigInteger(amount);
    }

    public Amount add(Amount that) {
        return new Amount(this.amount.add(that.amount));
    }

    public Amount subtract(Amount that) {

        return new Amount(this.amount.subtract(that.amount));
    }

    public Amount divide(int val) {
        return new Amount(this.amount.divide(new BigInteger(Integer.toString(val))));
    }

    public Amount multiply(int val) {
        return new Amount(this.amount.multiply(new BigInteger(Integer.toString(val))));
    }

    public Amount negate() {
        return new Amount(amount.negate());
    }

    public int compareTo(Amount that)
    {
        return amount.compareTo(that.amount);
    }

    /**
     * Checks whether this amount is positive.
     * @return <code>true</code> if this amount is positive; <code>false</code> otherwise.
     */
    public boolean isPositive() {
        return amount.signum() == 1;
    }

    /**
     * Checks whether this amount is negative.
     * @return <code>true</code> if this amount is negative; <code>false</code> otherwise.
     */
    public boolean isNegative() {
        return amount.signum() == -1;
    }

    /**
     * Checks whether this amount is zero.
     * @return <code>true</code> if this amount is zero; <code>false</code> otherwise
     */
    public boolean isZero() {
        return amount.signum() == 0;
    }

    public BigInteger toBigInteger() {
        return amount;
    }

    /**
     * Gets a string representation of this amount.
     * The string representation will be the result of the underlying
     * <code>BigInteger</code>'s <code>toString()</code> method.
     * @return a string representation of this amount
     */
    @Override
	public String toString() {
        return amount.toString();
    }

    /**
     * Checks whether this instance is equal to another instance.
     * @param o the other instance
     * @return <code>true</code> if this instance is equal to <code>o</code>;
     *          <code>false</code> otherwise
     */
    @Override
	public boolean equals(Object o) {
        if (o instanceof Amount) {
            Amount that = (Amount) o;
            return this.amount.equals(that.amount);
        } else {
            return false;
        }
    }

    @Override
	public int hashCode() {
        return amount.hashCode();
    }

    /**
     * Adds to amounts. Null is allowed for the parameters
     * @param a an amount, null allowed
     * @param b an amount, null allowed
     * @return the sum of a and b or null if both a and b are null
     */
    public static Amount add(Amount a, Amount b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            return a.add(b);
        }
    }

    /**
     * Checks whether a and b are equal amounts.
     * @param a an amount, null allowed
     * @param b an amount, null allowed
     * @return true if a and b are both null or if a and b are both not null then a and b are equal; false otherwise
     */
    public static boolean areEqual(Amount a, Amount b) {
        if (a == null && b == null) {
            return true;
        } else if (a != null && b != null) {
            return a.equals(b);
        } else {
            return false;
        }
    }


}
