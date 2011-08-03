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
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

/**
 * This class offers functionality to format texts. Specific subclasses
 * are available to create texts in specific formats like plain text,
 * HTML or LaTeX.
 *
 * @author Sander Kooijmans
 */
public abstract class TextFormat {

    protected TextResource textResource;

    /**
     * Contains the table columns of the current table.
     * <code>null</code> indicates that currently no table is formatted.
     */
    protected String tableColumns;

    /**
     * Contains the widths of all columns of the table.
     * <code>null</code> indicates that currently no table is formatted.
     */
    protected int[] columnWidths;

    public TextFormat(TextResource textResource) {
        this.textResource = textResource;
    }

    public abstract String getNewLine();

    public abstract String getStartOfDocument();

    public abstract String getEndOfDocument();

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
    public String getStartOfTable(String columns, int[] columnWidths) {
        if (columns.length() != columnWidths.length) {
            throw new IllegalArgumentException(
                    "The number of columns and column widths differ");
        }
        this.tableColumns = columns;
        this.columnWidths = columnWidths;
        return "";
    }

    /**
     * Gets the string that represents a single row in the table.
     * @param values the values of the cells in the row. The length
     *         must be equal to the number of columns of the table.
     * @return the string that represents a single row in the table
     */
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
                if (values[i] != null) {
                    sb.append(getColumnSeparator());
                } else {
                    sb.append(getInvisibleColumnSeparator());
                }
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
            if (i+1 < values.length) {
                sb.append(getCellSeparator());
            }
        }
        sb.append(getEndOfRow());
        return sb.toString();
    }

    /**
     * Gets the string that represents a header row in the table.
     * @param values the values of the cells in the row. The length
     *         must be equal to the number of columns of the table.
     * @return the string that represents a single row in the table
     */
    public abstract String getHeaderRow(String[] values);

    public abstract String getEndOfTable();

    public abstract String getStartOfRow();

    public abstract String getEndOfRow();

    public abstract String getCellLeftAligned(String value, int width);

    public abstract String getCellRightAligned(String value, int width);

    public abstract String getCellCentered(String value, int width);

    public String getCellSeparator() {
        return "";
    }

    public abstract String getColumnSeparator();

    public abstract String getInvisibleColumnSeparator();

    public abstract String getHorizontalSeparator();

    public abstract String getNewParagraph();

    public String getEmptyRow() {
        String[] values = new String[columnWidths.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = "";
        }
        return getRow(values);
    }

    public String formatAmount(Amount amount) {
        return Factory.getInstance(AmountFormat.class).formatAmount(amount);
    }

}
