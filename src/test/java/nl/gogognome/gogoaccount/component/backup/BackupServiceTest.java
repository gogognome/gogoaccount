package nl.gogognome.gogoaccount.component.backup;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

class BackupServiceTest {

	private final BackupService backupService = new BackupService();

	@TempDir
	File directory;

	@Test
	public void testCreatingBackupDirectory() throws Exception {
		File originalFile = createFileWithContents("file.txt");

		backupService.createBackupOfDatabaseFile(originalFile);

		File backupDirectory = new File(directory, "backup");
		assertThat(backupDirectory)
				.exists()
				.isDirectoryContaining("regex:.*[.]bak");

		assertThat(getSingleFileFrom(backupDirectory))
				.exists()
				.hasExtension("bak")
				.hasSameBinaryContentAs(originalFile);
	}

	private File createFileWithContents(String filename) throws IOException {
		File originalFile = new File(directory, filename);
		Files.writeString(originalFile.toPath(), "DIt is een test");
		return originalFile;
	}

	private File getSingleFileFrom(File backupDirectory) throws Exception {
		return Stream.of(backupDirectory.listFiles())
				.findAny()
				.orElseThrow(() -> new Exception("No backup file found in the backup directory."));
	}
}