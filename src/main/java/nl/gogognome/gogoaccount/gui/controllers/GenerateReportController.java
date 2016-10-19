package nl.gogognome.gogoaccount.gui.controllers;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.views.GenerateReportView;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.reportgenerators.OdtReportGeneratorTask;
import nl.gogognome.gogoaccount.reportgenerators.ReportTask;
import nl.gogognome.gogoaccount.reportgenerators.ReportToModelConverter;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.swing.views.ViewOwner;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

import java.io.File;
import java.util.Date;

public class GenerateReportController {

    private final Document document;
    private final AmountFormat amountFormat;
    private final TextResource textResource;
    private final BookkeepingService bookkeepingService;
    private final ConfigurationService configurationService;
    private final InvoiceService invoiceService;
    private final LedgerService ledgerService;
    private final PartyService partyService;
    private final ReportToModelConverter reportToModelConverter;
    private final ViewFactory viewFactory;

    private ViewOwner viewOwner;

    public GenerateReportController(Document document, AmountFormat amountFormat, TextResource textResource,
                                    BookkeepingService bookkeepingService, ConfigurationService configurationService,
                                    InvoiceService invoiceService, LedgerService ledgerService, PartyService partyService,
                                    ReportToModelConverter reportToModelConverter, ViewFactory viewFactory) {
        this.document = document;
        this.amountFormat = amountFormat;
        this.textResource = textResource;
        this.bookkeepingService = bookkeepingService;
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.ledgerService = ledgerService;
        this.partyService = partyService;
        this.reportToModelConverter = reportToModelConverter;
        this.viewFactory = viewFactory;
    }

    public void setViewOwner(ViewOwner viewOwner) {
        this.viewOwner = viewOwner;
    }

    public void execute() {
        HandleException.for_(viewOwner.getWindow(), () -> {
            GenerateReportView view = (GenerateReportView) viewFactory.createView(GenerateReportView.class);
            ViewDialog dialog = new ViewDialog(viewOwner.getWindow(), view);
            dialog.showDialog();

            Date date = view.getDate();
            File reportFile = view.getReportFile();
            if (date != null && reportFile != null) {
                Task task;

                switch (view.getReportType()) {
                    case PLAIN_TEXT:
                        task = new ReportTask(document, amountFormat, textResource, bookkeepingService, configurationService, invoiceService, ledgerService, partyService, date, reportFile, view.getReportType());
                        break;

                    case ODT_DOCUMENT:
                        task = new OdtReportGeneratorTask(document, bookkeepingService, reportToModelConverter, date, reportFile, view.getTemplateFile());
                        break;

                    default:
                        MessageDialog.showErrorMessage(viewOwner.getWindow(),
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
            new TaskWithProgressDialog(viewOwner, description);
        taskWithProgressDialog.execute(task);
    }
}
