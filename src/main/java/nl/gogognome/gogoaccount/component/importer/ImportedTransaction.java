package nl.gogognome.gogoaccount.component.importer;

import java.util.Date;

import nl.gogognome.lib.text.Amount;

public record ImportedTransaction(
		String fromAccount,
		String fromName,
		Amount amount,
		Date date,
		String toAccount,
		String toName,
		String description
) { }