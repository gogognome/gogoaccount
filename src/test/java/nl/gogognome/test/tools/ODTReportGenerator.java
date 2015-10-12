package nl.gogognome.test.tools;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.gui.Start;
import nl.gogognome.gogoaccount.reportgenerators.OdtReportGeneratorTask;
import nl.gogognome.gogoaccount.services.XMLFileReader;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.util.DateUtil;

/**
 * Tests generation of an ODT report.
 */
public class ODTReportGenerator {
	public static void main(String[] args) throws Exception {
		new Start().initFactory(new Locale("nl"));

		File bookkeepingFile = new File(args[0]);
		File templateFile = new File(args[1]);
		File reportFile = new File(args[2]);

		XMLFileReader reader = new XMLFileReader(bookkeepingFile);
		Document document = reader.createDatabaseFromFile();
		Bookkeeping bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
		Date date = DateUtil.addYears(bookkeeping.getStartOfPeriod(), 1);

		OdtReportGeneratorTask task = new OdtReportGeneratorTask(document, date, reportFile, templateFile);
		task.execute(new TaskProgressListener() {
			@Override
			public void onProgressUpdate(int percentageCompleted) {
			}
		});

		System.out.println("Done!");
	}

}
