package nl.gogognome.gogoaccount.component.backup;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.io.*;
import java.nio.file.*;
import java.text.*;
import nl.gogognome.gogoaccount.component.document.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.lib.util.*;

/**
 * This service creates backup of the database file. Backups are stored in a subdirectory of the directory containing
 * the database file. The subdirectory is named "backup-[database file without extension]" and it will be created
 * if it does not exist yet.
 */
public class BackupService {

	public void createBackupOfDatabaseFile(File file) throws ServiceException {
		File backupFile = getBackupFile(file, ".bak");
		try {
			Files.copy(file.toPath(), backupFile.toPath(), REPLACE_EXISTING);
		} catch (IOException e) {
			throw new ServiceException("A problem occurred while creating a backup of " + file.getAbsolutePath(), e);
		}
	}

	public void createBackupOfSqlStatements(Document document) throws ServiceException {
		File backupFile = getBackupFile(document.getDatabaseFile(), ".sql.zip");
		ServiceTransaction.withoutResult(() -> new BackupDAO(document).createBackupOfDocument(backupFile));
	}

	private File getBackupFile(File originalPath, String extension) {
		String backupFilename = getBackupFilename(originalPath, extension);
		File directory = getBackupDirectory(originalPath);
		return new File(directory, backupFilename);
	}

	private File getBackupDirectory(File originalPath) {
		File directory = new File(originalPath.getParentFile(), "backup");
		if (!directory.exists()) {
			directory.mkdir();
		}
		return directory;
	}

	private String getBackupFilename(File originalPath, String extension) {
		String formattedDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(DateUtil.createNow());
		return ("backup-" + formattedDate + "-" + originalPath.getName() + extension);
	}
}
