/*
 * $Id: Party.java,v 1.13 2007-09-09 19:39:56 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.Date;


/**
 * This class represents a party. A party can be either a debtor or a creditor.
 *
 * @author Sander Kooijmans
 */
public class Party implements Comparable {
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
    
    /**
     * Constructs a party.
     * @param name the name of the party.
     * @param journals the journals that modify the balance of this debtor or
     *                 creditor.
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
     */
    public Party(String id, String name, String address, String zipCode, String city, Date birthDate) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.zipCode = zipCode;
        this.city = city;
        this.birthDate = birthDate;
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
     * Checks whether this instance equals another instance.
     * @param that the instance to which this instance is compared.
     * @return <code>true</code> if this instance equals <code>that</code>;
     *         <code>false</code> otherwise.
     */
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
    
    public String toString() {
        return id + " " + name + " " + address + " "  + zipCode + " " + city + " " + birthDate;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        Party that = (Party)o;
        return this.id.compareTo(that.id);
    }
}
