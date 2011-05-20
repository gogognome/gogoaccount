/**
 *
 */
package nl.gogognome.cf.services.importers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nl.gogognome.csv.CsvFileParser;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;


/**
 * This class reads a comma separated values file that was created by the Rabobank.
 * A list of {@link ImportedTransaction}s is created that represents the contents
 * of the CSV file.
 *
 * @author Sander Kooijmans
 */
public class RabobankCSVImporter {

	private File csvFile;

	private List<ImportedTransaction> transactions = new ArrayList<ImportedTransaction>();

	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private final static AmountFormat AMOUNT_FORMAT = new AmountFormat(Locale.US);

	public RabobankCSVImporter(File csvFile) throws FileNotFoundException {
		this.csvFile = csvFile;
	}

	/**
	 * Reads the CSV file and returns a list of transactions.
	 * @return the transactions
	 * @throws IOException if a problem occurred while reading the file
	 * @throws ParseException if a problem occurred while interpreting the file
	 */
	public List<ImportedTransaction> importTransactions() throws IOException, ParseException {
		CsvFileParser parser = new CsvFileParser(csvFile);
		parseValues(parser.getValues());
		return transactions;
	}

	private void parseValues(String[][] values) throws ParseException {
		for (int line=0; line<values.length; line++) {
			parseLine(values[line]);
		}
	}

	private void parseLine(String[] values) throws ParseException {
		if (values.length == 16) {
			if ("C".equals(values[3])) {
				parseCreditTransaction(values);
			} else if ("D".equals(values[3])) {
				parseDebetTransaction(values);
			} else {
				throw new ParseException("Invalid BY_AF_CODE");
			}
		}
	}

	private void parseDebetTransaction(String[] values) throws ParseException {
		String fromAccount = values[0];
		String toAccount = values[5];
		String toName = values[6];
		Date date = parseDate(values);
		Amount amount = parseAmount(values);
		String description = parseDescription(values);

		transactions.add(new ImportedTransaction(fromAccount, null, amount, date, toAccount,
				toName, description));
	}

	private void parseCreditTransaction(String[] values) throws ParseException {
		String fromAccount = values[5];
		String fromName = values[6];
		String toAccount = values[0];
		Date date = parseDate(values);
		Amount amount = parseAmount(values);
		String description = parseDescription(values);

		transactions.add(new ImportedTransaction(fromAccount, fromName, amount, date, toAccount,
				null, description));
	}

	private Date parseDate(String[] values) throws ParseException {
		String dateString = values[7];
		try {
			return DATE_FORMAT.parse(dateString);
		} catch (java.text.ParseException e) {
			throw new ParseException("\"" + dateString + "\" is not a valid date.", e);
		}
	}

	private Amount parseAmount(String[] values) throws ParseException {
		Currency currency = parseCurrency(values);
		String amountString = values[4];

		try {
			return AMOUNT_FORMAT.parse(amountString, currency);
		} catch (java.text.ParseException e) {
			throw new ParseException("\"" + amountString + "\" is not a valid amount.", e);
		}
	}

	private Currency parseCurrency(String[] values) throws ParseException {
		String currencyCode = values[1];
		try {
			return Currency.getInstance(currencyCode);
		} catch (IllegalArgumentException e) {
			throw new ParseException("\"" + currencyCode + "\" is not a valid currency code.", e);
		}
	}

	private String parseDescription(String[] values) {
		StringBuilder sb = new StringBuilder(100);
		for (int i=10; i<=15; i++) {
			sb.append(values[i]);
		}
		return sb.toString();
	}

}
