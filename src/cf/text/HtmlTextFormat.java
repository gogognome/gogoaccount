/*
 * $Id: HtmlTextFormat.java,v 1.1 2007-03-04 20:43:02 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.text;

import nl.gogognome.text.TextResource;

/**
 * This class implements a <code>TextFormat</code> for HTML documents.
 *
 * @author Sander Kooijmans
 */
public class HtmlTextFormat extends TextFormat {

    /**
     * @param textResource
     */
    public HtmlTextFormat(TextResource textResource) {
        super(textResource);
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getNewLine()
     */
    public String getNewLine() {
        return "<br>\n";
    }


    public String getStartOfTable(String columns, int[] columnWidths) {
        this.tableColumns = columns;
        this.columnWidths = columnWidths;

        return "<table border=\"1\">\n";
    }
    
    /* (non-Javadoc)
     * @see cf.text.TextFormat#endOfTable()
     */
    public String getEndOfTable() {
        return "</table>\n";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getStartOfRow()
     */
    public String getStartOfRow() {
        return "<tr>";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getEndOfRow()
     */
    public String getEndOfRow() {
        return "</tr>\n";
    }

    
    public String getRow(String[] values) {
        StringBuffer sb = new StringBuffer("<tr>");
        for (int i = 0; i < values.length; i++) {
            char type = tableColumns.charAt(i);
            if (type != '|') {
                String alignment = "LEFT";
                if (type == 'r') {
                    alignment = "RIGHT";
                } else if (type == 'c') {
                    alignment = "CENTER";
                }
	            sb.append("<td halign=\"");
	            sb.append(alignment);
	            sb.append("\">");
	            sb.append(values[i]);
	            sb.append("</td>");
            }
        }
        sb.append("</tr>");
        return sb.toString();
    }
    
    public String getHeaderRow(String[] values) {
        StringBuffer sb = new StringBuffer("<tr>");
        for (int i = 0; i < values.length; i++) {
            char type = tableColumns.charAt(i);
            if (type != '|') {
                String alignment = "LEFT";
                if (type == 'r') {
                    alignment = "RIGHT";
                } else if (type == 'c') {
                    alignment = "CENTER";
                }
	            sb.append("<th halign=\"");
	            sb.append(alignment);
	            sb.append("\">");
	            sb.append(values[i]);
	            sb.append("</th>");
            }
        }
        sb.append("</tr>");
        return sb.toString();
    }
    
    /* (non-Javadoc)
     * @see cf.text.TextFormat#getCellLeftAligned(java.lang.String, int)
     */
    public String getCellLeftAligned(String value, int width) {
        return "<td>" + value + "</td>";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getCellRightAligned(java.lang.String, int)
     */
    public String getCellRightAligned(String value, int width) {
        return "<td halign=\"RIGHT\">" + value + "</td>";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getCellCentered(java.lang.String, int)
     */
    public String getCellCentered(String value, int width) {
        return "<td halign=\"CENTER\">" + value + "</td>";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getColumnSeparator()
     */
    public String getColumnSeparator() {
        return "";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getInvisibleColumnSeparator()
     */
    public String getInvisibleColumnSeparator() {
        return "";
    }
    
    /* (non-Javadoc)
     * @see cf.text.TextFormat#getHorizontalSeparator()
     */
    public String getHorizontalSeparator() {
        return "";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getStartOfDocument()
     */
    public String getStartOfDocument() {
        return "<html>\n\t<head></head>\n\t<body>\n";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getEndOfDocument()
     */
    public String getEndOfDocument() {
        return "\t</body>\n</html>\n";
    }

    public String getNewParagraph() {
        return "<p>";
    }
    
}
 