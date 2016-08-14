package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.ledger.FormattedJournalEntry;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.ListTableModel;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FormattedJournalEntriesTableModel extends ListTableModel<FormattedJournalEntry> {

    public FormattedJournalEntriesTableModel(List<FormattedJournalEntry> rows) throws ServiceException {
    	super(
                ColumnDefinition.<FormattedJournalEntry>builder("gen.date", Date.class, 200)
                    .add(row -> row.date)
                    .build(),
                ColumnDefinition.<FormattedJournalEntry>builder("gen.id", String.class, 200)
                    .add(row -> row.id)
                    .build(),
                ColumnDefinition.<FormattedJournalEntry>builder("gen.description", String.class, 500)
                    .add(row -> row.description)
                    .build(),
                ColumnDefinition.<FormattedJournalEntry>builder("gen.invoice", String.class, 200)
                    .add(row -> row.invoiceDescription)
                    .build(),
                ColumnDefinition.<FormattedJournalEntry>builder("gen.party", String.class, 200)
                    .add(row -> row.party)
                    .build()
        );
        setRows(rows);
    }
}
