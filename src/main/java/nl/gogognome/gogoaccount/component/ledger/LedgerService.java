package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;

import javax.print.Doc;
import java.util.List;

public class LedgerService {

    public JournalEntry createJournalEntry(Document document, JournalEntry journalEntry) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            JournalEntry createdJournalEntry = new JournalEntryDAO(document).create(journalEntry);
            document.notifyChange();
            return createdJournalEntry;
        });
    }

    public List<JournalEntryDetail> findJournalEntryDetails(Document document, JournalEntry journalEntry) throws ServiceException {
        return ServiceTransaction.withResult(() -> new JournalEntryDetailDAO(document).findByJournalEntry(journalEntry.getUniqueId()));
    }
}
