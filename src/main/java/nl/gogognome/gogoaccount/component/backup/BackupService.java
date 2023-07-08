package nl.gogognome.gogoaccount.component.backup;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.io.*;
import java.nio.file.*;
import java.text.*;
import nl.gogognome.gogoaccount.component.document.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.lib.util.*;

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
		File originalPath = document.getDatabaseFile();
		String backupFilename = getBackupFile(originalPath, ".sql.zip").getAbsolutePath();
		ServiceTransaction.withoutResult(() -> new BackupDAO(document).createBackupOfDocument(backupFilename));
	}

	private File getBackupFile(File originalPath, String extension) {
		String formattedDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(DateUtil.createNow());
		File directory = originalPath.getParentFile();
		String backupFilename = ("backup-" + formattedDate + "-" + originalPath.getName() + extension);
		return new File(directory, backupFilename);
	}
}
