package nl.gogognome.gogoaccount.component.invoice;

import java.util.Date;

import nl.gogognome.gogoaccount.components.document.Document;

/**
 * This class represents search criteria for invoices.
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

}
