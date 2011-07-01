/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package cf.ui.views;

import java.awt.BorderLayout;

import javax.swing.JLabel;

import nl.gogognome.cf.services.importers.ImportedTransaction;
import nl.gogognome.lib.gui.beans.ValuesEditPanel;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseModificationFailedException;
import cf.engine.Journal;
import cf.engine.JournalItem;

/**
 * This class extends the {@link EditJournalView} so that it can
 * shows details about the transaction for which a journal is created.
 * The initial values of the view depend on the current transaction.
 *
 * @author Sander Kooijmans
 */
public class AddJournalForTransactionView extends EditJournalView {

	public interface Plugin {
		public ImportedTransaction getNextImportedTransaction();
		public void journalAdded(Journal journal);
	}

	private Plugin plugin;

	private JLabel fromAccount = new JLabel();

	private JLabel amount = new JLabel();

	private JLabel date = new JLabel();

	private JLabel toAccount = new JLabel();

	private JLabel description = new JLabel();

    /**
     * Constructor. To edit an existing journal, give <code>journal</code> a non-<code>null</code> value.
     * To add one or more new journals, set <code>journal</code> to <code>null</code>.
     *
     * @param database the database to which the journal must be added
     * @param titleId the id of the title
     * @param journal the journal used to initialize the elements of the view. Must be <code>null</code>
     *        to edit a new journal
     * @param plugin plugin used to determine initial values for a new journal
     */
	public AddJournalForTransactionView(Database database, Plugin plugin) {
		super(database, "ajd.title", null);
		this.plugin = plugin;
	}

	@Override
	public void onInit() {
		super.onInit();

		addImportedTransactionComponent();
	}

	private void addImportedTransactionComponent() {
		ValuesEditPanel vep = new ValuesEditPanel();
		addDeinitializable(vep);
		vep.addField("AddJournalForTransactionView.date", date);
		vep.addField("AddJournalForTransactionView.fromAccount", fromAccount);
		vep.addField("AddJournalForTransactionView.toAccount", toAccount);
		vep.addField("AddJournalForTransactionView.amount", amount);
		vep.addField("AddJournalForTransactionView.description", description);
		vep.setBorder(WidgetFactory.getInstance().createTitleBorderWithMargin("AddJournalForTransactionView.transaction"));

		add(vep, BorderLayout.NORTH);
	}

	@Override
	protected void initValuesForNextJournal() {
		ImportedTransaction t = plugin.getNextImportedTransaction();
		if (t != null) {
			initValuesForImportedTransaction(t);
			updateLabelsForImportedTransaction(t);
		} else {
			requestClose();
		}
	}

	@Override
	protected void createNewJournal(Journal journal) throws DatabaseModificationFailedException {
		super.createNewJournal(journal);
		plugin.journalAdded(journal);
	}

	private void initValuesForImportedTransaction(ImportedTransaction t) {
		dateModel.setDate(t.getDate());
		descriptionModel.setString(t.getDescription());
		Account account = database.getAllAccounts()[0];
		itemsTableModel.addItem(new JournalItem(t.getAmount(), account, true, null, null));
		itemsTableModel.addItem(new JournalItem(t.getAmount(), account, false, null, null));
	}

	private void updateLabelsForImportedTransaction(ImportedTransaction t) {
		TextResource tr = TextResource.getInstance();
		date.setText(tr.formatDate("gen.dateFormat", t.getDate()));
		fromAccount.setText(formatAccountAndName(t.getFromAccount(), t.getFromName()));
		toAccount.setText(formatAccountAndName(t.getToAccount(), t.getToName()));
		amount.setText(tr.getAmountFormat().formatAmount(t.getAmount()));
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

}
