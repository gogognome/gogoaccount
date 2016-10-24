package nl.gogognome.gogoaccount.component.document;

import nl.gogognome.dataaccess.transaction.CurrentTransaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class DocumentTest {

    private Document document = new Document();
    private DocumentListener listener = mock(DocumentListener.class);

    @Before
    public void initTest() {
        CurrentTransaction.transactionCreator = DocumentAwareTransaction::new;
        document.addListener(listener);
    }

    @Test
    public void whenNoTrasnsactionPresentNotifyChangeNotifiesListenerImmediately() {
        Assert.assertFalse(CurrentTransaction.hasTransaction());

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
