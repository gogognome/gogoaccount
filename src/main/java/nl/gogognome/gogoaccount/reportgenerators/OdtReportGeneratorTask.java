package nl.gogognome.gogoaccount.reportgenerators;

import java.io.File;
import java.util.Date;
import java.util.Map;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.TaskProgressListener;

/**
 * Generates a report as an ODT document based on a template file.
 */
public class OdtReportGeneratorTask implements Task {

    private final Document document;
    private final BookkeepingService bookkeepingService;
    private final ReportToModelConverter reportToModelConverter;
    private final Date date;
    private final File reportFile;
    private final File templateFile;

    private Report report;
    private Map<String, Object> model;

    public OdtReportGeneratorTask(Document document, BookkeepingService bookkeepingService, ReportToModelConverter reportToModelConverter, Date date, File reportFile, File templateFile) {
        super();
        this.document = document;
        this.bookkeepingService = bookkeepingService;
        this.reportToModelConverter = reportToModelConverter;
        this.date = date;
        this.reportFile = reportFile;
        this.templateFile = templateFile;
    }

    @Override
    public Object execute(TaskProgressListener progressListener) throws Exception {
        progressListener.onProgressUpdate(0);

        report = bookkeepingService.createReport(document, date);
        progressListener.onProgressUpdate(33);

        model = reportToModelConverter.buildModel(report);
        progressListener.onProgressUpdate(67);

        JodReportsUtil.createDocument(templateFile, reportFile, model);
        progressListener.onProgressUpdate(100);

        return null;
    }

}
