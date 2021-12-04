package nl.gogognome.gogoaccount.component.backup;

import java.io.*;
import java.text.*;
import nl.gogognome.gogoaccount.component.document.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.lib.util.*;

public class BackupService {

	public void createBackup(Document document) throws ServiceException {
		String backupFilename = determinePathToStoreBackup(document);
		ServiceTransaction.withoutResult(() -> new BackupDAO(document).createBackupOfDocument(backupFilename));
	}

	private String determinePathToStoreBackup(Document document) {
		String formattedDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(DateUtil.createNow());

		File originalPath = new File(document.getFilePath());
		File directory = originalPath.getParentFile();
		String backupFilename = ("backup-" + originalPath.getName() + "-" + formattedDate + ".zip")
				.replaceAll(".h2.db", "");
		return new File(directory, backupFilename).getAbsolutePath();
	}

}
