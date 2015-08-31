/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public Licensen
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package nl.gogognome.test.tools;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.gui.Start;
import nl.gogognome.gogoaccount.reportgenerators.OdtReportGeneratorTask;
import nl.gogognome.gogoaccount.services.XMLFileReader;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.util.DateUtil;

/**
 * Tests generation of an ODT report.
 *
 * @author Sander Kooijmans
 */
public class ODTReportGenerator {
	public static void main(String[] args) throws Exception {
		new Start().initFactory(new Locale("nl"));

		File bookkeepingFile = new File(args[0]);
		File templateFile = new File(args[1]);
		File reportFile = new File(args[2]);

		XMLFileReader reader = new XMLFileReader(bookkeepingFile);
		Document document = reader.createDatabaseFromFile();

		Date date = DateUtil.addYears(document.getStartOfPeriod(), 1);

		OdtReportGeneratorTask task = new OdtReportGeneratorTask(document, date, reportFile, templateFile);
		task.execute(new TaskProgressListener() {
			@Override
			public void onProgressUpdate(int percentageCompleted) {
			}
		});

		System.out.println("Done!");
	}

}
