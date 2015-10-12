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

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package nl.gogognome.gogoaccount.reportgenerators;

import java.io.File;
import java.util.Date;
import java.util.Map;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.TaskProgressListener;

/**
 * Generates a report as an ODT document based on a template file.
 *
 * @author Sander Kooijmans
 */
public class OdtReportGeneratorTask implements Task {

	private final Document document;
	private final Date date;
	private final File reportFile;
	private final File templateFile;

    private Report report;
    private Map<String, Object> model;

	public OdtReportGeneratorTask(Document document, Date date, File reportFile, File templateFile) {
		super();
		this.document = document;
		this.date = date;
		this.reportFile = reportFile;
		this.templateFile = templateFile;
	}

	@Override
	public Object execute(TaskProgressListener progressListener) throws Exception {
    	progressListener.onProgressUpdate(0);

    	report = ObjectFactory.create(BookkeepingService.class).createReport(document, date);
    	progressListener.onProgressUpdate(33);

    	createModel();
    	progressListener.onProgressUpdate(67);

    	JodReportsUtil.createDocument(templateFile, reportFile, model);
    	progressListener.onProgressUpdate(100);

		return null;
	}

	private void createModel() throws ServiceException {
		ReportToModelConverter converter = new ReportToModelConverter(document, report);
		model = converter.getModel();
	}
}
