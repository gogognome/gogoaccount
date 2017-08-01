package nl.gogognome.gogoaccount.component.settings;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.StringUtil;

import java.sql.SQLException;
import java.util.Calendar;

import static nl.gogognome.lib.util.StringUtil.replace;

class FormattedIdGenerator {

    String findNextId(Document document, String previousId, String format) throws ServiceException, SQLException {
        format = fillInYearAndDate(format);
        int startIndex = format.indexOf('n');
        int endIndex = startIndex;
        while (endIndex < format.length() && format.charAt(endIndex) == 'n') {
            endIndex++;
        }

        long previousSequenceNumber = getPreviousSequenceNumber(previousId, format, startIndex, endIndex);

        String formattedSequenceNumber = StringUtil.prependToSize(Long.toString(previousSequenceNumber + 1), endIndex - startIndex, '0');
        return replace(format, startIndex, endIndex, formattedSequenceNumber);
    }

    private long getPreviousSequenceNumber(String previousId, String format, int startIndex, int endIndex) throws ServiceException {
        if (previousId == null || previousId.length() != format.length()) {
            return 0;
        }
        if (!replace(previousId, startIndex, endIndex, "").equals(replace(format, startIndex, endIndex, ""))) {
            return 0;
        }
        try {
            return Long.parseLong(previousId.substring(startIndex, endIndex));
        } catch (NumberFormatException e) {
            throw new ServiceException("Invalid number found in previous id: " + previousId);
        }
    }

    private String fillInYearAndDate(String invoiceIdFormat) {
        String year = Integer.toString(DateUtil.getField(DateUtil.createNow(), Calendar.YEAR));
        String month = Integer.toString(DateUtil.getField(DateUtil.createNow(), Calendar.MONTH) + 1);
        invoiceIdFormat = invoiceIdFormat.replaceAll("yyyy", StringUtil.prependToSize(year, 4, '0'));
        invoiceIdFormat = invoiceIdFormat.replaceAll("mm", StringUtil.prependToSize(month, 2, '0'));
        return invoiceIdFormat;
    }

}
