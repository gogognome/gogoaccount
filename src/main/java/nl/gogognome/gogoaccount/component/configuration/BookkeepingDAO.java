package nl.gogognome.gogoaccount.component.configuration;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.components.document.Document;

import java.sql.SQLException;
import java.util.Currency;

public class BookkeepingDAO extends AbstractDomainClassDAO<Bookkeeping> {

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
        return bookkeeping;
    }

    @Override
    protected NameValuePairs getNameValuePairs(Bookkeeping bookkeeping) throws SQLException {
        return new NameValuePairs()
                .add("id", SINGLETON_ID)
                .add("description", bookkeeping.getDescription())
                .add("currency", bookkeeping.getCurrency().getCurrencyCode())
                .add("start_of_period", bookkeeping.getStartOfPeriod());
    }
}
