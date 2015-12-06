package nl.gogognome.gogoaccount.component.importer;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.DateUtil;
import au.com.bytecode.opencsv.CSVReader;


/**
 * This class reads a comma separated values file that was created by the Rabobank.
 * A list of {@link ImportedTransactionRabobankCsv}s is created that represents the contents
 * of the CSV file.
 */
public class RabobankCSVImporter implements TransactionImporter {

	private List<ImportedTransaction> transactions = new ArrayList<>();

	public RabobankCSVImporter() {
	}

	@Override
	public List<ImportedTransaction> importTransactions(Reader reader)
			throws IOException, ParseException {
		CSVReader csvReader = new CSVReader(reader);
		parseValues(csvReader.readAll());
		reader.close();
		return transactions;
	}

	private void parseValues(List<String[]> values) throws ParseException {
		for (String[] line : values) {
			parseLine(line);
		}
	}

	private void parseLine(String[] values) throws ParseException {
		if (values.length == 19) {
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

		transactions.add(new ImportedTransactionRabobankCsv(fromAccount, null, amount, date, toAccount,
				toName, description));
	}

	private void parseCreditTransaction(String[] values) throws ParseException {
		String fromAccount = values[5];
		String fromName = values[6];
		String toAccount = values[0];
		Date date = parseDate(values);
		Amount amount = parseAmount(values);
		String description = parseDescription(values);

		transactions.add(new ImportedTransactionRabobankCsv(fromAccount, fromName, amount, date, toAccount,
				null, description));
	}

	private Date parseDate(String[] values) throws ParseException {
		String dateString = values[2];
		try {
			return DateUtil.parseDateYYYYMMDD(dateString);
		} catch (java.text.ParseException e) {
			throw new ParseException("\"" + dateString + "\" is not a valid date.", e);
		}
	}

	private Amount parseAmount(String[] values) throws ParseException {
		Currency currency = parseCurrency(values);
		String amountString = values[4];

		try {
			return new Amount(new AmountFormat(Locale.US, currency).parse(amountString, currency));
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
