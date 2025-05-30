package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.reportgenerators.InvoicesToModelConverter;
import nl.gogognome.gogoaccount.reportgenerators.OdtInvoiceGeneratorTask;
import nl.gogognome.gogoaccount.reportgenerators.OdtInvoiceParameters;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import nl.gogognome.lib.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Date;

public class InvoiceToOdtView extends View {

    private final Document document;
    private final ViewFactory viewFactory;
    private final InvoicesToModelConverter invoicesToModelConverter;
    private final MessageDialog messageDialog;
    private final HandleException handleException;

    private FileModel templateFileModel = new FileModel();
    private FileModel odtFileModel = new FileModel();
    private DateModel dateModel = new DateModel(new Date());
    private StringModel concerningModel = new StringModel();
    private StringModel ourReferenceModel = new StringModel();
    private DateModel dueDateModel = new DateModel();

    public InvoiceToOdtView(Document document, ViewFactory viewFactory, InvoicesToModelConverter invoicesToModelConverter) {
        this.document = document;
        this.viewFactory = viewFactory;
        this.invoicesToModelConverter = invoicesToModelConverter;
        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);
    }

    /**
     * Checks whether the user has entered all necessary information.
     * If so, then the ODT file will be generated; if not, then an
     * error message is shown to the user.
     */
    protected void generateInvoices() {
        handleException.of(() -> {
            Date date = dateModel.getDate();
            if (date == null) {
                messageDialog.showWarningMessage("gen.invalidDate");
                return;
            }

            Date dueDate = dueDateModel.getDate();
            if (dueDate == null) {
                messageDialog.showWarningMessage("gen.invalidDate");
                return;
            }

            if (odtFileModel.getFile() == null) {
                messageDialog.showWarningMessage("invoiceToOdtView.noOdtFileNameSpecified");
                return;
            }

            if (templateFileModel.getFile() == null) {
                messageDialog.showWarningMessage("invoiceToOdtView.noTemplateFileNameSpecified");
                return;
            }

            // Let the user select the invoices that should be added to the ODT file.
            InvoiceEditAndSelectionView invoicesView = (InvoiceEditAndSelectionView) viewFactory.createView(InvoiceEditAndSelectionView.class);
            invoicesView.enableMultiSelect();
            ViewDialog dialog = new ViewDialog(getViewOwner().getWindow(), invoicesView);
            dialog.showDialog();
            if (invoicesView.getSelectedInvoices() != null) {
                OdtInvoiceParameters parameters = new OdtInvoiceParameters(document, invoicesView.getSelectedInvoices());
                parameters.setConcerning(concerningModel.getString());
                parameters.setDate(date);
                parameters.setDueDate(dueDate);
                parameters.setOurReference(ourReferenceModel.getString());
                OdtInvoiceGeneratorTask task = new OdtInvoiceGeneratorTask(invoicesToModelConverter, parameters,
                        odtFileModel.getFile(), templateFileModel.getFile());
                TaskWithProgressDialog progressDialog = new TaskWithProgressDialog(this, textResource, "invoiceToOdtView.progressDialogTitle");
                progressDialog.execute(task);
            }
        });
    }

    @Override
    public String getTitle() {
        return textResource.getString("invoiceToOdtView.title");
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onInit() {
        InputFieldsColumn vep = new InputFieldsColumn();
        addCloseable(vep);

        vep.addField("invoiceToOdtView.templateFilename", templateFileModel);
        vep.addField("invoiceToOdtView.odtFileName", odtFileModel);
        vep.addField("invoiceToOdtView.date", dateModel);
        vep.addField("invoiceToOdtView.concerning", concerningModel);
        vep.addField("invoiceToOdtView.ourReference", ourReferenceModel);
        dueDateModel.setDate(DateUtil.addMonths(new Date(), 1), null);
        vep.addField("invoiceToOdtView.dueDate", dueDateModel);

        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
        buttonPanel.addButton("invoiceToOdtView.generate", new GenerateAction());

        // Add panels to view
        setLayout(new BorderLayout());
        add(vep, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setBorder(widgetFactory.createTitleBorderWithMarginAndPadding("invoiceToOdtView.title"));
    }

	private final class GenerateAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    generateInvoices();
		}
	}
}
