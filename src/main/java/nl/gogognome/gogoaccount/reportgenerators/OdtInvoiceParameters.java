package nl.gogognome.gogoaccount.reportgenerators;

import java.util.Date;
import java.util.List;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.document.Document;


/**
 * This class contains the parameters needed for generating
 * and ODT file containing invoices.
 */
public class OdtInvoiceParameters {

	private final Document document;
	private final List<Invoice> invoices;
    private String concerning;
    private Date date;
    private Date dueDate;
    private String ourReference;

	public OdtInvoiceParameters(Document document, List<Invoice> invoices) {
		this.document = document;
		this.invoices = invoices;
		this.date = new Date();
	}

	public String getConcerning() {
		return concerning;
	}

	public void setConcerning(String concerning) {
		this.concerning = concerning;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public String getOurReference() {
		return ourReference;
	}

	public void setOurReference(String ourReference) {
		this.ourReference = ourReference;
	}

	public Document getDocument() {
		return document;
	}

	public List<Invoice> getInvoices() {
		return invoices;
	}
}
