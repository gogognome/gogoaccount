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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import net.sf.jooreports.templates.DocumentTemplate;
import net.sf.jooreports.templates.DocumentTemplateException;
import net.sf.jooreports.templates.UnzippedDocumentTemplate;
import net.sf.jooreports.templates.ZippedDocumentTemplate;
import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import cf.engine.Database;

/**
 * Generates a report as an ODT document based on a template file.
 *  
 * @author Sander Kooijmans
 */
public class OdtGeneratorTask implements Task {

	private final Database database;
	private final Date date;
	private final File reportFile;
	private final File templateFile;
	
    private Report report;
    private Map<String, Object> model;

    private TextResource textResource = Factory.getInstance(TextResource.class);
    private AmountFormat amountFormat = Factory.getInstance(AmountFormat.class);
    
	public OdtGeneratorTask(Database database, Date date, File reportFile, File templateFile) {
		super();
		this.database = database;
		this.date = date;
		this.reportFile = reportFile;
		this.templateFile = templateFile;
	}

	@Override
	public Object execute(TaskProgressListener progressListener) throws Exception {
    	progressListener.onProgressUpdate(0);
    	
    	report = BookkeepingService.createReport(database, date);
    	progressListener.onProgressUpdate(33);
    	
    	createModel();
    	progressListener.onProgressUpdate(67);
    	
    	createDocument();
    	progressListener.onProgressUpdate(100);
    	
		return null;
	}

	private void createModel() {
		ReportToModelConverter converter = new ReportToModelConverter(database, report);
		model = converter.getModel();
	}

	private void createDocument() throws FileNotFoundException, IOException, DocumentTemplateException {
		DocumentTemplate template = null;
		if (templateFile.isDirectory()) {
			template = new UnzippedDocumentTemplate(templateFile);
		} else {
			template = new ZippedDocumentTemplate(templateFile);
		}

		template.createDocument(model, new FileOutputStream(reportFile));
	}
}
