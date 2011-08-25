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


/**
 * This class represents a party. A party can be either a debtor or a creditor.
 *
 * @author Sander Kooijmans
 */
public class Party implements Comparable<Party> {
    /**  The id of the party. The id must be unique for all parties. */
    private String id;

    /** The name of this party. */
    private String name;

    /** The address of this party. */
    private String address;

    /** The zip code of this party. */
    private String zipCode;

    /** The city of this party. */
    private String city;

    /** The birth date of this party. */
    private Date birthDate;

    /** THe type of this party. */
    private String type;

    /** Contains remarks about this party. */
    private String remarks;

    /**
     * Constructs a party.
     * @param name the name of the party.
     * @param name the name of the party.
     */
    public Party(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Constructs a party.
     * @param id the id of the party
     * @param name the name of the party.
     * @param address the address of the party
     * @param zipCode the zip code of the party
     * @param city the city where the party lives
     * @param birthDate the birth date of the party
     * @param type the type of the party
     * @param remarks remarks about the party
     */
    public Party(String id, String name, String address, String zipCode, String city, Date birthDate,
        String type, String remarks) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.zipCode = zipCode;
        this.city = city;
        this.birthDate = birthDate;
        this.type = type;
        this.remarks = remarks;
    }

    /**
     * Gets the id of this party.
     * @return the id of this party
     */
    public String getId()
    {
        return id;
    }

    /**
     * Gets the name of this party.
     * @return the name of this party
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gets the address of this party.
     * @return the address of this party
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Gets the zip code of this party.
     * @return the zip code of this party
     */
    public String getZipCode()
    {
        return zipCode;
    }

    /**
     * Gets the city of this party.
     * @return the city of this party
     */
    public String getCity()
    {
        return city;
    }

    /**
     * Gets the hash code for this instance.
     * @return the hash code
     */
    @Override
	public int hashCode() {
        return id.hashCode();
    }

    /**
     * Gets the birth date of this party.
     * @return the birth date of this party
     */
    public Date getBirthDate() {
        return birthDate;
    }

    /**
     * Gets the type of this party.
     * @return the type of this party
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the remarks for this party.
     * @return the remarks for this party
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * Checks whether this instance equals another instance.
     * @param that the instance to which this instance is compared.
     * @return <code>true</code> if this instance equals <code>that</code>;
     *         <code>false</code> otherwise.
     */
    @Override
	public boolean equals(Object that)
    {
        boolean result = false;
        if (that instanceof Party)
        {
            Party thatParty = (Party)that;
            result = this.id.equals(thatParty.id) && this.name.equals(thatParty.name);
        }
        return result;
    }

    @Override
	public String toString() {
        return id + ' ' + name;
    }

    @Override
	public int compareTo(Party that) {
        return this.id.compareTo(that.id);
    }
}
