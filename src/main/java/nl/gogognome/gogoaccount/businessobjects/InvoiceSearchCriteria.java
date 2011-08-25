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

import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.services.InvoiceService;



/**
 * This class represents search criteria for invoices.
 *
 * @author Sander Kooijmans
 */
public class InvoiceSearchCriteria {

    private String id;
    private String name;
    private boolean includeClosedInvoices;

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
    public boolean areClosedInvoicesIncluded() {
        return includeClosedInvoices;
    }
    public void setIncludeClosedInvoices(boolean includeClosedInvoices) {
        this.includeClosedInvoices = includeClosedInvoices;
    }

    /**
     * Checks whether the specified <code>Invoice</code> matches these criteria.
     * @param invoice the invoice
     * @return <code>true</code> if the invoice matches the criteria,
     *          <code>false</code> otherwise
     */
    public boolean matches(Database database, Invoice invoice) {
        boolean matches = true;
        if (id != null) {
            matches = matches && matches(id, invoice.getId());
        }
        if (name != null) {
            matches = matches && matches(name, invoice.getPayingParty().getName());
        }
        if (!includeClosedInvoices) {
            matches = matches && !InvoiceService.isPaid(database, invoice.getId(), new Date());
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
