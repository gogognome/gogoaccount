package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionService;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.gui.components.AccountFormatter;
import nl.gogognome.gogoaccount.services.ServiceException;
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
    private StringModel journalEntryIdModel = new StringModel();
    private StringModel journalEntryDescriptionModel = new StringModel();
    private nl.gogognome.lib.swing.models.ListModel<Account> bankAccountListModel = new nl.gogognome.lib.swing.models.ListModel<>();
    private nl.gogognome.lib.swing.models.ListModel<Account> debtorAccountListModel = new nl.gogognome.lib.swing.models.ListModel<>();

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
        try {
            List<Account> accounts = ObjectFactory.create(ConfigurationService.class).findAllAccounts(document);
            bankAccountListModel.setItems(accounts);
            debtorAccountListModel.setItems(accounts);
            debtorAccountListModel.setSelectedItem(accounts.stream().filter(a -> a.getType() == AccountType.DEBTOR).findFirst().orElse(null), null);

            InputFieldsColumn vep = new InputFieldsColumn();
            addCloseable(vep);

            vep.addField("generateAutomaticCollectionFileView.sepaFileName", sepaFileModel);
            vep.addField("generateAutomaticCollectionFileView.collectionDate", collectionDateModel);
            vep.addField("generateAutomaticCollectionFileView.journalEntryId", journalEntryIdModel);
            vep.addField("generateAutomaticCollectionFileView.journalEntryDescription", journalEntryDescriptionModel);
            vep.addComboBoxField("generateAutomaticCollectionFileView.bankAccount", bankAccountListModel, new AccountFormatter());
            vep.addComboBoxField("generateAutomaticCollectionFileView.debtorAccount", debtorAccountListModel, new AccountFormatter());
            collectionDateModel.setDate(DateUtil.addDays(new Date(), 1), null);

            // Create button panel
            ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
            buttonPanel.addButton("generateAutomaticCollectionFileView.generate", new GenerateAction());

            // Add panels to view
            setLayout(new BorderLayout());
            add(widgetFactory.createLabel("generateAutomaticCollectionFileView.helpText.html"), BorderLayout.NORTH);
            add(vep, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);

            setBorder(widgetFactory.createTitleBorderWithMarginAndPadding("generateAutomaticCollectionFileView.title"));
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, "gen.internalError", e);
            requestClose();
        }
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

            if (bankAccountListModel.getSelectedItem() == null) {
                MessageDialog.showErrorMessage(this, "generateAutomaticCollectionFileView.noBankAccountSelected");
                return;
            }

            if (debtorAccountListModel.getSelectedItem() == null) {
                MessageDialog.showErrorMessage(this, "generateAutomaticCollectionFileView.noDebtorAccountSelected");
                return;
            }

            // Let the user select the invoices that should be added to the SEPA file.
            InvoiceEditAndSelectionView invoicesView = new InvoiceEditAndSelectionView(document, true, true);
            ViewDialog dialog = new ViewDialog(getParentWindow(), invoicesView);
            dialog.showDialog();
            if (invoicesView.getSelectedInvoices() != null) {
                SepaFileGeneratorTask task = new SepaFileGeneratorTask(document, sepaFileModel.getFile(),
                        collectionDateModel.getDate(), invoicesView.getSelectedInvoices(), journalEntryDescriptionModel.getString(),
                        journalEntryIdModel.getString(), bankAccountListModel.getSelectedItem(), debtorAccountListModel.getSelectedItem());

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
        private final java.util.List<Invoice> invoices;
        private final String journalEntryDescription;
        private final String journalEntryId;
        private final Account bankAccount;
        private final Account debtorAccount;

        public SepaFileGeneratorTask(Document document, File sepaFile, Date collectionDate, List<Invoice> invoices,
                                     String journalEntryDescription, String journalEntryId, Account bankAccount, Account debtorAccount) {
            this.document = document;
            this.sepaFile = sepaFile;
            this.collectionDate = collectionDate;
            this.invoices = invoices;
            this.journalEntryDescription = journalEntryDescription;
            this.journalEntryId = journalEntryId;
            this.bankAccount = bankAccount;
            this.debtorAccount = debtorAccount;
        }

        @Override
        public Object execute(TaskProgressListener progressListener) throws Exception {
            progressListener.onProgressUpdate(40);

            AutomaticCollectionService automaticCollectionService = ObjectFactory.create(AutomaticCollectionService.class);
            automaticCollectionService.createSepaAutomaticCollectionFile(document, sepaFile, invoices, collectionDate);

            progressListener.onProgressUpdate(70);

            automaticCollectionService.validateSepaAutomaticCollectionFile(sepaFile);

            progressListener.onProgressUpdate(90);

            automaticCollectionService.createJournalEntryForAutomaticCollection(document, collectionDate, journalEntryId,
                    journalEntryDescription, invoices, bankAccount, debtorAccount);

            progressListener.onProgressUpdate(100);

            return null;
        }
    }

}
