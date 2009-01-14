/*
 * $Id: CreationException.java,v 1.1 2009-01-14 21:32:15 sanderk Exp $
 */

package nl.gogognome.cf.services;

/**
 * This exception is thrown by a service method if it cannot create what it was supposed to create.
 * @author Sander Kooijmans
 */
public class CreationException extends Exception {

    public CreationException(String message) {
        super(message);
    }
    
    public CreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
