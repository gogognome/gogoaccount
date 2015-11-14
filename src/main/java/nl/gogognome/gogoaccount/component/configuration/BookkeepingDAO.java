package nl.gogognome.gogoaccount.component.configuration;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;

import java.sql.SQLException;
import java.util.Currency;

class BookkeepingDAO extends AbstractDomainClassDAO<Bookkeeping> {

    private final static long SINGLETON_ID = 1;

    protected BookkeepingDAO(Document document) {
        super("bookkeeping", null, document.getBookkeepingId());
    }

    public Bookkeeping getSingleton() throws SQLException {
        return get(SINGLETON_ID);
    }

    @Override
    protected Bookkeeping getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        Bookkeeping bookkeeping = new Bookkeeping();
        bookkeeping.setDescription(result.getString("description"));
        bookkeeping.setCurrency(Currency.getInstance(result.getString("currency")));
        bookkeeping.setStartOfPeriod(result.getDate("start_of_period"));
        bookkeeping.setOrganizationName(result.getString("organization_name"));
        bookkeeping.setOrganizationAddress(result.getString("organization_address"));
        bookkeeping.setOrganizationZipCode(result.getString("organization_zip_code"));
        bookkeeping.setOrganizationCity(result.getString("organization_city"));
        bookkeeping.setIban(result.getString("iban"));
        bookkeeping.setBic(result.getString("bic"));
        bookkeeping.setAutomaticCollectionContractNumber(result.getString("automatic_collection_contract_number"));
        return bookkeeping;
    }

    @Override
    protected NameValuePairs getNameValuePairs(Bookkeeping bookkeeping) throws SQLException {
        return new NameValuePairs()
                .add("id", SINGLETON_ID)
                .add("description", bookkeeping.getDescription())
                .add("currency", bookkeeping.getCurrency().getCurrencyCode())
                .add("start_of_period", bookkeeping.getStartOfPeriod())
                .add("organization_name", bookkeeping.getOrganizationName())
                .add("organization_address", bookkeeping.getOrganizationAddress())
                .add("organization_zip_code", bookkeeping.getOrganizationZipCode())
                .add("organization_city", bookkeeping.getOrganizationCity())
                .add("iban", bookkeeping.getIban())
                .add("bic", bookkeeping.getBic())
                .add("automatic_collection_contract_number", bookkeeping.getAutomaticCollectionContractNumber());
    }
}
