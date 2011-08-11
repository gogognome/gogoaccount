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
package nl.gogognome.gogoaccount.controllers;

import java.awt.Window;
import java.io.File;
import java.util.Date;

import nl.gogognome.gogoaccount.reportgenerators.OdtReportGeneratorTask;
import nl.gogognome.gogoaccount.reportgenerators.ReportTask;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import cf.engine.Database;
import cf.ui.views.GenerateReportView;

/**
 * This controller lets the user generate a report.
 *
 * @author Sander Kooijmans
 */
public class GenerateReportController {

	private Database database;
	private Window parentWindow;

	public GenerateReportController(Database database, Window parentWindow) {
		super();
		this.database = database;
		this.parentWindow = parentWindow;
	}

	public void execute() {
		GenerateReportView view = new GenerateReportView(database);
		ViewDialog dialog = new ViewDialog(parentWindow, view);
		dialog.showDialog();

		Date date = view.getDate();
		File reportFile = view.getReportFile();
		if (date != null && reportFile != null) {
	        Task task;

			switch (view.getReportType()) {
			case PLAIN_TEXT:
			    task = new ReportTask(database, date, reportFile, view.getReportType());
				break;

			case ODT_DOCUMENT:
				task = new OdtReportGeneratorTask(database, date, reportFile, view.getTemplateFile());
				break;

			default:
				MessageDialog.showErrorMessage(parentWindow,
						new Exception("Unknown report type: " + view.getReportType()),
						"gen.internalError");
				return;
			}

			startTask(task);
		}
	}

	private void startTask(Task task) {
        String description = Factory.getInstance(TextResource.class)
        	.getString("genreport.progress");
        TaskWithProgressDialog taskWithProgressDialog =
        	new TaskWithProgressDialog(parentWindow, description);
		taskWithProgressDialog.execute(task);
	}
}
