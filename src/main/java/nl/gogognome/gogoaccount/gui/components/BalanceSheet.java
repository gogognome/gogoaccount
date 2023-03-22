package nl.gogognome.gogoaccount.gui.components;

import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * This component shows two columns of names and amounts.
 * Use this to include a balance sheet or operational result
 * in the user interface.
 *
 * @author Sander Kooijmans
 */
public class BalanceSheet extends JPanel {

	private static final long serialVersionUID = 1L;

	public static class Row {
		public String description;
		public Amount amount;
	}

	private final String leftTitle;
	private final String rightTitle;
	private List<Row> leftRows;
	private List<Row> rightRows;

	private Amount totalLeft;
	private Amount totalRight;

	private int row;
	private int row2;

    private final TextResource tr = Factory.getInstance(TextResource.class);
    private final AmountFormat af = Factory.getInstance(AmountFormat.class);

    private final Border rightBorder = new CompoundBorder(new LineBorder(LineBorder.LB_RIGHT, 1),
        new EmptyBorder(0, 0, 0, 5));

    private final Border leftBorder = new CompoundBorder(new LineBorder(LineBorder.LB_LEFT, 1),
        new EmptyBorder(0, 5, 0, 0));

	public BalanceSheet(String leftTitle, String rightTitle) {
		super();
		this.leftTitle = leftTitle;
		this.rightTitle = rightTitle;
		this.leftRows = Collections.emptyList();
		this.rightRows = Collections.emptyList();

		setLayout(new GridBagLayout());

		addComponents();
	}

	/**
	 * Call this method after the rows have changed. The balance
	 * sheet will updated to display the current rows.
	 */
	public void update() {
		removeAll();
		addComponents();
	}

	public void setLeftRows(List<Row> leftRows) {
		this.leftRows = leftRows;
	}

	public void setRightRows(List<Row> rightRows) {
		this.rightRows = rightRows;
	}

	private void addComponents() {
        row = 0;

        totalLeft = Amount.ZERO;
        totalRight = Amount.ZERO;

        addHeader();

        row2 = row;
        addLeftRows();
        addRightRows();

        addEmptyLeftRows();
        addEmptyRightRows();

        addFooter();

        // Add label to push other labels to the left and top.
        add(new JLabel(), SwingUtils.createGBConstraints(5, row, 1, 1, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, 0, 0, 0, 0));
        row++;
	}

	private void addHeader() {
        Border bottomBorder = new LineBorder(LineBorder.LB_BOTTOM, 3);
        int col = 0;

        JLabel label = new JLabel(leftTitle);
        label.setBorder(bottomBorder);
        add(label, createHeaderConstraints(col++));

        label = new JLabel();
        label.setBorder(bottomBorder);
        add(label, createHeaderConstraints(col++));

        label = new JLabel();
        label.setBorder(bottomBorder);
        add(label, createHeaderConstraints(col++));

        label = new JLabel(rightTitle, SwingConstants.RIGHT);
        label.setBorder(bottomBorder);
        add(label, createHeaderConstraints(col++));

        row++;
	}

	private Object createHeaderConstraints(int col) {
		return SwingUtils.createGBConstraints(col, row, 1, 1, 1.0, 1.0,
            GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, 0, 0, 0, 0);
	}

	private void addLeftRows() {
        for (Row r : leftRows) {
            add(new JLabel(r.description), createRowConstraints(0, row));

            JLabel label = new JLabel(af.formatAmount(r.amount.toBigInteger()), SwingConstants.RIGHT);
            label.setBorder(rightBorder);
            add(label, createRowConstraints(1, row));

            totalLeft = totalLeft.add(r.amount);

            row++;
        }
	}

	private GridBagConstraints createRowConstraints(int x, int y) {
		int rightInset = x % 2 == 0 ? 20 : 0;
		return SwingUtils.createGBConstraints(x, y, 1, 1, 0.0, 0.0,
        		GridBagConstraints.NORTH, GridBagConstraints.BOTH, 0, 0, 0, rightInset);
	}

	private void addRightRows() {
        for (Row r : rightRows) {
            JLabel label = new JLabel(r.description);
            label.setBorder(leftBorder);
            add(label, createRowConstraints(2, row2));

            label = new JLabel(af.formatAmount(r.amount.toBigInteger()), SwingConstants.RIGHT);
            add(label, createRowConstraints(3, row2));

            totalRight = totalRight.add(r.amount);

            row2++;
        }

	}

	private void addEmptyLeftRows() {
        while (row < row2) {
            JLabel label = new JLabel(" ", SwingConstants.RIGHT);
            label.setBorder(rightBorder);
            add(label, createRowConstraints(1, row));
            row++;
        }
	}

	private void addEmptyRightRows() {
        while (row2 < row) {
            JLabel label = new JLabel(" ");
            label.setBorder(leftBorder);
            add(label, createRowConstraints(2, row2));
            row2++;
        }
	}

	private void addFooter() {
        Border topBorder = new LineBorder(LineBorder.LB_TOP, 1);
        GridBagConstraints gbc = createRowConstraints(0, row);
        gbc.insets = new Insets(0, 0, 0, 0);
        for (int col=0; col<4; col++) {
            JLabel label = new JLabel();
            gbc.gridx = col;
            label.setBorder(topBorder);
            add(label, gbc);
        }
        row++;

        int col = 0;
        JLabel label = new JLabel(tr.getString("gen.total"));
        add(label, createRowConstraints(col++, row));

        label = new JLabel(af.formatAmount(totalLeft.toBigInteger()), SwingConstants.RIGHT);
        label.setBorder(rightBorder);
        add(label, createRowConstraints(col++, row));

        label = new JLabel(tr.getString("gen.total"));
        label.setBorder(leftBorder);
        add(label, createRowConstraints(col++, row));

        label = new JLabel(af.formatAmount(totalRight.toBigInteger()), SwingConstants.RIGHT);
        add(label, createRowConstraints(col++, row));
        row++;
	}
}
