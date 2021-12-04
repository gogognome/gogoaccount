package nl.gogognome.gogoaccount.component.backup;

import java.sql.*;
import nl.gogognome.dataaccess.dao.*;
import nl.gogognome.gogoaccount.component.document.*;

public class BackupDAO extends AbstractDAO {

	public BackupDAO(Document document) {
		super(document.getBookkeepingId());
	}

	public void createBackupOfDocument(String filename) throws SQLException {
		execute("SCRIPT SIMPLE DROP TO '" + filename + "' COMPRESSION ZIP").ignoreResult();
	}
}
