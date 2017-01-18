package nl.gogognome.gogoaccount.reportgenerators;

import nl.gogognome.lib.text.TextResource;

/**
 * This class implements a <code>TextFormat</code> for plain text documents.
 */
public class PlainTextFormat extends TextFormat {

    public PlainTextFormat(TextResource textResource) {
        super(textResource);
    }

    @Override
	public String getNewLine() {
        return System.getProperty("line.separator");
    }

    @Override
	public String getEndOfTable() {
        return "";
    }

    @Override
	public String getStartOfRow() {
        return "";
    }

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

    @Override
	public String getColumnSeparator() {
        return " | ";
    }

    @Override
	public String getInvisibleColumnSeparator() {
        return "   ";
    }

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

    @Override
	public String getStartOfDocument() {
        return "";
    }

    @Override
	public String getEndOfDocument() {
        return "";
    }

    @Override
	public String getNewParagraph() {
        return "\n\n";
    }

}
