package nl.gogognome.gogoaccount.component.document;

import nl.gogognome.dataaccess.migrations.DatabaseMigratorDAO;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import org.h2.jdbcx.JdbcDataSource;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DocumentService {

    public Document createNewDatabase(String description) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Document document = new Document();

            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:bookkeeping-" + document.getBookkeepingId());
            CompositeDatasourceTransaction.registerDataSource(document.getBookkeepingId(), dataSource);
            document.connectionToKeepInMemoryDatabaseAlive = dataSource.getConnection();

            new DatabaseMigratorDAO(document.getBookkeepingId()).applyMigrationsFromResource("/database/_migrations.txt");

            ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            bookkeeping.setDescription(description);
            bookkeeping.setStartOfPeriod(getFirstDayOfYear(new Date()));
            ObjectFactory.create(ConfigurationService.class).updateBookkeeping(document, bookkeeping);
            return document;
        });
    }

    private static Date getFirstDayOfYear(Date date) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
