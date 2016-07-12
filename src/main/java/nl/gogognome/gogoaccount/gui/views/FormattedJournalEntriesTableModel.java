package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.ledger.FormattedJournalEntry;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FormattedJournalEntriesTableModel extends AbstractListTableModel<FormattedJournalEntry> {

	private final static ColumnDefinition DATE =
		new ColumnDefinition("gen.date", Date.class, 200);

	private final static ColumnDefinition ID =
		new ColumnDefinition("gen.id", String.class, 200);

	private final static ColumnDefinition DESCRIPTION =
		new ColumnDefinition("gen.description", String.class, 500);

	private final static ColumnDefinition INVOICE =
		new ColumnDefinition("gen.invoice", String.class, 200);

	private final static ColumnDefinition PARTY =
		new ColumnDefinition("gen.party", String.class, 200);

	private final static List<ColumnDefinition> COLUMNS =
		Arrays.asList(DATE, ID, DESCRIPTION, INVOICE, PARTY);

    public FormattedJournalEntriesTableModel(List<FormattedJournalEntry> rows) throws ServiceException {
    	super(COLUMNS, rows);
    }

    @Override
	public Object getValueAt(int rowIndex, int columnIndex) {
        FormattedJournalEntry row = getRow(rowIndex);
        Object result = null;
        ColumnDefinition colDef = COLUMNS.get(columnIndex);
        if (DATE == colDef) {
            result = row.date;
        } else if (ID == colDef) {
            result = row.id;
        } else if (DESCRIPTION == colDef) {
            result = row.description;
        } else if (INVOICE == colDef) {
            result = row.invoiceDescription;
        } else if (PARTY == colDef) {
            result = row.party;
        }
        return result;
    }

}
