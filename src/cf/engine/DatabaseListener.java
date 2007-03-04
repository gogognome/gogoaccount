/*
 * $Id: DatabaseListener.java,v 1.1 2006-07-25 18:14:11 sanderk Exp $
 *
 * Copyright (C) 2005 Sander Kooijmans
 *
 */

package cf.engine;

/**
 * This interface specifies a listener to changes in the database. 
 * 
 * @author Sander Kooijmans
 */
public interface DatabaseListener 
{

	/**
	 * This method is called when the database has changed.
	 * @param db the new database. Note that <tt>db</tt> need not be the same database
	 *        instance in subsequent calls!
	 */
	void databaseChanged( Database db );
}
