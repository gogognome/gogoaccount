/*
 * $Id: ParseException.java,v 1.1 2006-07-20 18:28:03 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

/**
 * This exception is thrown when a syntax error is found while parsing an XML file. 
 * 
 * @author Sander Kooijmans
 */
public class ParseException extends Exception 
{
	public ParseException( String description ) 
	{
		super(description);
	}
	
	public ParseException( Exception cause ) 
	{
		super(cause);
	}
}
