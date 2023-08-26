package nl.gogognome.gogoaccount.component.importer;

import java.io.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import com.google.common.annotations.*;
import com.google.common.base.*;
import au.com.bytecode.opencsv.*;
import nl.gogognome.lib.text.*;

abstract class AbstractCSVTransactionImporter implements TransactionImporter {

	private final Locale DUTCH = new Locale("nl");
	private final DateFormat dateFormat;
	private final char separator;

	private final Map<String, Integer> headerToIndex = new HashMap<>();

	protected AbstractCSVTransactionImporter(String dateFormat, char separator) {
		this.dateFormat = new SimpleDateFormat(dateFormat, DUTCH);
		this.separator = separator;
	}

	protected List<String[]> readAllLinesFrom(File file, Charset charset) throws IOException {
		try (Reader reader = getReader(file, charset)) {
			CSVReader csvReader = new CSVReader(reader, separator);
			return csvReader.readAll();
		}
	}

	@VisibleForTesting
	protected Reader getReader(File file, Charset charset) throws IOException {
		return new FileReader(file, charset);
	}

	protected void parseHeader(String[] header) {
		for (int index=0; index<header.length; index++) {
			headerToIndex.put(header[index], index);
		}
	}

	protected List<ImportedTransaction> parseValues(List<String[]> values) throws ParseException {
		List<ImportedTransaction> transactions = new ArrayList<>();
		for (String[] line : values) {
			transactions.add(parseLine(line));
		}
		return transactions;
	}

	protected abstract ImportedTransaction parseLine(String[] values) throws ParseException;

	protected String getValue(String[] values, String columnName) throws ParseException {
		Integer columnIndex = headerToIndex.get(columnName);
		if (columnIndex == null) {
			throw new ParseException("The file does not have a column '" + columnName + "'.");
		}
		return getValue(values, columnIndex);
	}

	protected String getValue(String[] values, int columnIndex) {
		return Strings.emptyToNull(trim(values[columnIndex]));
	}

	protected String trim(String s) {
		return s
				.replaceAll("\\s+", " ") // replace multiple spaces by a single space.
				.trim(); // replace space at begin and end.
	}

	protected Date parseDate(String dateAsString) throws ParseException {
		try {
			return dateFormat.parse(dateAsString);
		} catch (java.text.ParseException e) {
			throw new ParseException("The date '" + dateAsString + "' has an invalid format.");
		}
	}

	protected Amount parseAmount(String amount, String currencyCode) throws ParseException {
		Currency currency = parseCurrency(currencyCode);

		try {
			return new Amount(new AmountFormat(DUTCH, currency).parse(amount));
		} catch (java.text.ParseException e) {
			throw new ParseException("\"" + amount + "\" is not a valid amount.", e);
		}
	}

	protected Currency parseCurrency(String currencyCode) throws ParseException {
		try {
			return Currency.getInstance(currencyCode);
		} catch (IllegalArgumentException e) {
			throw new ParseException("\"" + currencyCode + "\" is not a valid currency code.", e);
		}
	}

}
