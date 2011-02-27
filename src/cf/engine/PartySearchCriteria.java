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
            matches = matches && type.equals(party.getType());
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
