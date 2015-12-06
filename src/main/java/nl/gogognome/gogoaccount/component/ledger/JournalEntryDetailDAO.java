package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.util.AmountInDatabase;

import java.sql.SQLException;
import java.util.List;

class JournalEntryDetailDAO extends AbstractDomainClassDAO<JournalEntryDetail> {

    public JournalEntryDetailDAO(Document document) {
        super("journal_entry_detail", "domain_class_sequence", document.getBookkeepingId());
    }

    public List<JournalEntryDetail> findByJournalEntry(long journalEntryUniqueId) throws SQLException {
        return findAll(new NameValuePairs().add("journal_entry_id", journalEntryUniqueId), "id");
    }

    public void deleteByJournalEntry(long journalEntryUniqueId) throws SQLException {
        deleteWhere(new NameValuePairs().add("journal_entry_id", journalEntryUniqueId));
    }

    public boolean isAccountUsed(String accountId) throws SQLException {
        return count(new NameValuePairs().add("account_id", accountId)) > 0;
    }

    @Override
    protected JournalEntryDetail getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        JournalEntryDetail detail = new JournalEntryDetail(result.getLong("id"));
        detail.setJournalEntryUniqueId(result.getLong("journal_entry_id"));
        detail.setAmount(AmountInDatabase.parse(result.getString("amount")));
        detail.setAccountId(result.getString("account_id"));
        detail.setDebet(result.getBoolean("debet"));
        detail.setInvoiceId(result.getString("invoice_id"));
        detail.setPaymentId(result.getString("payment_id"));
        return detail;
    }

    @Override
    protected NameValuePairs getNameValuePairs(JournalEntryDetail journalEntryDetail) throws SQLException {
        return new NameValuePairs()
                .add("id", journalEntryDetail.getId())
                .add("journal_entry_id", journalEntryDetail.getJournalEntryUniqueId())
                .add("amount", AmountInDatabase.format(journalEntryDetail.getAmount()))
                .add("account_id", journalEntryDetail.getAccountId())
                .add("debet", journalEntryDetail.isDebet())
                .add("invoice_id", journalEntryDetail.getInvoiceId())
                .add("payment_id", journalEntryDetail.getPaymentId());
    }
}
