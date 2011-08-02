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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DayOfYearComparator;
import cf.engine.Invoice;

/**
 * The table model that shows information about the invoices.
 *
 * @author Sander Kooijmans
 */
class InvoicesTableModel extends AbstractListTableModel<Invoice> {

	private final static ColumnDefinition ID =
		new ColumnDefinition("gen.id", String.class, 40);

	private final static ColumnDefinition NAME =
		new ColumnDefinition("gen.name", String.class, 200);

	private final static ColumnDefinition SALDO =
		new ColumnDefinition("gen.saldo", String.class, 200);

	private final static ColumnDefinition DATE =
		new ColumnDefinition.Builder("gen.date", Date.class, 100)
			.add(new DayOfYearComparator()).build();

	private final static List<ColumnDefinition> COLUMN_DEFINITIONS =
		Arrays.asList(ID, NAME, SALDO, DATE);

	public InvoicesTableModel(List<Invoice> initialRows) {
		super(COLUMN_DEFINITIONS, initialRows);
	}

	@Override
	public Object getValueAt(int row, int col) {
		Invoice invoice = getRow(row);
		ColumnDefinition colDef = COLUMN_DEFINITIONS.get(col);

		Object result = null;
		if (ID == colDef) {
			result = invoice.getId();
		} else if (NAME == colDef) {
			result = invoice.getPayingParty().getName();
		} else if (SALDO == colDef) {
			result = TextResource.getInstance().getAmountFormat().formatAmount(
					invoice.getRemainingAmountToBePaid(new Date()));
		} else if (DATE == colDef) {
			return invoice.getIssueDate();
		}

		return result;
	}

}
