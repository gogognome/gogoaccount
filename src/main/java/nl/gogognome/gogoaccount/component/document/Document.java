package nl.gogognome.gogoaccount.component.document;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class Document {

    protected Connection connectionToKeepInMemoryDatabaseAlive;

    /** The name of the file from which the database was loaded. */
    private String fileName;

    private Locale locale = Locale.US;

    /**
     * Contains the <tt>DatabaseListeners</tt>.
     */
    private final ArrayList<DocumentListener> listeners = new ArrayList<>();

    private final String bookkeepingId = UUID.randomUUID().toString();

    protected Document() {
    }

    public String getBookkeepingId() {
        return bookkeepingId;
    }

    /**
     * Adds a database listener.
     * @param l the database listener.
     */
    public void addListener( DocumentListener l ) {
        listeners.add(l);
    }

    /**
     * Removes a database listener.
     * @param l the database listener.
     */
    public void removeListener( DocumentListener l ) {
        listeners.remove(l);
    }

    /** Notifies the listeners. */
    private void notifyListeners()
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
        notifyListeners();
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
}
