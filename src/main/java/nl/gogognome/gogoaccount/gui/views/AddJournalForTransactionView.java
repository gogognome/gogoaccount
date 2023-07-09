package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.importer.ImportBankStatementService;
import nl.gogognome.gogoaccount.component.importer.ImportedTransaction;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.services.ServiceException;
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

    private final ConfigurationService configurationService;
    private final ImportBankStatementService importBankStatementService;

    private Plugin plugin;

    private ImportedTransaction importedTransaction;

    private final JLabel fromAccount = new JLabel();
    private final JLabel amount = new JLabel();
    private final JLabel date = new JLabel();
    private final JLabel toAccount = new JLabel();
    private final JLabel description = new JLabel();

    public AddJournalForTransactionView(Document document, ConfigurationService configurationService,
                                        ImportBankStatementService importBankStatementService, InvoiceService invoiceService,
                                        LedgerService ledgerService, PartyService partyService, ViewFactory viewFactory) {
        super(document, configurationService, invoiceService, ledgerService, partyService, viewFactory);
        this.importBankStatementService = importBankStatementService;
        this.configurationService = configurationService;
        setTitle("ajd.title");
    }

    public void setInitialValuesPlugin(Plugin plugin) {
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
    protected JournalEntry createNewJournalEntry(JournalEntry journalEntry, java.util.List<JournalEntryDetail> journalEntryDetails) throws ServiceException {
        journalEntry = super.createNewJournalEntry(journalEntry, journalEntryDetails);
        plugin.journalAdded(journalEntry);
        return journalEntry;
    }

    private void initValuesForImportedTransaction(ImportedTransaction t) throws ServiceException {
        dateModel.setDate(t.date());
        descriptionModel.setString(t.description());
        Account debetAccount = importBankStatementService.getFromAccount(document, t);
        Account creditAccount = importBankStatementService.getToAccount(document, t);
        if (debetAccount != null && creditAccount != null) {
            journalEntryDetailsTableModel.addRow(createDefaultItemToBeAdded());
            journalEntryDetailsTableModel.addRow(createDefaultItemToBeAdded());
        }
    }

    private void updateLabelsForImportedTransaction(ImportedTransaction t) {
        date.setText(textResource.formatDate("gen.dateFormat", t.date()));
        fromAccount.setText(formatAccountAndName(t.fromAccount(), t.fromName()));
        toAccount.setText(formatAccountAndName(t.toAccount(), t.toName()));
        amount.setText(Factory.getInstance(AmountFormat.class).formatAmount(t.amount().toBigInteger()));
        description.setText(t.description());
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
        switch (journalEntryDetailsTableModel.getRowCount()) {
            case 0 -> { // first item
                Account account = importBankStatementService.getToAccount(document, importedTransaction);
                if (account == null) {
                    account = getDefaultAccount();
                }

                JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
                journalEntryDetail.setAmount(importedTransaction.amount());
                journalEntryDetail.setAccountId(account != null ? account.getId() : null);
                journalEntryDetail.setDebet(true);
                return journalEntryDetail;
            }
            case 1 -> { // second item
                Account account = importBankStatementService.getFromAccount(document, importedTransaction);
                if (account == null) {
                    account = getDefaultAccount();
                }
                JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
                journalEntryDetail.setAmount(importedTransaction.amount());
                journalEntryDetail.setAccountId(account != null ? account.getId() : null);
                journalEntryDetail.setDebet(false);
                return journalEntryDetail;
            }
            default -> { // other item
                return null;
            }
        }
    }

    private Account getDefaultAccount() {
        try {
            return configurationService.findAllAccounts(document).get(0);
        } catch (ServiceException e) {
            return null;
        }
    }
}
