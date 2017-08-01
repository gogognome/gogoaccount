package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.util.AmountInDatabase;
import nl.gogognome.lib.util.StringUtil;

import java.sql.SQLException;

class InvoiceDAO extends AbstractDomainClassDAO<Invoice> {

    InvoiceDAO(Document document) {
        super("invoice", null, document.getBookkeepingId());
    }

    public Invoice createWithNewId(String invoiceId, Invoice invoice) throws SQLException {
        NameValuePairs nvp = getNameValuePairs(invoice);
        nvp.remove("id");
        nvp.add("id", invoiceId);
        insert(tableName, nvp);

        return getObjectFromResultSet(convertNameValuePairsToResultSet(nvp));
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
