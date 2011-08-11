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
package nl.gogognome.gogoaccount.reportgenerators;

import java.util.Date;
import java.util.List;

import cf.engine.Database;
import cf.engine.Invoice;

/**
 * This class contains the parameters needed for generating
 * and ODT file containing invoices.
 *
 * @author Sander Kooijmans
 */
public class OdtInvoiceParameters {

	private final Database database;
	private final List<Invoice> invoices;
    private String concerning;
    private Date date;
    private Date dueDate;
    private String ourReference;

	public OdtInvoiceParameters(Database database, List<Invoice> invoices) {
		super();
		this.database = database;
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

	public Database getDatabase() {
		return database;
	}

	public List<Invoice> getInvoices() {
		return invoices;
	}
}
