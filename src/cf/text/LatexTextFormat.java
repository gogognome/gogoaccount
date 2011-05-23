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

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.TextResource;

/**
 * This class implements a <code>TextFormat</code> for Latex files.
 *
 * @author Sander Kooijmans
 */
public class LatexTextFormat extends TextFormat {

    /**
     * @param textResource
     */
    public LatexTextFormat(TextResource textResource) {
        super(textResource);
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getNewLine()
     */
    @Override
	public String getNewLine() {
        return "\n\n";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getStartOfDocument()
     */
    @Override
	public String getStartOfDocument() {
        return "\\documentclass[a4paper,10pt]{article}\n" +
        	"\\usepackage[dutch]{babel}\n" +
        	"\\usepackage{url}\n" +
        	"\\usepackage{eurosym}\n" +
        	"\\global\\def\\input{}\n" +
        	"\\global\\def\\include{}\n" +
        	"\\global\\def\\listfiles{}\n" +
        	"\\global\\def\\typein{}\n" +
        	"\\global\\def\\special{}\n" +
        	"\\nofiles\n" +
        	"\\begin{document}\n\n";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getEndOfDocument()
     */
    @Override
	public String getEndOfDocument() {
        return "\\end{document}";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getHeaderRow(java.lang.String[])
     */
    @Override
	public String getHeaderRow(String[] values) {
        String[] headerValues = new String[values.length];
        for (int i = 0; i < headerValues.length; i++) {
            headerValues[i] = values[i] != null ? "\\textbf{" + values[i] + "}" : null;
        }
        return getRow(headerValues);
    }

    /**
     * Gets the string for the start of a table.
     * @param columns specifies the columns of the table. Each column is represented
     *                 by one character.
     *         <ul>
     *           <li>'l' for a left-algined column
     *           <li>'r' for a right-algined column
     *           <li>'c' for a centered column
     *           <li>'|' for a column separator
     *         </ul>
     * @param columnWidths the widths of the columns
     * @return the string for the start of the table
     */
    @Override
	public String getStartOfTable(String columns, int[] columnWidths) {
        super.getStartOfTable(columns, columnWidths);
        return "\\begin{tabular}{" + columns + "}\n";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getEndOfTable()
     */
    @Override
	public String getEndOfTable() {
        return "\\end{tabular}\n";
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
        return " \\\\ \n";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getCellLeftAligned(java.lang.String, int)
     */
    @Override
	public String getCellLeftAligned(String value, int width) {
        return value;
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getCellRightAligned(java.lang.String, int)
     */
    @Override
	public String getCellRightAligned(String value, int width) {
        return value;
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getCellCentered(java.lang.String, int)
     */
    @Override
	public String getCellCentered(String value, int width) {
        return value;
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getColumnSeparator()
     */
    @Override
	public String getColumnSeparator() {
        return " ";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getInvisibleColumnSeparator()
     */
    @Override
	public String getInvisibleColumnSeparator() {
        return " ";
    }

    /* (non-Javadoc)
     * @see cf.text.TextFormat#getHorizontalSeparator()
     */
    @Override
	public String getHorizontalSeparator() {
        return "\\hline";
    }

    @Override
	public String getCellSeparator() {
        return " & ";
    }

    @Override
	public String getRow(String[] values) {
        if (values.length != columnWidths.length) {
            throw new IllegalArgumentException(
                    "The number of values differs from the number of columns");
        }
        StringBuffer sb = new StringBuffer(100);
        sb.append(getStartOfRow());
        for (int i=0; i<values.length; i++) {
            switch(tableColumns.charAt(i)) {
            case '|':
                break;
            case 'r':
                sb.append(getCellRightAligned(values[i], columnWidths[i]));
                break;
            case 'c':
                sb.append(getCellCentered(values[i], columnWidths[i]));
                break;
            default:
                sb.append(getCellLeftAligned(values[i], columnWidths[i]));
            	break;
            }
            if (tableColumns.charAt(i) != '|' && i+1 < values.length) {
                sb.append(getCellSeparator());
            }
        }
        sb.append(getEndOfRow());
        return sb.toString();
    }

    @Override
	public String getNewParagraph() {
        return "\n\\paragraph{}\n";
    }

    @Override
	public String formatAmount(Amount amount) {
        return textResource.getAmountFormat().formatAmount(amount, "\\euro");
    }
}
