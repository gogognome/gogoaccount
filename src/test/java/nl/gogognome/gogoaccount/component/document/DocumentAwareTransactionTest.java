package nl.gogognome.gogoaccount.component.document;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;

public class DocumentAwareTransactionTest {

    @Test
    public void closingTransactionNotifiesListeneres() {
        Document document1 = mock(Document.class);
        Document document2 = mock(Document.class);
        DocumentAwareTransaction transaction = new DocumentAwareTransaction();
        transaction.notifyListenersWhenTransactionCloses(document1);
        transaction.notifyListenersWhenTransactionCloses(document2);

        transaction.close();

        verify(document1).notifyListeners();
        verify(document2).notifyListeners();
    }
}