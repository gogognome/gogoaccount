package nl.gogognome.gogoaccount.component.document;

import nl.gogognome.dataaccess.DataAccessException;
import nl.gogognome.dataaccess.transaction.CompositeDatasourceTransaction;

import java.util.ArrayList;
import java.util.List;

public class DocumentAwareTransaction extends CompositeDatasourceTransaction {

    private List<Document> documents = new ArrayList<>();

    @Override
    public void close() throws DataAccessException {
        try {
            super.close();
        } finally {
            documents.forEach(d -> d.notifyListeners());
        }
    }

    public void notifyListenersWhenTransactionCloses(Document document) {
        if (!documents.contains(document)) {
            documents.add(document);
        }
    }
}
