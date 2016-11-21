package nl.gogognome.gogoaccount.component.party;

import java.util.Date;
import java.util.List;


/**
 * This class represents a party. A party can be either a debtor or a creditor.
 */
public class Party implements Comparable<Party> {
    /**  The id of the party. The id must be unique for all parties. */
    private final String id;

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

    private List<String> tags;

    /** Contains remarks about this party. */
    private String remarks;

    public Party(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
	public int hashCode() {
        return id.hashCode();
    }

    @Override
	public boolean equals(Object that)
    {
        boolean result = false;
        if (that instanceof Party)
        {
            Party thatParty = (Party)that;
            result = this.id.equals(thatParty.id);
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
