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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.event.TableModelListener;

import nl.gogognome.gogoaccount.businessobjects.Invoice;
import nl.gogognome.gogoaccount.businessobjects.Journal;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.database.DatabaseListener;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;

/**
 * This class implements a table model for a table containing journals.
 *
 * @author Sander Kooijmans
 */
public class JournalsTableModel extends AbstractListTableModel<Journal>
		implements DatabaseListener {

	private final static ColumnDefinition DATE =
		new ColumnDefinition("gen.date", Date.class, 200);

	private final static ColumnDefinition ID =
		new ColumnDefinition("gen.id", String.class, 200);

	private final static ColumnDefinition DESCRIPTION =
		new ColumnDefinition("gen.description", String.class, 500);

	private final static ColumnDefinition INVOICE =
		new ColumnDefinition("gen.invoice", String.class, 200);

	private final static List<ColumnDefinition> COLUMNS =
		Arrays.asList(DATE, ID, DESCRIPTION, INVOICE);

    /** Contains the <code>TableModelListener</code>s of this <code>TableModel</code>. */
    private ArrayList<TableModelListener> journalsTableModelListeners = new ArrayList<TableModelListener>();

    private Database database;

    /**
     * Constructor.
     * @param database the database from which to take the data
     */
    public JournalsTableModel(Database database) {
    	super(COLUMNS, database.getJournals());
        this.database = database;
        database.addListener(this);
    }

    @Override
	public void databaseChanged(Database db) {
        replaceRows(database.getJournals());
    }

	@Override
	public Object getValueAt(int row, int col) {
        Journal journal = getRow(row);
        Object result = null;
        ColumnDefinition colDef = COLUMNS.get(col);
        if (DATE == colDef) {
            result = journal.getDate();
        } else if (ID == colDef) {
            result = journal.getId();
        } else if (DESCRIPTION == colDef) {
            result = journal.getDescription();
        } else if (INVOICE == colDef) {
            String id = journal.getIdOfCreatedInvoice();
            if (id != null) {
                Invoice invoice = database.getInvoice(id);
                result = invoice.getId() + " (" + invoice.getConcerningParty().getName() + ")";
            }
        }
        return result;
    }

}
