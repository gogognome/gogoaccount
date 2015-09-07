package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.NoRecordFoundException;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

public class InvoiceDAO extends AbstractDomainClassDAO<Invoice> {

    private final Document document;

    public InvoiceDAO(Document document) {
        super("invoice", "domain_class_sequence", document.getBookkeepingId());
        this.document = document;
    }

    public void createDetails(String invoiceId, List<String> descriptions, List<Amount> amounts) throws SQLException {
        if (descriptions.size() != amounts.size()) {
            throw new IllegalArgumentException("descriptions and amounts must have same size");
        }

        AmountFormat amountFormat = new AmountFormat(document.getLocale());
        for (int i=0; i<descriptions.size(); i++) {
            insert("invoice_details", new NameValuePairs()
                    .add("invoice_id", invoiceId)
                    .add("description", descriptions.get(i))
                    .add("amount", amountFormat.formatAmount(amounts.get(i)))
            );
        }
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
            throw new SQLException("Could not parse amount");
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
