package nl.gogognome.gogoaccount.component.importer;

import java.io.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import com.google.common.base.*;
import au.com.bytecode.opencsv.*;
import nl.gogognome.lib.text.*;


/**
 * This class reads a comma separated values file that was created by the Rabobank.
 * A list of {@link ImportedTransaction}s is created that represents the contents
 * of the CSV file.
 */
public class RabobankCSVImporter implements TransactionImporter {

	private static final String OWN_IBAN = "IBAN/BBAN";
	private static final String OTHER_IBAN = "Tegenrekening IBAN/BBAN";
	private static final String OTHER_NAME = "Naam tegenpartij";
	private static final String AMOUNT = "Bedrag";
	private static final String CURRENCY = "Munt";
	private static final String DATE = "Datum";
	private static final String DESCRIPTION_1 = "Omschrijving-1";

	private final Locale DUTCH = new Locale("nl");
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", DUTCH);

	private final Map<String, Integer> headerToIndex = new HashMap<>();
	private List<ImportedTransaction> transactions = new ArrayList<>();


	public RabobankCSVImporter() {
	}

	@Override
	public List<ImportedTransaction> importTransactions(File file) throws IOException, ParseException {
		try (FileReader reader = new FileReader(file, Charset.forName("windows-1252"))) {
			return importTransactions(reader);
		}
	}

	public List<ImportedTransaction> importTransactions(Reader reader) throws IOException, ParseException {
		CSVReader csvReader = new CSVReader(reader);
		List<String[]> lines = csvReader.readAll();
		parseHeader(lines.get(0));
		parseValues(lines.subList(1, lines.size()));
		return transactions;
	}

	private void parseHeader(String[] header) {
		for (int index=0; index<header.length; index++) {
			headerToIndex.put(header[index], index);
		}
	}

	private void parseValues(List<String[]> values) throws ParseException {
		for (String[] line : values) {
			parseLine(line);
		}
	}

	private void parseLine(String[] values) throws ParseException {
		Amount amount = parseAmount(getValue(values, AMOUNT), getValue(values, CURRENCY));

		String fromAccount = getValue(values, amount.isNegative() ? OWN_IBAN : OTHER_IBAN);
		String fromName = amount.isNegative() ? null : getValue(values, OTHER_NAME);
		String toAccount = getValue(values, amount.isNegative() ? OTHER_IBAN : OWN_IBAN);
		String toName = amount.isNegative() ? getValue(values, OTHER_NAME) : null;
		Date date = parseDate(getValue(values, DATE));
		String description = getValue(values, DESCRIPTION_1);

		transactions.add(new ImportedTransaction(
				fromAccount,
				fromName,
				amount.isNegative() ? amount.negate() : amount,
				date,
				toAccount,
				toName,
				description));
	}

	private String getValue(String[] values, String column) throws ParseException {
		Integer index = headerToIndex.get(column);
		if (index == null) {
			throw new ParseException("The file does not have a column '" + column + "'.");
		}
		return Strings.emptyToNull(values[index]);
	}

	private Date parseDate(String dateAsString) throws ParseException {
		try {
			return dateFormat.parse(dateAsString);
		} catch (java.text.ParseException e) {
			throw new ParseException("The date '" + dateAsString + "' has an invalid format.");
		}
	}

	private Amount parseAmount(String amount, String currencyCode) throws ParseException {
		Currency currency = parseCurrency(currencyCode);

		try {
			return new Amount(new AmountFormat(DUTCH, currency).parse(amount));
		} catch (java.text.ParseException e) {
			throw new ParseException("\"" + amount + "\" is not a valid amount.", e);
		}
	}

	private Currency parseCurrency(String currencyCode) throws ParseException {
		try {
			return Currency.getInstance(currencyCode);
		} catch (IllegalArgumentException e) {
			throw new ParseException("\"" + currencyCode + "\" is not a valid currency code.", e);
		}
	}

}
