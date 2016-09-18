package nl.gogognome.gogoaccount.component.document;

import nl.gogognome.dataaccess.DataAccessException;
import nl.gogognome.dataaccess.migrations.DatabaseMigratorDAO;
import nl.gogognome.dataaccess.migrations.Migration;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import org.h2.jdbcx.JdbcDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static java.util.stream.Collectors.*;

public class DocumentService {

    private ConfigurationService configurationService;

    public DocumentService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public Document openDocument(String fileName) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            String fileNameWithouExtension = fileName;
            if (fileName.endsWith(".h2.db")) {
                fileNameWithouExtension = fileName.substring(0, fileName.length() - 6);
            }
            String jdbcUrl = "jdbc:h2:file:" + fileNameWithouExtension;
            Document document = new Document();

            registerDataSource(document, jdbcUrl);
            applyDatabaseMigrations(document, Long.MAX_VALUE);

            return document;
        });
    }

    public Document createNewDocumentInMemory(String description) throws ServiceException {
        String jdbcUrl = "jdbc:h2:mem:bookkeeping-" + UUID.randomUUID();
        return createDocument(jdbcUrl, description);
    }

    public Document createNewDocument(File file, String description) throws ServiceException {
        String jdbcUrl = "jdbc:h2:file:" + file.getAbsolutePath();
        return createDocument(jdbcUrl, description);
    }

    private Document createDocument(String jdbcUrl, String description) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Document document = new Document();

            registerDataSource(document, jdbcUrl);
            applyDatabaseMigrations(document);

            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            bookkeeping.setDescription(description);
            bookkeeping.setStartOfPeriod(getFirstDayOfYear(new Date()));
            configurationService.updateBookkeeping(document, bookkeeping);

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
