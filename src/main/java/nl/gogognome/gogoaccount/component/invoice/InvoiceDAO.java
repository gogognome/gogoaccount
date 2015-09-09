package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.lib.text.AmountFormat;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Set;

class InvoiceDAO extends AbstractDomainClassDAO<Invoice> {

    private final Document document;

    public InvoiceDAO(Document document) {
        super("invoice", null, document.getBookkeepingId());
        this.document = document;
    }

    public Set<String> findExistingInvoiceIds() throws SQLException {
        return execute("SELECT id FROM " + tableName).toSet(r -> r.getString(1));
    }

    @Override
    protected Invoice getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        Invoice invoice = new Invoice(result.getString("id"));
        invoice.setIssueDate(result.getDate("issue_date"));
        try {
            AmountFormat amountFormat = new AmountFormat(document.getLocale());
            invoice.setAmountToBePaid(amountFormat.parse(result.getString("amount_to_be_paid")));
        } catch (ParseException e) {
            throw new SQLException("Could not parse amount " + result.getString("amount_to_be_paid"));
        }
        invoice.setConcerningPartyId(result.getString("concerning_party_id"));
        invoice.setPayingPartyId(result.getString("paying_party_id"));
        return invoice;
    }

    @Override
    protected NameValuePairs getNameValuePairs(Invoice invoice) throws SQLException {
        return new NameValuePairs()
                .add("id", invoice.getId())
                .add("issue_date", invoice.getIssueDate())
                .add("amount_to_be_paid", new AmountFormat(document.getLocale()).formatAmount(invoice.getAmountToBePaid()))
                .add("concerning_party_id", invoice.getConcerningPartyId())
                .add("paying_party_id", invoice.getPayingPartyId());
    }
}
