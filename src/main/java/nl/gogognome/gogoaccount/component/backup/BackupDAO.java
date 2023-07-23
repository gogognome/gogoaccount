package nl.gogognome.gogoaccount.component.backup;

import java.io.*;
import java.sql.*;
import nl.gogognome.dataaccess.dao.*;
import nl.gogognome.gogoaccount.component.document.*;

public class BackupDAO extends AbstractDAO {

	public BackupDAO(Document document) {
		super(document.getBookkeepingId());
	}

	/**
	 * Creates a compressed SQL-script with statements to create a database in the same state as the current database.
	 * @param backupFile the file (typically with extension ".sql.zip") that is created.
	 * @throws SQLException if a problem occurs generating the SQL statements and writing them to the file.
	 */
	public void createBackupOfDocument(File backupFile) throws SQLException {
		execute("SCRIPT SIMPLE DROP TO '" + backupFile.getAbsolutePath() + "' COMPRESSION ZIP")
				.ignoreResult();
	}
}
