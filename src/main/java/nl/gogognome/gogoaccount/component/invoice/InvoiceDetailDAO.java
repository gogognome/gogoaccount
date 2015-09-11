package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

class InvoiceDetailDAO extends AbstractDomainClassDAO<InvoiceDetail> {

    private final Document document;

    public InvoiceDetailDAO(Document document) {
        super("invoice_detail", "domain_class_sequence", document.getBookkeepingId());
        this.document = document;
    }

    public void createDetails(String invoiceId, List<String> descriptions, List<Amount> amounts) throws SQLException {
        if (descriptions.size() != amounts.size()) {
            throw new IllegalArgumentException("descriptions and amounts must have same size");
        }

        for (int i=0; i<descriptions.size(); i++) {
            InvoiceDetail invoiceDetail = new InvoiceDetail();
            invoiceDetail.setInvoiceId(invoiceId);
            invoiceDetail.setDescription(descriptions.get(i));
            invoiceDetail.setAmount(amounts.get(i));
            create(invoiceDetail);
        }
    }

    public void updateDetails(String invoiceId, List<String> newDescriptions, List<Amount> newAmounts) throws SQLException {
        deleteWhere(new NameValuePairs().add("invoice_id", invoiceId));
        createDetails(invoiceId, newDescriptions, newAmounts);
    }

    public List<InvoiceDetail> findForInvoice(String invoiceId) throws SQLException {
        return findAll(new NameValuePairs().add("invoice_id", invoiceId));
    }

    @Override
    protected InvoiceDetail getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        InvoiceDetail invoiceDetail = new InvoiceDetail(result.getLong("id"));
        invoiceDetail.setInvoiceId(result.getString("invoice_id"));
        invoiceDetail.setDescription(result.getString("description"));
        invoiceDetail.setAmount(document.toAmount(result.getString("amount")));
        return invoiceDetail;
    }

    @Override
    protected NameValuePairs getNameValuePairs(InvoiceDetail invoiceDetail) throws SQLException {
        return new NameValuePairs()
                .add("id", invoiceDetail.getId())
                .add("invoice_id", invoiceDetail.getInvoiceId())
                .add("description", invoiceDetail.getDescription())
                .add("amount", document.toString(invoiceDetail.getAmount()));
    }
}
