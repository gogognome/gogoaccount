package nl.gogognome.gogoaccount.component.configuration;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.components.document.Document;

import java.sql.SQLException;
import java.util.Currency;

public class BookkeepingDAO extends AbstractDomainClassDAO<Bookkeeping> {

    private Bookkeeping singleton;

    protected BookkeepingDAO(Document document) {
        super("account", null, document.getBookkeepingId());
    }

    public Bookkeeping getSingleton() throws SQLException {
        return get(1L);
    }

    @Override
    protected Bookkeeping getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        Bookkeeping bookkeeping = new Bookkeeping();
        bookkeeping.setCurrency(Currency.getInstance(result.getString("currency")));
        bookkeeping.setStartOfPeriod(result.getDate("start_of_period"));
        return bookkeeping;
    }

    @Override
    protected NameValuePairs getNameValuePairs(Bookkeeping bookkeeping) throws SQLException {
        return new NameValuePairs()
                .add("id", 1L)
                .add("currency", bookkeeping.getCurrency().getCurrencyCode())
                .add("start_of_period", bookkeeping.getStartOfPeriod());
    }
}
