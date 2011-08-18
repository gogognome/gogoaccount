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
package nl.gogognome.gogoaccount.gui.views;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;

class TransactionsJournalsTableModel extends AbstractListTableModel<Transaction> {

	private Database database;

    private final static ColumnDefinition DATE =
        new ColumnDefinition("importBankStatementView.date", Date.class, 70);

    private final static ColumnDefinition TO_ACCOUNT =
        new ColumnDefinition("importBankStatementView.toAccount", String.class, 100);

    private final static ColumnDefinition FROM_ACCOUNT =
        new ColumnDefinition("importBankStatementView.fromAccount", String.class, 100);

    private final static ColumnDefinition AMOUNT =
        new ColumnDefinition("importBankStatementView.amount", String.class, 100);

    private final static ColumnDefinition DESCRIPTION1 =
        new ColumnDefinition("importBankStatementView.description1", String.class, 200);

    private final static ColumnDefinition ID =
        new ColumnDefinition("importBankStatementView.id", String.class, 50);

    private final static ColumnDefinition DESCRIPTION2 =
        new ColumnDefinition("importBankStatementView.description2", String.class, 200);

    private final static ColumnDefinition INVOICE =
        new ColumnDefinition("importBankStatementView.invoice", String.class, 200);

    private final static List<ColumnDefinition> COLUMN_DEFINITIONS = Arrays.asList(
        DATE, TO_ACCOUNT, FROM_ACCOUNT, AMOUNT, DESCRIPTION1, ID, DESCRIPTION2, INVOICE
    );

    public TransactionsJournalsTableModel(List<Transaction> transactions, Database database) {
        super(COLUMN_DEFINITIONS, transactions);
        this.database = database;
    }

    /** {@inheritDoc} */
    @Override
	public Object getValueAt(int rowIndex, int columnIndex) {
        ColumnDefinition colDef = getColumnDefinition(columnIndex);
        if (DATE.equals(colDef)) {
            return getRow(rowIndex).getImportedTransaction().getDate();
        } else if (FROM_ACCOUNT.equals(colDef)) {
            return getRow(rowIndex).getImportedTransaction().getFromAccount();
        } else if (TO_ACCOUNT.equals(colDef)) {
            return getRow(rowIndex).getImportedTransaction().getToAccount();
        } else if (AMOUNT.equals(colDef)) {
        	AmountFormat af = Factory.getInstance(AmountFormat.class);
            return af.formatAmount(getRow(rowIndex).getImportedTransaction().getAmount());
        } else if (DESCRIPTION1.equals(colDef)) {
            return getRow(rowIndex).getImportedTransaction().getDescription();
        } else if (ID.equals(colDef)) {
            return getRow(rowIndex).getJournal() != null ? getRow(rowIndex).getJournal().getId() : null;
        } else if (DESCRIPTION2.equals(colDef)) {
            return getRow(rowIndex).getJournal() != null ? getRow(rowIndex).getJournal().getDescription() : null;
        } else if (INVOICE.equals(colDef)) {
        	Journal journal = getRow(rowIndex).getJournal();
            if (journal != null) {
                String id = journal.getIdOfCreatedInvoice();
                if (id != null) {
                    Invoice invoice = database.getInvoice(id);
                    return invoice.getId() + " (" + invoice.getConcerningParty().getName() + ")";
                }
            }
        }
        return null;
    }

}