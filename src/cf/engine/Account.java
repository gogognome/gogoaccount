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


/**
 * This class represents an account.
 *
 * @author Sander Kooijmans
 */
public class Account implements Comparable<Account> {

    public enum Type {
        ASSET, LIABILITY, EXPENSE, REVENUE
    }

    /** The id of this account. The id's of all accounts must be unique. */
    private String id;

    /** The name of this account. */
    private String name;

    /**
     * Indicates whether this account is on the debet side of a balance or
     * of the results.
     * <code>true</code> indicates this account is an asset or a expense;
     * <code>false</code> indicates this account is a liability or a revenue.
     */
    private boolean debet;

    /** The type of this account. */
    private Type type;

    /**
     * Constructs an acount.
     * @param id the id of this account
     * @param name the name of this account
     * @param debet Indicates whether this account is on the debet side of a balance or
     *              of the results.
     * <code>true</code> indicates this account is an asset or a expense;
     * <code>false</code> indicates this account is a liability or a revenue.
     * @param type the type of this account
     */
    public Account(String id, String name, boolean debet, Type type) {
        this.id = id;
        this.name = name;
        this.debet = debet;
        this.type = type;
    }

    /**
     * Gets the id of this account.
     * @return the id of this account
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the name of this account.
     * @return the name of this account
     */
    public String getName() {
        return name;
    }

    /**
     * Checks whether this account is on the debet side of a balance or
     * of the results.
     * <code>true</code> indicates this account is an asset or a expense;
     * <code>false</code> indicates this account is a liability or a revenue.
     */
    public boolean isDebet() {
        return debet;
    }

    /**
     * Gets the type of this account.
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Account) {
            return this.id.equals(((Account)o).getId());
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    public int compareTo(Account that) {
        return this.id.compareTo(that.id);
    }

}
