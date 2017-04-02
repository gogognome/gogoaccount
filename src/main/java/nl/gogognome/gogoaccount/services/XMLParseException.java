package nl.gogognome.gogoaccount.services;

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
