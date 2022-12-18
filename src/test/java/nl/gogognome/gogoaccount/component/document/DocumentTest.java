package nl.gogognome.gogoaccount.component.document;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import nl.gogognome.dataaccess.transaction.*;

public class DocumentTest {

    private final Document document = new Document();
    private final DocumentListener listener = mock(DocumentListener.class);

    @BeforeEach
    public void initTest() {
        CurrentTransaction.transactionCreator = DocumentAwareTransaction::new;
        document.addListener(listener);
    }

    @Test
    public void whenNoTrasnsactionPresentNotifyChangeNotifiesListenerImmediately() {
        assertFalse(CurrentTransaction.hasTransaction());

        document.notifyChange();

        verify(listener).documentChanged(document);
    }

    @Test
    public void whenTrasnsactionPresentNotifyChangeDoesNotNotifyListener() {
        CurrentTransaction.create();
        document.notifyChange();

        verify(listener, never()).documentChanged(document);
        CurrentTransaction.close(true);
    }

    @Test
    public void whenTrasnsactionPresentNotifyChangeNotifiesListenerWhenTransactionClosesWithCommit() {
        CurrentTransaction.create();
        document.notifyChange();

        CurrentTransaction.close(true);

        verify(listener).documentChanged(document);
    }

    @Test
    public void whenTrasnsactionPresentNotifyChangeNotifiesListenerWhenTransactionClosesWithRollback() {
        CurrentTransaction.create();
        document.notifyChange();

        CurrentTransaction.close(false);

        verify(listener).documentChanged(document);
    }

}
