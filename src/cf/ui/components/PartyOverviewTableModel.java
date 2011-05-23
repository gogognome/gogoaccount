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
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import nl.gogognome.lib.swing.SortedTableModel;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
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
public class PartyOverviewTableModel extends AbstractTableModel implements SortedTableModel {

    /** The database. */
    private Database database;

    /** The party to be shown. */
    private Party party;

    /** The date. */
    private Date date;

    private Font smallFont;

    private Font defaultFont;

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

    /** The information shown in the table. */
    LineInfo[] lineInfos;

    private Amount totalDebet;

    private Amount totalCredit;

    /**
     * Constructs a new <code>partyOverviewComponent</code>.
     * @param database the database
     * @param party the party to be shown
     * @param date the date
     */
    public PartyOverviewTableModel(Database database, Party party, Date date) {
        super();
        this.database = database;
        this.party = party;
        this.date = date;
        initializeValues();

        defaultFont = new JLabel().getFont();
        smallFont = defaultFont.deriveFont(defaultFont.getSize() * 8.0f / 10.0f);
    }

    private void initializeValues() {
        totalDebet = Amount.getZero(database.getCurrency());
        totalCredit = totalDebet;
        ArrayList<LineInfo> lineInfoList = new ArrayList<LineInfo>();
        Invoice[] invoices = database.getInvoices();

        // Sort invoices on date
        Arrays.sort(invoices, new Comparator<Invoice>() {
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
                    invoiceInfo.description = TextResource.getInstance().getString("partyOverviewTableModel.totalAmount");
                    invoiceInfo.isFirstLine = true;
                    if (!invoices[i].getAmountToBePaid().isNegative()) {
                        invoiceInfo.debetAmount = invoices[i].getAmountToBePaid();
                        totalDebet = totalDebet.add(invoiceInfo.debetAmount);
                    } else {
                        invoiceInfo.creditAmount = invoices[i].getAmountToBePaid().negate();
                        totalCredit = totalCredit.add(invoiceInfo.creditAmount);
                    }
                    lineInfoList.add(invoiceInfo);

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
                        lineInfoList.add(lineInfo);
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
                        lineInfoList.add(lineInfo);
	                }
	            }
            }
        }

        lineInfos = new LineInfo[lineInfoList.size()];
        lineInfoList.toArray(lineInfos);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() {
        return 5;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() {
        return lineInfos.length + 1;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int row, int column) {
        TextResource tr = TextResource.getInstance();
        AmountFormat af = tr.getAmountFormat();
        String result;
        if (row < lineInfos.length) {
	        switch(column) {
	        case 0:
	            if (lineInfos[row].date != null) {
    	            result = tr.formatDate(
    	                    "gen.dateFormat", lineInfos[row].date);
	            } else {
	                result = "";
	            }
	            break;

            case 1:
                result = lineInfos[row].id;
                break;

	        case 2:
	            if (lineInfos[row].isDescription) {
                    result = "   " + lineInfos[row].description;
	            } else {
	                result = lineInfos[row].description;
	            }
	            break;

	        case 3:
                if (lineInfos[row].debetAmount != null) {
                    result = af.formatAmountWithoutCurrency(lineInfos[row].debetAmount);
                } else {
                    result = "";
                }
                break;

	        case 4:
                if (lineInfos[row].creditAmount != null) {
                    result = af.formatAmountWithoutCurrency(lineInfos[row].creditAmount);
                } else {
                    result = "";
                }
                break;

	        default:
	            result = null;
	        }
        } else {
	        switch(column)
	        {
	        case 0:
	        case 1:
	            result = "";
	            break;

	        case 2:
	            result = tr.getString("gen.total");
	            break;

	        case 3:
                result = af.formatAmountWithoutCurrency(totalDebet);
	            break;

	        case 4:
                result = af.formatAmountWithoutCurrency(totalCredit);
	            break;

	        default:
	            result = null;
	        }
        }
        return result;
    }

    @Override
    public String getColumnName(int column) {
        String id = null;
        switch(column) {
        case 0:
            id = "gen.date";
            break;

        case 1:
            id = "gen.id";
            break;

        case 2:
            id = "gen.description";
            break;

        case 3:
            id = "gen.debet";
            break;

        case 4:
            id = "gen.credit";
            break;

        default:
            id = null;
        }
        return TextResource.getInstance().getString(id);
    }

    public int getColumnWidth(int column) {
        switch(column) {
        case 0: return 250;
        case 1: return 250;
        case 2: return 600;
        case 3: return 200;
        case 4: return 200;
        default: return 0;
        }
    }

    public Comparator<Object> getComparator(int column) {
        return null;
    }

    public TableCellRenderer getRendererForColumn(int column) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row < lineInfos.length && lineInfos[row].isDescription) {
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
                    if (row < lineInfos.length && lineInfos[row].isFirstLine && row > 0) {
                        // Add border to the top of the label.
                        label.setBorder(new Border() {
                            public Insets getBorderInsets(Component c) {
                                return new Insets(2, 0, 0, 0);
                            }

                            public boolean isBorderOpaque() {
                                return true;
                            }

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
}
