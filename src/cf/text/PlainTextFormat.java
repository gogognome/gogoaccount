/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package cf.text;

import nl.gogognome.lib.text.TextResource;

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
    @Override
	public String getNewLine() {
        return "\n";
    }


    /* (non-Javadoc)
     * @see cf.text.TextFormat#endOfTable()
     */
    @Override
	public String getEndOfTable() {
        return "";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getStartOfRow()
     */
    @Override
	public String getStartOfRow() {
        return "";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getEndOfRow()
     */
    @Override
	public String getEndOfRow() {
        return getNewLine();
    }

    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
	public String getColumnSeparator() {
        return " | ";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getInvisibleColumnSeparator()
     */
    @Override
	public String getInvisibleColumnSeparator() {
        return "   ";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getHorizontalSeparator()
     */
    @Override
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
    @Override
	public String getStartOfDocument() {
        return "";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getEndOfDocument()
     */
    @Override
	public String getEndOfDocument() {
        return "";
    }

    @Override
	public String getNewParagraph() {
        return "\n\n";
    }

}
