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
import java.util.Map;

import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.TaskProgressListener;

/**
 * Generates invoices as an ODT document based on a template file.
 *
 * @author Sander Kooijmans
 */
public class OdtInvoiceGeneratorTask implements Task {

	private final OdtInvoiceParameters parameters;

	private final File reportFile;
	private final File templateFile;

    private Map<String, Object> model;

	public OdtInvoiceGeneratorTask(OdtInvoiceParameters parameters,
			File reportFile, File templateFile) {
		super();
		this.parameters = parameters;
		this.reportFile = reportFile;
		this.templateFile = templateFile;
	}

	@Override
	public Object execute(TaskProgressListener progressListener) throws Exception {
    	progressListener.onProgressUpdate(0);

    	createModel();
    	progressListener.onProgressUpdate(50);

    	JodReportsUtil.createDocument(templateFile, reportFile, model);
    	progressListener.onProgressUpdate(100);

		return null;
	}

	private void createModel() {
		InvoicesToModelConverter converter = new InvoicesToModelConverter(parameters);
		model = converter.getModel();
	}
}
