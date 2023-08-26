package nl.gogognome.gogoaccount.component.importer;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import com.google.common.base.*;
import nl.gogognome.lib.text.*;

/**
 * This class reads a tab-separated values (TSV) file that was created by ABN AMRO Bank.
 * A list of {@link ImportedTransaction}s is created that represents the contents
 * of the file.
 */
public class AbnAmroBankTSVTransactionImporter extends AbstractCSVTransactionImporter {

	private static final int OWN_IBAN_COLUMN = 0;
	private static final int CURRENCY_COLUMN = 1;
	private static final int DATE_COLUMN = 2;
	private static final int AMOUNT_COLUMN = 6;
	private static final int DESCRIPTION_COLUMN = 7;

	public AbnAmroBankTSVTransactionImporter() {
		super("yyyyMMdd", '\t');
	}

	@Override
	public List<ImportedTransaction> importTransactions(File file) throws IOException, ParseException {
		List<String[]> lines = readAllLinesFrom(file, StandardCharsets.UTF_8);
		return parseValues(lines);
	}

	@Override
	protected ImportedTransaction parseLine(String[] values) throws ParseException {
		Amount amount = parseAmount(getValue(values, AMOUNT_COLUMN), getValue(values, CURRENCY_COLUMN));

		String description = getValue(values, DESCRIPTION_COLUMN);
		String fromAccount = amount.isNegative() ? getValue(values, OWN_IBAN_COLUMN) : getIbanFromDescription(description);
		String fromName = amount.isNegative() ? null : getNameFromDescription(description);
		String toAccount = amount.isNegative() ? getIbanFromDescription(description) : getValue(values, OWN_IBAN_COLUMN);
		String toName = amount.isNegative() ? getNameFromDescription(description) : null;
		Date date = parseDate(getValue(values, DATE_COLUMN));

		return new ImportedTransaction(
				fromAccount,
				fromName,
				amount.isNegative() ? amount.negate() : amount,
				date,
				toAccount,
				toName,
				getDescriptionFromDescription(description));
	}

	private String getNameFromDescription(String description) {
		return descriptionHasSlashes(description)
				? getValueFromDescriptionWithSlashesByKey("NAME", description)
				: getNameFromDescriptionWithoutSlashes(description);
	}

	private String getNameFromDescriptionWithoutSlashes(String description) {
		int nameIndex = description.indexOf("Naam:");
		int descriptionIndex = description.indexOf("Omschrijving:");
		int authorizationIndex = description.indexOf("Machtiging:");
		// Name is followed by either authorization or description. Find out which one to use as end index.
		int nameEndIndex = authorizationIndex != -1 ? authorizationIndex : descriptionIndex;

		if (nameIndex == -1 || nameEndIndex == -1 || nameIndex > nameEndIndex) {
			return null;
		}
		return trim(description.substring(nameIndex + 5, nameEndIndex));
	}

	private String getIbanFromDescription(String description) {
		return descriptionHasSlashes(description)
				? getValueFromDescriptionWithSlashesByKey("IBAN", description)
				: getIbanFromDescriptionWithoutSlashes(description);
	}

	private String getIbanFromDescriptionWithoutSlashes(String description) {
		int ibanIndex = description.indexOf("IBAN:");
		int bicIndex = description.indexOf("BIC:");
		if (bicIndex == -1) {
			bicIndex = description.length();
		}

		if (ibanIndex == -1 || ibanIndex > bicIndex) {
			return null;
		}
		return trim(description.substring(ibanIndex + 5, bicIndex));
	}

	private String getDescriptionFromDescription(String description) {
		return descriptionHasSlashes(description)
				? getDescriptionFromDescriptionWithSlashes(description)
				: getDescriptionFromDescriptionWithoutSlashes(description);
	}

	private boolean descriptionHasSlashes(String description) {
		return description.startsWith("/");
	}

	private String getDescriptionFromDescriptionWithSlashes(String description) {
		String part1 = getValueFromDescriptionWithSlashesByKey("REMI", description);

		String part2 = null;
		if (!description.contains("/EREF/NOTPROVIDED")) {
			part2 = getValueFromDescriptionWithSlashesByKey("EREF", description);
		}

		if (part1 == null && part2 == null) {
			return description;
		}

		return Joiner.on(' ')
				.skipNulls()
				.join(part1, part2);
	}

	private String getValueFromDescriptionWithSlashesByKey(String key, String description) {
		String start = "/" + key + "/";
		int startIndex = description.indexOf(start);
		int endIndex = description.indexOf("/", startIndex +  start.length());
		if (endIndex == -1) {
			endIndex = description.length();
		}
		if (startIndex == -1 || startIndex > endIndex) {
			return null;
		}
		return trim(description.substring(startIndex + start.length(), endIndex));
	}

	private String getDescriptionFromDescriptionWithoutSlashes(String description) {
		int descriptionIndex = description.indexOf("Omschrijving:");
		if (descriptionIndex == -1) {
			return trim(description);
		}
		return trim(description.substring(descriptionIndex + 13)
				.replace("Kenmerk:", "")
				.replace("NOTPROVIDED", ""));
	}
}
