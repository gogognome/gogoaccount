package nl.gogognome.gogoaccount.gui.controllers;

import java.io.*;
import java.util.*;
import nl.gogognome.gogoaccount.component.configuration.*;
import nl.gogognome.gogoaccount.component.document.*;
import nl.gogognome.gogoaccount.component.invoice.*;
import nl.gogognome.gogoaccount.component.ledger.*;
import nl.gogognome.gogoaccount.component.party.*;
import nl.gogognome.gogoaccount.gui.*;
import nl.gogognome.gogoaccount.gui.views.*;
import nl.gogognome.gogoaccount.reportgenerators.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.lib.swing.dialogs.*;
import nl.gogognome.lib.swing.views.*;
import nl.gogognome.lib.task.*;
import nl.gogognome.lib.task.ui.*;
import nl.gogognome.lib.text.*;

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
    private HandleException handleException;

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
        handleException = new HandleException(new MessageDialog(textResource, viewOwner.getWindow()));
    }

    public void execute() {
        handleException.of(() -> {
            GenerateReportView view = (GenerateReportView) viewFactory.createView(GenerateReportView.class);
            ViewDialog dialog = new ViewDialog(viewOwner.getWindow(), view);
            dialog.showDialog();

            Date date = view.getDate();
            File reportFile = view.getReportFile();
            if (date != null && reportFile != null) {
                Task task = switch (view.getReportType()) {
                    case PLAIN_TEXT ->
                            new ReportTask(document, amountFormat, textResource, bookkeepingService, configurationService, invoiceService, ledgerService, partyService, date, reportFile, view.getReportType());
                    case ODT_DOCUMENT ->
                            new OdtReportGeneratorTask(document, bookkeepingService, reportToModelConverter, date, reportFile, view.getTemplateFile());
                };

                startTask(task, reportFile);
            }
        });
    }

    private void startTask(Task task, File reportFile) {
        TaskWithProgressDialog taskWithProgressDialog = new TaskWithProgressDialog(viewOwner, textResource, "genreport.progress", reportFile.getAbsolutePath());
        taskWithProgressDialog.execute(task);
    }
}
