/**
 *
 */
package nl.gogognome.cf.services.importers;

/**
 * This exception is thrown when a problem occurs while parsing an imported file.
 *
 * @author Sander Kooijmans
 */
public class ParseException extends Exception {

	public ParseException() {
		super();
	}

	public ParseException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParseException(String message) {
		super(message);
	}

	public ParseException(Throwable cause) {
		super(cause);
	}

}
