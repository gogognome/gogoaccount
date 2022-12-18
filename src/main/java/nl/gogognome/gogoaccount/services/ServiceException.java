package nl.gogognome.gogoaccount.services;

/**
 * This exception is thrown by a service method if it cannot fulfill its contract.
 * @author Sander Kooijmans
 */
public class ServiceException extends Exception {

    public ServiceException(Throwable cause) { super(cause); }

    public ServiceException(String message) { super(message); }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
