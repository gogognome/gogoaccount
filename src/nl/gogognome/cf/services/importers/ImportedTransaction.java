/**
 *
 */
package nl.gogognome.cf.services.importers;

import java.util.Date;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;

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
			toAccount + " (" + toName + ") at " + date + " (" + description + ")";
	}
}
