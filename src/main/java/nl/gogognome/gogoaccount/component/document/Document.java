package nl.gogognome.gogoaccount.component.document;

import nl.gogognome.dataaccess.transaction.CurrentTransaction;
import nl.gogognome.dataaccess.transaction.Transaction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class Document {

    private final ArrayList<DocumentListener> listeners = new ArrayList<>();
    private final String bookkeepingId = UUID.randomUUID().toString();
    private String fileName;
    private Locale locale = Locale.US;
    protected Connection connectionToKeepInMemoryDatabaseAlive;
    private boolean readonly;

    protected Document() {
    }

    public String getBookkeepingId() {
        return bookkeepingId;
    }

    public void addListener( DocumentListener l ) {
        listeners.add(l);
    }

    public void removeListener( DocumentListener l ) {
        listeners.remove(l);
    }

    void notifyListeners()
    {
        for (DocumentListener l : listeners) {
            l.documentChanged(this);
        }
    }

    /**
     * This method is called each time the database changes.
     * This method will make sure that the <tt>DatabaseListener</tt>s get notified
     * at the proper moment only if this database is the current database.
     */
    public void notifyChange() {
        DocumentAwareTransaction documentAwareTransaction = null;
        if (CurrentTransaction.hasTransaction()) {
            Transaction transaction = CurrentTransaction.get();
            if (transaction instanceof DocumentAwareTransaction) {
                documentAwareTransaction = (DocumentAwareTransaction) transaction;
            }
        }
        if (documentAwareTransaction != null) {
            documentAwareTransaction.notifyListenersWhenTransactionCloses(this);
        } else {
            notifyListeners();
        }
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
        notifyChange();
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public void ensureDocumentIsWriteable() {
        if (readonly) {
            throw new AttemptToModifyReadonlyDocumentException();
        }
    }
}
