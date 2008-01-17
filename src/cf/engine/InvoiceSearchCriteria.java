/*
 * $Id: InvoiceSearchCriteria.java,v 1.2 2008-01-17 20:51:57 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;


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
    public boolean matches(Invoice invoice) {
        boolean matches = true;
        if (id != null) {
            matches = matches && matches(id, invoice.getId());
        }
        if (name != null) {
            matches = matches && matches(name, invoice.getPayingParty().getName());
        }
        if (!includeClosedInvoices) {
            matches = matches && !invoice.hasBeenPaid();
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
