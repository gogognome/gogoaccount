package nl.gogognome.gogoaccount.gui.controllers;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.views.GenerateReportView;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.reportgenerators.OdtReportGeneratorTask;
import nl.gogognome.gogoaccount.reportgenerators.ReportTask;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

import java.awt.*;
import java.io.File;
import java.util.Date;

public class GenerateReportController {

    private final Document document;
    private final ViewFactory viewFactory;

    private Window parentWindow;

    public GenerateReportController(Document document, ViewFactory viewFactory) {
        super();
        this.document = document;
        this.viewFactory = viewFactory;
    }

    public void setParentWindow(Window parentWindow) {
        this.parentWindow = parentWindow;
    }

    public void execute() {
        HandleException.for_(parentWindow, () -> {
            GenerateReportView view = (GenerateReportView) viewFactory.createView(GenerateReportView.class);
            ViewDialog dialog = new ViewDialog(parentWindow, view);
            dialog.showDialog();

            Date date = view.getDate();
            File reportFile = view.getReportFile();
            if (date != null && reportFile != null) {
                Task task;

                switch (view.getReportType()) {
                    case PLAIN_TEXT:
                        task = new ReportTask(document, date, reportFile, view.getReportType());
                        break;

                    case ODT_DOCUMENT:
                        task = new OdtReportGeneratorTask(document, date, reportFile, view.getTemplateFile());
                        break;

                    default:
                        MessageDialog.showErrorMessage(parentWindow,
                                new Exception("Unknown report type: " + view.getReportType()),
                                "gen.internalError");
                        return;
                }

                startTask(task);
            }
        });
    }

    private void startTask(Task task) {
        String description = Factory.getInstance(TextResource.class)
            .getString("genreport.progress");
        TaskWithProgressDialog taskWithProgressDialog =
            new TaskWithProgressDialog(parentWindow, description);
        taskWithProgressDialog.execute(task);
    }
}
