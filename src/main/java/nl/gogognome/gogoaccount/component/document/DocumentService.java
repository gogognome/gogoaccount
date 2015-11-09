package nl.gogognome.gogoaccount.component.document;

import nl.gogognome.dataaccess.DataAccessException;
import nl.gogognome.dataaccess.migrations.DatabaseMigratorDAO;
import nl.gogognome.dataaccess.migrations.Migration;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import org.h2.jdbcx.JdbcDataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static java.util.stream.Collectors.*;

public class DocumentService {

    public Document createNewDocument(String description) throws ServiceException {
        String jdbcUrl = "jdbc:h2:mem:bookkeeping-" + UUID.randomUUID();
        return createDocument(jdbcUrl, description, Long.MAX_VALUE);
    }

    public Document createNewDocument(String fileName, String description) throws ServiceException {
        String jdbcUrl = "jdbc:h2:file:" + fileName;
        return createDocument(jdbcUrl, description, Long.MAX_VALUE);
    }

    public Document createNewDocument(String fileName, String description, long maxMigrationNr) throws ServiceException {
        String jdbcUrl = "jdbc:h2:file:" + fileName;
        return createDocument(jdbcUrl, description, maxMigrationNr);
    }

    private Document createDocument(String jdbcUrl, String description, long maxMigrationNr) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Document document = new Document();

            registerDataSource(document, jdbcUrl);
            applyDatabaseMigrations(document, maxMigrationNr);

            ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            bookkeeping.setDescription(description);
            bookkeeping.setStartOfPeriod(getFirstDayOfYear(new Date()));
            ObjectFactory.create(ConfigurationService.class).updateBookkeeping(document, bookkeeping);

            return document;
        });
    }

    private void registerDataSource(Document document, String jdbcUrl) throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(jdbcUrl);
        CompositeDatasourceTransaction.registerDataSource(document.getBookkeepingId(), dataSource);
        document.connectionToKeepInMemoryDatabaseAlive = dataSource.getConnection();
    }

    private void applyDatabaseMigrations(Document document, long maxMigrationNr) throws IOException, DataAccessException, SQLException {
        DatabaseMigratorDAO databaseMigratorDAO = new DatabaseMigratorDAO(document.getBookkeepingId());
        List<Migration> migrations = databaseMigratorDAO.loadMigrationsFromResource("/database/_migrations.txt");
        List<Migration> migrationsToBeApplied = migrations.stream().filter(m -> m.getId() <= maxMigrationNr).collect(toList());
        databaseMigratorDAO.applyMigrations(migrationsToBeApplied);
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

    public void applyDatabaseMigrations(Document document) throws ServiceException {
        ServiceTransaction.withoutResult(() -> applyDatabaseMigrations(document, Long.MAX_VALUE));
    }
}
