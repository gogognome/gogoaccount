package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.util.AmountInDatabase;

import java.sql.SQLException;
import java.util.Set;

class InvoiceDAO extends AbstractDomainClassDAO<Invoice> {

    InvoiceDAO(Document document) {
        super("invoice", null, document.getBookkeepingId());
    }

    Set<String> findExistingInvoiceIds() throws SQLException {
        return execute("SELECT id FROM " + tableName).toSet(r -> r.getString(1));
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
