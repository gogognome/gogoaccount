/*
 * $Id: PartySearchCriteria.java,v 1.4 2007-12-13 21:16:30 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.util.Date;

import nl.gogognome.util.DateUtil;

/**
 * This class represents search criteria for parties. 
 *
 * @author Sander Kooijmans
 */
public class PartySearchCriteria {

    private String id;
    private String name;
    private String address;
    private String zipCode;
    private String city;
    private Date birthDate;
    private String type;
    
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getZipCode() {
        return zipCode;
    }
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    public Date getBirthDate() {
        return birthDate;
    }
    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Checks whether the specified <code>Party</code> matches these criteria.
     * @param party the party
     * @return <code>true</code> if the party matches the criteria,
     *          <code>false</code> otherwise
     */
    public boolean matches(Party party) {
        boolean matches = true;
        if (id != null) {
            matches = matches && matches(id, party.getId());
        }
        if (name != null) {
            matches = matches && matches(name, party.getName());
        }
        if (address != null) {
            matches = matches && matches(address, party.getAddress());
        }
        if (zipCode != null) {
            matches = matches && matches(zipCode, party.getZipCode());
        }
        if (city != null) {
            matches = matches && matches(city, party.getCity());
        }
        if (birthDate != null) {
            if (party.getBirthDate() != null) {
                matches = matches && DateUtil.compareDayOfYear(birthDate, party.getBirthDate()) == 0;
            } else {
                matches = false;
            }
        }
        if (type != null) {
            matches = type.equals(party.getType());
        }
        return matches;
    }
    
    /**
     * Checks whether a specified criteria matches a specified value.
     * @param criteria the criteria
     * @param value the value
     * @return <code>true</code> if the criteria matches;
     *          <code>false</code> otherwise
     */
    private boolean matches(String criteria, String value) {
        return value != null && value.toLowerCase().indexOf(criteria.toLowerCase()) != -1;
    }
}
