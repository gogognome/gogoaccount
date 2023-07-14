package nl.gogognome.gogoaccount.component.importer;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import nl.gogognome.lib.text.*;

/**
 * This class reads a comma separated values file that was created by KNAB.
 * A list of {@link ImportedTransaction}s is created that represents the contents
 * of the CSV file.
 */
public class KnabCSVImporter extends AbstractCSVTransactionImporter {

	private static final String INDICATOR = "KNAB EXPORT";
	private static final String CREDIT_DEBIT = "CreditDebet";
	private static final String AMOUNT = "Bedrag";
	private static final String CURRENCY = "Valutacode";
	private static final String OWN_IBAN = "Rekeningnummer";
	private static final String OTHER_IBAN = "Tegenrekeningnummer";
	private static final String OTHER_NAME = "Tegenrekeninghouder";
	private static final String DATE = "Transactiedatum";
	private static final String DESCRIPTION = "Omschrijving";

	public KnabCSVImporter() {
		super("dd-MM-yyyy", ';');
	}

	@Override
	public List<ImportedTransaction> importTransactions(File file) throws IOException, ParseException {
		List<String[]> lines = readAllLinesFrom(file, StandardCharsets.UTF_8);
		validateIndicator(lines.get(0));
		parseHeader(lines.get(1));
		return parseValues(lines.subList(2, lines.size()));
	}

	private void validateIndicator(String[] indicatorLine) throws ParseException {
		if (!INDICATOR.equals(indicatorLine[0])) {
			throw new ParseException("The file does not start with the indicator '" + INDICATOR + "'");
		}
	}

	@Override
	protected ImportedTransaction parseLine(String[] values) throws ParseException {
		String amountString = getValue(values, AMOUNT);
		if (!amountString.contains(",")) {
			amountString += ",00";
		}

		Amount amount = parseAmount(amountString, getValue(values, CURRENCY));
		boolean debet = "D".equals(getValue(values, CREDIT_DEBIT));

		String fromAccount = getValue(values, debet ? OWN_IBAN : OTHER_IBAN);
		String fromName = debet ? null : getValue(values, OTHER_NAME);
		String toAccount = getValue(values, debet ? OTHER_IBAN : OWN_IBAN);
		String toName = debet ? getValue(values, OTHER_NAME) : null;
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
