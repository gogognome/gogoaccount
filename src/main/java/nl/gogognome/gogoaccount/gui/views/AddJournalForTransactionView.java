package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.services.ImportBankStatementService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.importers.ImportedTransaction;
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
		void journalAdded(Journal journal);
	}

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
		super(document, "ajd.title", null);
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
	protected void createNewJournal(Journal journal) throws DocumentModificationFailedException, ServiceException {
		super.createNewJournal(journal);
		plugin.journalAdded(journal);
	}

	private void initValuesForImportedTransaction(ImportedTransaction t) throws ServiceException {
		dateModel.setDate(t.getDate());
		descriptionModel.setString(t.getDescription());
		ImportBankStatementService service = new ImportBankStatementService(document);
		Account debetAccount = service.getFromAccount(t);
		Account creditAccount = service.getToAccount(t);
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
	protected JournalItem createDefaultItemToBeAdded() throws ServiceException {
		switch (itemsTableModel.getRowCount()) {
		case 0: { // first item
			Account account = new ImportBankStatementService(document).getToAccount(importedTransaction);
			if (account == null) {
                account = getDefaultAccount();
            }
			return new JournalItem(importedTransaction.getAmount(), account, true);
		}

		case 1: { // second item
			Account account = new ImportBankStatementService(document).getFromAccount(importedTransaction);
			if (account == null) {
				account = getDefaultAccount();
			}
			return new JournalItem(importedTransaction.getAmount(), account, false);
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
