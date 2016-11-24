package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.email.EmailService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoicePreviewTemplate;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;

import static nl.gogognome.gogoaccount.component.invoice.InvoiceSending.Type.EMAIL;

public class EmailInvoicesView extends SendInvoicesView {

    private final Document document;
    private final EmailService emailService;
    private final InvoiceService invoiceService;

    public EmailInvoicesView(Document document, EmailService emailService, InvoiceService invoiceService,
                             InvoicePreviewTemplate invoicePreviewTemplate, SettingsService settingsService) {
        super(document, invoicePreviewTemplate, settingsService);
        this.document = document;
        this.emailService = emailService;
        this.invoiceService = invoiceService;
    }

    @Override
    protected String getButtonResourceId() {
        return "EmailInvoicesView.sendEmail";
    }

    @Override
    protected boolean send() {
        int choice = MessageDialog.showYesNoQuestion(this, "gen.titleWarning", "SendInvoicesView.areYouSureToSendEmails");
        if (choice != MessageDialog.YES_OPTION) {
            return false;
        }

        TaskWithProgressDialog progressDialog = new TaskWithProgressDialog(this, textResource.getString("SendInvoicesView.sendingEmails"));
        progressDialog.execute(taskProgressListener -> {
            int progress = 0;
            for (Invoice invoice : invoicesToSend) {
                progress += 100 / invoicesToSend.size();
                taskProgressListener.onProgressUpdate(progress);
                String xml = fillInParametersInTemplate(templateModel.getString(), invoice);
                emailService.sendEmail(document, invoiceIdToParty.get(invoice.getId()).getEmailAddress(), invoice.getDescription(), xml, "utf-8", "html");
                invoiceService.createInvoiceSending(document, invoice, EMAIL);
            }
            taskProgressListener.onProgressUpdate(100);
            return null;
        });
        return true;
    }


}
