package nl.gogognome.gogoaccount.components.document;

/**
 * This interface specifies a listener to changes in the document.
 */
public interface DocumentListener
{

	/**
	 * This method is called when the database has changed.
	 * @param document the new document. Note that <tt>document</tt> need not be the same document
	 *        instance in subsequent calls!
	 */
	void documentChanged(Document document);
}
