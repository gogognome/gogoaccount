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
package cf.ui.dialogs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.JournalItem;

/**
 * Table model for journal items.
 *
 * @author Sander Kooijmans
 */
public class ItemsTableModel extends AbstractListTableModel<JournalItem> {

	private static final long serialVersionUID = 1L;

	private final static ColumnDefinition ACCOUNT =
		new ColumnDefinition("gen.account", String.class, 300);

	private final static ColumnDefinition DEBET =
		new ColumnDefinition.Builder("gen.debet", String.class, 100)
			.add(new RightAlignedRenderer()).build();

	private final static ColumnDefinition CREDIT =
		new ColumnDefinition.Builder("gen.credit", String.class, 100)
			.add(new RightAlignedRenderer()).build();

	private final static ColumnDefinition INVOICE =
		new ColumnDefinition("gen.invoice", String.class, 300);

	private final static List<ColumnDefinition> COLUMN_DEFINTIIONS =
		Arrays.asList(ACCOUNT, DEBET, CREDIT, INVOICE);

    private Database database;

    /**
     * Constructor.
     * @param database the database
     */
    public ItemsTableModel(Database database) {
    	super(COLUMN_DEFINTIIONS, Collections.<JournalItem>emptyList());
        this.database = database;
    }

    public void setJournalItems(JournalItem[] itemsArray) {
    	replaceRows(Arrays.asList(itemsArray));
    }

    @Override
	public Object getValueAt(int row, int col) {
    	ColumnDefinition colDef = COLUMN_DEFINTIIONS.get(col);
        AmountFormat af = Factory.getInstance(AmountFormat.class);
        String result = null;
        JournalItem item = getRow(row);

        if (ACCOUNT == colDef) {
            result = item.getAccount().getId() + " " + item.getAccount().getName();
        } else if (DEBET == colDef) {
            result = item.isDebet() ? af.formatAmountWithoutCurrency(item.getAmount()) : "" ;
        } else if (CREDIT == colDef) {
            result = item.isCredit() ? af.formatAmountWithoutCurrency(item.getAmount()) : "" ;
        } else if (INVOICE == colDef) {
            Invoice invoice = database.getInvoice(item.getInvoiceId());
            result = invoice != null ? invoice.getId() + " (" + invoice.getPayingParty().getId()
                + " - " + invoice.getPayingParty().getName() + ")" : "";
        }
        return result;
    }

}