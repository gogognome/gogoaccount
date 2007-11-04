/*
 * $Id: DatabaseModificationFailedException.java,v 1.1 2007-11-04 19:24:56 sanderk Exp $
 *
 * Copyright (C) 2005 Sander Kooijmans
 *
 */

package cf.engine;

/**
 * This exception is thrown when database modification failed. 
 */
public class DatabaseModificationFailedException extends Exception {

    public DatabaseModificationFailedException(String message) {
        super(message);
    }
    
    public DatabaseModificationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
