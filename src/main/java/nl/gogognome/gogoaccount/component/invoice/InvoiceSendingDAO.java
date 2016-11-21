package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;

import java.sql.SQLException;

class InvoiceSendingDAO extends AbstractDomainClassDAO<InvoiceSending> {

    public InvoiceSendingDAO(Document document) {
        super("invoice_sending", "domain_class_sequence", document.getBookkeepingId());
    }

    @Override
    protected InvoiceSending getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        InvoiceSending invoiceSending = new InvoiceSending(result.getLong("id"));
        invoiceSending.setInvoiceId(result.getString("invoice_id"));
        invoiceSending.setDate(result.getDate("date"));
        invoiceSending.setType(result.getEnum(InvoiceSending.Type.class, "type"));
        return invoiceSending;
    }

    @Override
    protected NameValuePairs getNameValuePairs(InvoiceSending invoiceSending) throws SQLException {
        return new NameValuePairs()
                .add("invoice_id", invoiceSending.getInvoiceId())
                .add("date", invoiceSending.getDate())
                .add("type", invoiceSending.getType());
    }
}
