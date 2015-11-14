package nl.gogognome.gogoaccount.component.importer;

import java.util.Date;

import nl.gogognome.lib.text.Amount;

public interface ImportedTransaction {

	String getFromAccount();

	String getFromName();

	Amount getAmount();

	Date getDate();

	String getToAccount();

	String getToName();

	String getDescription();

}