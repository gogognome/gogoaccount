package nl.gogognome.gogoaccount.component.importer;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import nl.gogognome.lib.text.*;


/**
 * This class reads a comma separated values file that was created by the Rabobank for a credit card.
 * A list of {@link ImportedTransaction}s is created that represents the contents
 * of the CSV file.
 */
public class RabobankCreditCardCSVImporter extends AbstractCSVTransactionImporter  {

	private static final String CREDITCARD_NUMBER = "Creditcard Nummer";
	private static final String PRODUCT_NAME = "Productnaam";
	private static final String AMOUNT = "Bedrag";
	private static final String CURRENCY = "Munt";
	private static final String DATE = "Datum";
	private static final String DESCRIPTION = "Omschrijving";

	public RabobankCreditCardCSVImporter() {
		super("yyyy-MM-dd", ',');
	}

	@Override
	public List<ImportedTransaction> importTransactions(File file) throws IOException, ParseException {
		List<String[]> lines = readAllLinesFrom(file, Charset.forName("windows-1252"));
		parseHeader(lines.get(0));
		return parseValues(lines.subList(1, lines.size()));
	}

	@Override
	protected ImportedTransaction parseLine(String[] values) throws ParseException {
		Amount amount = parseAmount(getValue(values, AMOUNT), getValue(values, CURRENCY));

		String creditcard =  getValue(values, PRODUCT_NAME) + " " + getValue(values, CREDITCARD_NUMBER);
		String fromAccount = amount.isNegative() ? creditcard : null;
		String fromName = null;
		String toAccount = !amount.isNegative() ? creditcard : null;
		String toName = null;
		Date date = parseDate(getValue(values, DATE));
		String description = getValue(values, DESCRIPTION);

		return new ImportedTransaction(
				fromAccount,
				fromName,
				amount.isNegative() ? amount.negate() : amount,
				date,
				toAccount,
				toName,
				description);
	}

}
