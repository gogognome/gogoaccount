package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionService;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.gui.components.AccountFormatter;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import nl.gogognome.lib.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Date;
import java.util.List;

public class GenerateAutomaticCollectionFileView extends View {

    private final Document document;
    private final AutomaticCollectionService automaticCollectionService;
    private final ConfigurationService configurationService;
    private final MessageDialog messageDialog;
    private final HandleException handleException;

    private List<Invoice> selectedInvoices;
    private FileModel sepaFileModel = new FileModel();
    private DateModel collectionDateModel = new DateModel(new Date());
    private StringModel journalEntryIdModel = new StringModel();
    private StringModel journalEntryDescriptionModel = new StringModel();
    private nl.gogognome.lib.swing.models.ListModel<Account> bankAccountListModel = new nl.gogognome.lib.swing.models.ListModel<>();
    private nl.gogognome.lib.swing.models.ListModel<Account> debtorAccountListModel = new nl.gogognome.lib.swing.models.ListModel<>();

    public GenerateAutomaticCollectionFileView(Document document, AutomaticCollectionService automaticCollectionService,
                                               ConfigurationService configurationService) {
        this.document = document;
        this.automaticCollectionService = automaticCollectionService;
        this.configurationService = configurationService;
        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);
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
        handleException.of(() -> {
            List<Account> accounts = configurationService.findAllAccounts(document);
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

            ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
            buttonPanel.addButton("generateAutomaticCollectionFileView.generate", this::onGenerateFile);

            setLayout(new BorderLayout());
            add(widgetFactory.createLabel("generateAutomaticCollectionFileView.helpText.html"), BorderLayout.NORTH);
            add(vep, BorderLayout.NORTH);
            add(buttonPanel, BorderLayout.SOUTH);

            setBorder(widgetFactory.createTitleBorderWithMarginAndPadding("generateAutomaticCollectionFileView.title"));
        });
    }

    private void onGenerateFile() {
        handleException.of(() -> {
            boolean valid = validateInput();
            if (!valid) {
                return;
            }

            SepaFileGeneratorTask task = new SepaFileGeneratorTask(document, automaticCollectionService, sepaFileModel.getFile(),
                    collectionDateModel.getDate(), selectedInvoices, journalEntryDescriptionModel.getString(),
                    journalEntryIdModel.getString(), bankAccountListModel.getSelectedItem(), debtorAccountListModel.getSelectedItem());

            TaskWithProgressDialog progressDialog = new TaskWithProgressDialog(this,
                    textResource.getString("generateAutomaticCollectionFileView.progressDialogTitle"));
            progressDialog.execute(task);
        });
    }

    private boolean validateInput() {
        boolean valid = true;
        Date date = collectionDateModel.getDate();
        if (date == null) {
            messageDialog.showWarningMessage("gen.invalidDate");
            valid = false;
        }

        if (sepaFileModel.getFile() == null) {
            messageDialog.showWarningMessage("generateAutomaticCollectionFileView.noSepaFileNameSpecified");
            valid = false;
        }

        if (bankAccountListModel.getSelectedItem() == null) {
            messageDialog.showWarningMessage("generateAutomaticCollectionFileView.noBankAccountSelected");
            valid = false;
        }

        if (debtorAccountListModel.getSelectedItem() == null) {
            messageDialog.showWarningMessage("generateAutomaticCollectionFileView.noDebtorAccountSelected");
            valid = false;
        }
        return valid;
    }

    public void setSelectedInvoices(List<Invoice> selectedInvoices) {
        this.selectedInvoices = selectedInvoices;
    }

    private static class SepaFileGeneratorTask implements Task {

        private final Document document;
        private final AutomaticCollectionService automaticCollectionService;
        private final File sepaFile;
        private final Date collectionDate;
        private final java.util.List<Invoice> invoices;
        private final String journalEntryDescription;
        private final String journalEntryId;
        private final Account bankAccount;
        private final Account debtorAccount;

        public SepaFileGeneratorTask(Document document, AutomaticCollectionService automaticCollectionService, File sepaFile, Date collectionDate, List<Invoice> invoices,
                                     String journalEntryDescription, String journalEntryId, Account bankAccount, Account debtorAccount) {
            this.document = document;
            this.automaticCollectionService = automaticCollectionService;
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
            automaticCollectionService.createSepaAutomaticCollectionFile(document, sepaFile, invoices, collectionDate, progressListener);
            automaticCollectionService.validateSepaAutomaticCollectionFile(sepaFile);
            automaticCollectionService.createJournalEntryForAutomaticCollection(document, collectionDate, journalEntryId,
                    journalEntryDescription, invoices, bankAccount, debtorAccount);
            File csvFile = new File(sepaFile.getParent(), createCsvFileName());
            automaticCollectionService.createCsvForAutomaticCollectionFile(document, csvFile, invoices);
            return null;
        }

        private String createCsvFileName() {
            String csvFileName = sepaFile.getName();
            int index = csvFileName.lastIndexOf('.');
            if (index != -1) {
                csvFileName = csvFileName.substring(0, index) + ".csv";
            } else {
                csvFileName += ".csv";
            }
            return csvFileName;
        }
    }

}
