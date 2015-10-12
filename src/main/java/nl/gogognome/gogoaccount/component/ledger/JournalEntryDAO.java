package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;

import java.sql.SQLException;

class JournalEntryDAO extends AbstractDomainClassDAO<JournalEntry> {

    public JournalEntryDAO(Document document) {
        super("journal_entry", "domain_class_sequence", document.getBookkeepingId());
    }


    @Override
    protected JournalEntry getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        JournalEntry journalEntry = new JournalEntry(result.getLong("id"));
        journalEntry.setId(result.getString("tag"));
        journalEntry.setDate(result.getDate("date"));
        journalEntry.setIdOfCreatedInvoice("created_invoice_id");
        journalEntry.setDescription(result.getString("description"));
        return journalEntry;
    }

    @Override
    protected NameValuePairs getNameValuePairs(JournalEntry journalEntry) throws SQLException {
        return new NameValuePairs()
                .add("id", journalEntry.getUniqueId())
                .add("tag", journalEntry.getId())
                .add("date", journalEntry.getDate())
                .add("create_invoice_id", journalEntry.getIdOfCreatedInvoice())
                .add("description", journalEntry.getDescription());
    }
}
