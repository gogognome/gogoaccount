package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.util.AmountInDatabase;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.StringUtil;

import java.sql.SQLException;
import java.util.Calendar;

class InvoiceDAO extends AbstractDomainClassDAO<Invoice> {

    InvoiceDAO(Document document) {
        super("invoice", null, document.getBookkeepingId());
    }

    public Invoice createWithNewId(String invoiceIdFormat, Invoice invoice) throws SQLException {
        String nextInvoiceId = getNextInvoiceId(invoiceIdFormat);

        NameValuePairs nvp = getNameValuePairs(invoice);
        nvp.remove("id");
        nvp.add("id", nextInvoiceId);
        insert(tableName, nvp);

        return getObjectFromResultSet(convertNameValuePairsToResultSet(nvp));
    }

    private String getNextInvoiceId(String invoiceIdFormat) throws SQLException {
        invoiceIdFormat = fillInYearAndDate(invoiceIdFormat);
        int startIndex = invoiceIdFormat.indexOf('n');
        int endIndex = startIndex;
        while (endIndex < invoiceIdFormat.length() && invoiceIdFormat.charAt(endIndex) == 'n') {
            endIndex++;
        }
        int previousSequenceNumber = getPreviousSequenceNumber(invoiceIdFormat, startIndex, endIndex);

        String formattedSequenceNumber = StringUtil.prependToSize(Integer.toString(previousSequenceNumber + 1), endIndex - startIndex, '0');
        return StringUtil.replace(invoiceIdFormat, startIndex, endIndex, formattedSequenceNumber);
    }

    private String fillInYearAndDate(String invoiceIdFormat) {
        String year = Integer.toString(DateUtil.getField(DateUtil.createNow(), Calendar.YEAR));
        String month = Integer.toString(DateUtil.getField(DateUtil.createNow(), Calendar.MONTH) + 1);
        invoiceIdFormat = invoiceIdFormat.replaceAll("yyyy", StringUtil.prependToSize(year, 4, '0'));
        invoiceIdFormat = invoiceIdFormat.replaceAll("mm", StringUtil.prependToSize(month, 2, '0'));
        return invoiceIdFormat;
    }

    private int getPreviousSequenceNumber(String invoiceIdFormat, int startIndex, int endIndex) throws SQLException {
        String pattern = StringUtil.replace(invoiceIdFormat, startIndex, endIndex, StringUtil.prependToSize("", endIndex - startIndex, '_'));
        String previousId = execute("SELECT MAX(id) FROM " + tableName + " WHERE id LIKE '" + pattern + "'").findFirst(r -> r.getString(1));
        int previousSequenceNumber;
        if (previousId != null) {
            try {
                previousSequenceNumber = Integer.parseInt(previousId.substring(startIndex, endIndex));
            } catch (NumberFormatException e) {
                throw new SQLException("Unexpected invoice id found while determining previous invoice id: " + previousId);
            }
        } else {
            previousSequenceNumber = 0;
        }
        return previousSequenceNumber;
    }

    @Override
    protected Invoice getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        Invoice invoice = new Invoice(result.getString("id"));
        invoice.setPartyReference(result.getString("party_reference"));
        invoice.setDescription(result.getString("description"));
        invoice.setIssueDate(result.getDate("issue_date"));
        invoice.setAmountToBePaid(AmountInDatabase.parse(result.getString("amount_to_be_paid")));
        invoice.setPartyId(result.getString("party_id"));
        return invoice;
    }

    @Override
    protected NameValuePairs getNameValuePairs(Invoice invoice) throws SQLException {
        return new NameValuePairs()
                .add("id", invoice.getId())
                .add("party_reference", invoice.getPartyReference())
                .add("description", invoice.getDescription())
                .add("issue_date", invoice.getIssueDate())
                .add("amount_to_be_paid", AmountInDatabase.format(invoice.getAmountToBePaid()))
                .add("party_id", invoice.getPartyId());
    }
}
