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
package cf.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Party;
import cf.engine.Payment;

/**
 * This class implements a model for a <code>JTable</code> that shows an overview
 * of a party at a specific date.
 *
 * @author Sander Kooijmans
 */
public class PartyOverviewTableModel extends AbstractListTableModel<LineInfo> {

	private final static ColumnDefinition DATE =
		new ColumnDefinition("gen.date", Date.class, 250);

	private final static ColumnDefinition ID =
		new ColumnDefinition("gen.id", String.class, 250);

	private final static ColumnDefinition DESCRIPTION =
		new ColumnDefinition("gen.description", String.class, 600);

	private final static ColumnDefinition DEBET =
		new ColumnDefinition("gen.debet", String.class, 250);

	private final static ColumnDefinition CREDIT =
		new ColumnDefinition("gen.credit", String.class, 250);

	private final static List<ColumnDefinition> COLUMN_DEFINITIONS =
		Arrays.asList(DATE, ID, DESCRIPTION, DEBET, CREDIT);

    private Database database;
    private Party party;
    private Date date;

    private Font smallFont;
    private Font defaultFont;

    /**
     * Constructs a new <code>partyOverviewComponent</code>.
     * @param database the database
     * @param party the party to be shown
     * @param date the date
     */
    public PartyOverviewTableModel(Database database, Party party, Date date) {
        super(COLUMN_DEFINITIONS, Collections.<LineInfo>emptyList());
        this.database = database;
        this.party = party;
        this.date = date;
        initializeValues();

        defaultFont = new JLabel().getFont();
        smallFont = defaultFont.deriveFont(defaultFont.getSize() * 8.0f / 10.0f);
    }

    private void initializeValues() {
        Amount totalDebet = Amount.getZero(database.getCurrency());
        Amount totalCredit = totalDebet;
        ArrayList<LineInfo> lineInfos = new ArrayList<LineInfo>();
        Invoice[] invoices = database.getInvoices();

        // Sort invoices on date
        Arrays.sort(invoices, new Comparator<Invoice>() {
            @Override
			public int compare(Invoice o1, Invoice o2) {
                return DateUtil.compareDayOfYear(o1.getIssueDate(), o2.getIssueDate());
            }
        });

        for (int i = 0; i < invoices.length; i++) {
            if (party.equals(invoices[i].getPayingParty())) {
                if (DateUtil.compareDayOfYear(invoices[i].getIssueDate(),date) <= 0) {
                    // Add a line with the total amount to be paid
                    LineInfo invoiceInfo = new LineInfo();
                    invoiceInfo.id = invoices[i].getId();
                    invoiceInfo.date = invoices[i].getIssueDate();
                    invoiceInfo.description = Factory.getInstance(TextResource.class).getString("partyOverviewTableModel.totalAmount");
                    invoiceInfo.isFirstLine = true;
                    if (!invoices[i].getAmountToBePaid().isNegative()) {
                        invoiceInfo.debetAmount = invoices[i].getAmountToBePaid();
                        totalDebet = totalDebet.add(invoiceInfo.debetAmount);
                    } else {
                        invoiceInfo.creditAmount = invoices[i].getAmountToBePaid().negate();
                        totalCredit = totalCredit.add(invoiceInfo.creditAmount);
                    }
                    lineInfos.add(invoiceInfo);

                    // Add a line per description of the invoice
                    String[] descriptions = invoices[i].getDescriptions();
                    Amount[] amounts = invoices[i].getAmounts();
                    for (int l=0; l<descriptions.length; l++) {
                        LineInfo lineInfo = new LineInfo();
                        lineInfo.isDescription = true;
//                        lineInfo.id = invoices[i].getId();
//                        lineInfo.date = invoices[i].getIssueDate();
                        lineInfo.description = descriptions[l];
                        if (amounts[l] != null) {
                            if (!amounts[l].isNegative()) {
                                lineInfo.debetAmount = amounts[l];
                            } else {
                                lineInfo.creditAmount = amounts[l].negate();
                            }
                        }
                        lineInfos.add(lineInfo);
                    }
                }
	            List<Payment> payments = invoices[i].getPayments();
	            for (Payment payment : payments) {
	                if (DateUtil.compareDayOfYear(payment.getDate(), date) <= 0) {
	                    LineInfo lineInfo = new LineInfo();
                        lineInfo.id = invoices[i].getId();
	                    lineInfo.date = payment.getDate();
	                    lineInfo.description = payment.getDescription();
	                    if (!payment.getAmount().isNegative()) {
	                        lineInfo.creditAmount = payment.getAmount();
	                        totalCredit = totalCredit.add(lineInfo.creditAmount);
	                    } else {
                            lineInfo.debetAmount = payment.getAmount().negate();
                            totalDebet = totalDebet.add(lineInfo.debetAmount);
	                    }
                        lineInfos.add(lineInfo);
	                }
	            }
            }
        }

        LineInfo totalLineInfo = new LineInfo();
        totalLineInfo.id = Factory.getInstance(TextResource.class).getString("gen.total");
        totalLineInfo.debetAmount = totalDebet;
        totalLineInfo.creditAmount = totalCredit;
        lineInfos.add(totalLineInfo);

        replaceRows(lineInfos);
    }

    @Override
	public Object getValueAt(int row, int column) {
        Object result = null;

        LineInfo lineInfo = getRow(row);
        ColumnDefinition colDef = COLUMN_DEFINITIONS.get(column);
        TextResource tr = Factory.getInstance(TextResource.class);
        AmountFormat af = Factory.getInstance(AmountFormat.class);

        if (DATE == colDef) {
            if (lineInfo.date != null) {
	            result = tr.formatDate("gen.dateFormat", lineInfo.date);
            }
        } else if (ID == colDef) {
        	result = lineInfo.id;
        } else if (DESCRIPTION == colDef) {
            if (lineInfo.isDescription) {
                result = "   " + lineInfo.description;
            } else {
                result = lineInfo.description;
            }
        } else if (DEBET == colDef) {
            if (lineInfo.debetAmount != null) {
                result = af.formatAmountWithoutCurrency(lineInfo.debetAmount);
            }
        } else if (CREDIT == colDef) {
            if (lineInfo.creditAmount != null) {
                result = af.formatAmountWithoutCurrency(lineInfo.creditAmount);
            }
        }

        return result;
    }

    @Override
	public TableCellRenderer getRendererForColumn(int column) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (getRow(row).isDescription) {
                    comp.setFont(smallFont);
                } else {
                    comp.setFont(defaultFont);
                }

                if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    if (column >= 3) {
                        label.setHorizontalAlignment(SwingConstants.RIGHT);
                    } else {
                        label.setHorizontalAlignment(SwingConstants.LEFT);
                    }
                    if (getRow(row).isFirstLine && row > 0) {
                        // Add border to the top of the label.
                        label.setBorder(new Border() {
                            @Override
							public Insets getBorderInsets(Component c) {
                                return new Insets(2, 0, 0, 0);
                            }

                            @Override
							public boolean isBorderOpaque() {
                                return true;
                            }

                            @Override
							public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                                g.setColor(Color.BLACK);
                                g.drawRect(x, y, width, 1);
                            }

                        });
                    }
                }

                return comp;
            }
        };
    }

    @Override
    public TableCellEditor getEditorForColumn(int column) {
    	return null;
    }
}

/** This class contains the information to be shown in a single row of the table. */
class LineInfo {
    Date date;
    String id;
    String description;
    Amount debetAmount;
    Amount creditAmount;
    boolean isDescription;
    boolean isFirstLine;
}

