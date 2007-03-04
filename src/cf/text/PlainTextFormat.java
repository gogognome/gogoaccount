/*
 * $Id: PlainTextFormat.java,v 1.2 2006-12-19 19:29:06 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.text;

import nl.gogognome.text.TextResource;

/**
 * This class implements a <code>TextFormat</code> for plain text documents.
 *
 * @author Sander Kooijmans
 */
public class PlainTextFormat extends TextFormat {

    /**
     * @param textResource
     */
    public PlainTextFormat(TextResource textResource) {
        super(textResource);
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getNewLine()
     */
    public String getNewLine() {
        return "\n";
    }


    /* (non-Javadoc)
     * @see cf.text.TextFormat#endOfTable()
     */
    public String getEndOfTable() {
        return "";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getStartOfRow()
     */
    public String getStartOfRow() {
        return "";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getEndOfRow()
     */
    public String getEndOfRow() {
        return getNewLine();
    }

    public String getHeaderRow(String[] values) {
        String[] headerValues = new String[values.length];
        for (int i = 0; i < headerValues.length; i++) {
            headerValues[i] = values[i] != null ? values[i].toUpperCase() : null;
        }
        return getRow(headerValues);
    }
    
    /* (non-Javadoc)
     * @see cf.text.TextFormat#getCellLeftAligned(java.lang.String, int)
     */
    public String getCellLeftAligned(String value, int width) {
        StringBuffer sb = new StringBuffer(width);
        sb.append(value);
        if (sb.length() > width) {
            sb.setLength(width);
        }
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getCellRightAligned(java.lang.String, int)
     */
    public String getCellRightAligned(String value, int width) {
        StringBuffer sb = new StringBuffer(width);
        for (int i=0; i<width - value.length(); i++) {
            sb.append(' ');
        }
        sb.append(value);
        if (sb.length() > width) {
            sb.setLength(width);
        }
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getCellCentered(java.lang.String, int)
     */
    public String getCellCentered(String value, int width) {
        StringBuffer sb = new StringBuffer(width);
        for (int i=0; i<(width - value.length()) / 2; i++) {
            sb.append(' ');
        }
        sb.append(value);
        if (sb.length() > width) {
            sb.setLength(width);
        }
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getColumnSeparator()
     */
    public String getColumnSeparator() {
        return " | ";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getInvisibleColumnSeparator()
     */
    public String getInvisibleColumnSeparator() {
        return "   ";
    }
    
    /* (non-Javadoc)
     * @see cf.text.TextFormat#getHorizontalSeparator()
     */
    public String getHorizontalSeparator() {
        StringBuffer sb = new StringBuffer(100);
        for (int column=0; column<columnWidths.length; column++) {
            if (tableColumns.charAt(column) == '|') {
                sb.append("---");
            } else {
                for (int i=0; i<columnWidths[column]; i++) {
                    sb.append('-');
                }
            }
        }
        sb.append(getNewLine());
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getStartOfDocument()
     */
    public String getStartOfDocument() {
        return "";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getEndOfDocument()
     */
    public String getEndOfDocument() {
        return "";
    }

    public String getNewParagraph() {
        return "\n\n";
    }
    
}
 