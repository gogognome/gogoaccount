/*
 * $Id: DeleteException.java,v 1.1 2009-12-01 19:23:59 sanderk Exp $
 */

package nl.gogognome.cf.services;

/**
 * This exception is thrown by a service method if it cannot delete  what it was supposed to delete.
 * @author Sander Kooijmans
 */
public class DeleteException extends Exception {

    public DeleteException(String message) {
        super(message);
    }

    public DeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
