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
package nl.gogognome.cf.services.importers;

import java.util.Date;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;

/**
 * This class represents a transaction that has been imported.
 *
 * @author Sander Kooijmans
 */
public class ImportedTransaction {

	private final String fromAccount;

	private final String fromName;

	private final Amount amount;

	private final Date date;

	private final String toAccount;

	private final String toName;

	private final String description;

	public ImportedTransaction(String fromAccount, String fromName,
			Amount amount, Date date, String toAccount, String toName,
			String description) {
		super();
		this.fromAccount = fromAccount;
		this.fromName = fromName;
		this.amount = amount;
		this.date = date;
		this.toAccount = toAccount;
		this.toName = toName;
		this.description = description;
	}

	@Override
	public String toString() {
		AmountFormat af =  TextResource.getInstance().getAmountFormat();
		return af.formatAmount(amount) + ' ' + fromAccount + " (" + fromName + ") -> " +
			toAccount + " (" + toName + ") at " + DateUtil.formatDateYYYYMMDD(date) +
			" (" + description + ")";
	}
}
