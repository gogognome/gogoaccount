package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.component.importer.ImportBankStatementService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.component.importer.ImportedTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;

import javax.swing.*;
import java.awt.*;

/**
 * This class extends the {@link EditJournalView} so that it can
 * shows details about the transaction for which a journal is created.
 * The initial values of the view depend on the current transaction.
 */
public class AddJournalForTransactionView extends EditJournalView {

    public interface Plugin {
        ImportedTransaction getNextImportedTransaction();
        void journalAdded(JournalEntry journalEntry);
    }

    private final ImportBankStatementService importBankStatementService = ObjectFactory.create(ImportBankStatementService.class);

    private Plugin plugin;

    private ImportedTransaction importedTransaction;

    private JLabel fromAccount = new JLabel();

    private JLabel amount = new JLabel();

    private JLabel date = new JLabel();

    private JLabel toAccount = new JLabel();

    private JLabel description = new JLabel();

    /**
     * Constructor. To edit an existing journal, give <code>journal</code> a non-<code>null</code> value.
     * To add one or more new journals, set <code>journal</code> to <code>null</code>.
     *
     * @param document the database to which the journal must be added
     * @param plugin plugin used to determine initial values for a new journal
     */
    public AddJournalForTransactionView(Document document, Plugin plugin) {
        super(document, "ajd.title", null, null);
        this.plugin = plugin;
    }

    @Override
    public void onInit() {
        super.onInit();

        addImportedTransactionComponent();
    }

    private void addImportedTransactionComponent() {
        InputFieldsColumn vep = new InputFieldsColumn();
        addCloseable(vep);
        vep.addVariableSizeField("AddJournalForTransactionView.date", date);
        // Order is "to account" and then "from account". This corresponds typically
        // with values on the debet and credit sides respectively.
        vep.addVariableSizeField("AddJournalForTransactionView.toAccount", toAccount);
        vep.addVariableSizeField("AddJournalForTransactionView.fromAccount", fromAccount);
        vep.addVariableSizeField("AddJournalForTransactionView.amount", amount);
        vep.addVariableSizeField("AddJournalForTransactionView.description", description);
        vep.setBorder(widgetFactory.createTitleBorderWithMarginAndPadding("AddJournalForTransactionView.transaction"));

        add(vep, BorderLayout.NORTH);
    }

    @Override
    protected void initValuesForNextJournal() throws ServiceException {
        importedTransaction = plugin.getNextImportedTransaction();
        if (importedTransaction != null) {
            initValuesForImportedTransaction(importedTransaction);
            updateLabelsForImportedTransaction(importedTransaction);
        } else {
            requestClose();
        }
    }

    @Override
    protected void createNewJournal(JournalEntry journalEntry, java.util.List<JournalEntryDetail> journalEntryDetails) throws DocumentModificationFailedException, ServiceException {
        super.createNewJournal(journalEntry, journalEntryDetails);
        plugin.journalAdded(journalEntry);
    }

    private void initValuesForImportedTransaction(ImportedTransaction t) throws ServiceException {
        dateModel.setDate(t.getDate());
        descriptionModel.setString(t.getDescription());
        Account debetAccount = importBankStatementService.getFromAccount(document, t);
        Account creditAccount = importBankStatementService.getToAccount(document, t);
        if (debetAccount != null && creditAccount != null) {
            itemsTableModel.addRow(createDefaultItemToBeAdded());
            itemsTableModel.addRow(createDefaultItemToBeAdded());
        }
    }

    private void updateLabelsForImportedTransaction(ImportedTransaction t) {
        date.setText(textResource.formatDate("gen.dateFormat", t.getDate()));
        fromAccount.setText(formatAccountAndName(t.getFromAccount(), t.getFromName()));
        toAccount.setText(formatAccountAndName(t.getToAccount(), t.getToName()));
        amount.setText(Factory.getInstance(AmountFormat.class).formatAmount(t.getAmount()));
        description.setText(t.getDescription());
    }

    private String formatAccountAndName(String account, String name) {
        if (account != null && name != null) {
            return account + " (" + name + ")";
        } else if (account != null) {
            return account;
        } else {
            return name;
        }
    }

    @Override
    protected JournalEntryDetail createDefaultItemToBeAdded() throws ServiceException {
        switch (itemsTableModel.getRowCount()) {
        case 0: { // first item
            Account account = importBankStatementService.getToAccount(document, importedTransaction);
            if (account == null) {
                account = getDefaultAccount();
            }

            JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
            journalEntryDetail.setAmount(importedTransaction.getAmount());
            journalEntryDetail.setAccountId(account != null ? account.getId() : null);
            journalEntryDetail.setDebet(true);
            return journalEntryDetail;
        }

        case 1: { // second item
            Account account = importBankStatementService.getFromAccount(document, importedTransaction);
            if (account == null) {
                account = getDefaultAccount();
            }
            JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
            journalEntryDetail.setAmount(importedTransaction.getAmount());
            journalEntryDetail.setAccountId(account != null ? account.getId() : null);
            journalEntryDetail.setDebet(false);
            return journalEntryDetail;
        }
        default: // other item
            return null;
        }
    }

    private Account getDefaultAccount() {
        try {
            return Factory.getInstance(ConfigurationService.class).findAllAccounts(document).get(0);
        } catch (ServiceException e) {
            return null;
        }
    }
}
