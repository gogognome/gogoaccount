package nl.gogognome.gogoaccount.component.importer;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import nl.gogognome.lib.text.*;

/**
 * This class reads a comma separated values file that was created by the Rabobank
 * for a business account. A list of {@link ImportedTransaction}s is created that represents
 * the contents of the CSV file.
 */
public class RabobankBusinessCSVImporter extends AbstractCSVTransactionImporter  {

	private static final String OWN_IBAN = "IBAN/BBAN";
	private static final String OWN_NAME = "Naam rekeninghouder";
	private static final String OTHER_IBAN = "IBAN/BBAN tegenpartij";
	private static final String OTHER_NAME = "Naam tegenpartij";
	private static final String AMOUNT = "Bedrag";
	private static final String CURRENCY = "Valuta";
	private static final String DATE = "Datum";
	private static final String DESCRIPTION_1 = "Omschrijving - 1";

	public RabobankBusinessCSVImporter() {
		super("dd-MM-yyyy", ',');
	}

	@Override
	public List<ImportedTransaction> importTransactions(File file) throws IOException, ParseException {
		List<String[]> lines = readAllLinesFrom(file, StandardCharsets.UTF_8);
		parseHeader(lines.get(0));
		return parseValues(lines.subList(1, lines.size()));
	}

	@Override
	protected ImportedTransaction parseLine(String[] values) throws ParseException {
		Amount amount = parseAmount(getValue(values, AMOUNT), getValue(values, CURRENCY));

		String fromAccount = getValue(values, amount.isNegative() ? OWN_IBAN : OTHER_IBAN);
		String fromName = getValue(values, amount.isNegative() ? OWN_NAME : OTHER_NAME);
		String toAccount = getValue(values, amount.isNegative() ? OTHER_IBAN : OWN_IBAN);
		String toName = getValue(values, amount.isNegative() ? OTHER_NAME : OWN_NAME);
		Date date = parseDate(getValue(values, DATE));
		String description = getValue(values, DESCRIPTION_1);

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
