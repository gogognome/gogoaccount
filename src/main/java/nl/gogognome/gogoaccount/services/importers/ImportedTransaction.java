package nl.gogognome.gogoaccount.services.importers;

import java.util.Date;

import nl.gogognome.lib.text.Amount;

public interface ImportedTransaction {

	public abstract String getFromAccount();

	public abstract String getFromName();

	public abstract Amount getAmount();

	public abstract Date getDate();

	public abstract String getToAccount();

	public abstract String getToName();

	public abstract String getDescription();

}