package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import nl.gogognome.lib.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Date;
import java.util.List;

public class GenerateAutomaticCollectionFileView extends View {

    private final Document document;

    private FileModel sepaFileModel = new FileModel();
    private DateModel collectionDateModel = new DateModel(new Date());
    private StringModel concerningModel = new StringModel();

    public GenerateAutomaticCollectionFileView(Document document) {
        this.document = document;
    }

    @Override
    public String getTitle() {
        return textResource.getString("generateAutomaticCollectionFileView.title");
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onInit() {
        InputFieldsColumn vep = new InputFieldsColumn();
        addCloseable(vep);

        vep.addField("generateAutomaticCollectionFileView.sepaFileName", sepaFileModel);
        vep.addField("generateAutomaticCollectionFileView.collectionDate", collectionDateModel);
        vep.addField("generateAutomaticCollectionFileView.concerning", concerningModel);
        collectionDateModel.setDate(DateUtil.addDays(new Date(), 1), null);

        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
        buttonPanel.addButton("generateAutomaticCollectionFileView.generate", new GenerateAction());

        // Add panels to view
        setLayout(new BorderLayout());
        add(vep, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setBorder(widgetFactory.createTitleBorderWithMarginAndPadding("generateAutomaticCollectionFileView.title"));
    }

    private void generateFile() {
        HandleException.for_(this, () -> {
            Date date = collectionDateModel.getDate();
            if (date == null) {
                MessageDialog.showErrorMessage(this, "gen.invalidDate");
                return;
            }

            if (sepaFileModel.getFile() == null) {
                MessageDialog.showErrorMessage(this, "generateAutomaticCollectionFileView.noSepaFileNameSpecified");
                return;
            }

            // Let the user select the invoices that should be added to the SEPA file.
            InvoiceEditAndSelectionView invoicesView = new InvoiceEditAndSelectionView(document, true, true);
            ViewDialog dialog = new ViewDialog(getParentWindow(), invoicesView);
            dialog.showDialog();
            if (invoicesView.getSelectedInvoices() != null) {
                SepaFileGeneratorTask task = new SepaFileGeneratorTask(document, sepaFileModel.getFile(),
                        collectionDateModel.getDate(), concerningModel.getString(), invoicesView.getSelectedInvoices());

                TaskWithProgressDialog progressDialog = new TaskWithProgressDialog(this,
                        textResource.getString("generateAutomaticCollectionFileView.progressDialogTitle"));
                progressDialog.execute(task);
            }
        });
    }

    private final class GenerateAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            generateFile();
        }
    }

    private static class SepaFileGeneratorTask implements Task {

        private final Document document;
        private final File sepaFile;
        private final Date collectionDate;
        private final String description;
        private final java.util.List<Invoice> invoices;

        public SepaFileGeneratorTask(Document document, File sepaFile, Date collectionDate, String description, List<Invoice> invoices) {
            this.document = document;
            this.sepaFile = sepaFile;
            this.collectionDate = collectionDate;
            this.description = description;
            this.invoices = invoices;
        }

        @Override
        public Object execute(TaskProgressListener progressListener) throws Exception {
            progressListener.onProgressUpdate(50);

            AutomaticCollectionService automaticCollectionService = ObjectFactory.create(AutomaticCollectionService.class);
            automaticCollectionService.createSepaAutomaticCollectionFile(document, sepaFile, invoices, collectionDate, description);

            progressListener.onProgressUpdate(80);

            automaticCollectionService.validateSepaAutomaticCollectionFile(sepaFile);

            progressListener.onProgressUpdate(100);

            return null;
        }
    }

}
