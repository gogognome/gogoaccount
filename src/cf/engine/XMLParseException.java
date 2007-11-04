/*
 * $Id: XMLParseException.java,v 1.1 2007-11-04 19:25:22 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

/**
 * This exception is thrown when a syntax error is found while parsing an XML file. 
 * 
 * @author Sander Kooijmans
 */
public class XMLParseException extends Exception 
{
	public XMLParseException( String description ) 
	{
		super(description);
	}
	
	public XMLParseException( Exception cause ) 
	{
		super(cause);
	}
}
