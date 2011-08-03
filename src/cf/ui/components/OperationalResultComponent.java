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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.OperationalResult;

/**
 * This class implements a graphical component that shows an operational result.
 *
 * @author Sander Kooijmans
 */
public class OperationalResultComponent extends JScrollPane {

    /** The panel that contains the components of the <code>OperationalResultComponent</code>. */
    private JPanel panel;

    private JPanel tempPanel;

    /**
     * The database used to create the operational result. Changes in this database will
     * lead to updates on this component.
     */
    private Database database;

    /**
     * The date model used to create the operational result. Changes in this model will
     * lead to updates on this component.
     */
    private DateModel dateModel;

    /**
     * Creates a new <code>OperationalResultComponent</code>.
     * @param database the datebase used to create the operational result
     * @param dateModel the date model used to determine the date of the operational result
     */
    public OperationalResultComponent(Database database, DateModel dateModel) {
        super();
        this.database = database;
        this.dateModel = dateModel;

        database.addListener(new DatabaseListener() {
            @Override
			public void databaseChanged(Database db) {
                initializeValues();
                validateTree();
            }
        });

        dateModel.addModelChangeListener(new ModelChangeListener() {
            @Override
			public void modelChanged(AbstractModel model) {
                if (((DateModel)(model)).getDate() != null) {
	                initializeValues();
	                validateTree();
                }
            }
        });

        panel = new JPanel(new GridBagLayout());
        panel.setBackground(getBackground());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tempPanel.setBackground(getBackground());
        tempPanel.add(panel);
        setViewportView(tempPanel);

        initializeValues();
    }

    private void initializeValues() {
        Date date = dateModel.getDate();
        if (date == null) {
            return; // do not change the current operational result if the date is invalid
        }

        OperationalResult operationalResult = database.getOperationalResult(date);
        TextResource tr = Factory.getInstance(TextResource.class);
        AmountFormat af = Factory.getInstance(AmountFormat.class);
        Database database = Database.getInstance();
        Account[] expenses = database.getExpenses();
        Account[] revenues = database.getRevenues();

        String[] expenseNames = new String[expenses.length];
        String[] expenseAmounts = new String[expenses.length];
        for (int i=0; i<expenses.length; i++) {
            expenseNames[i] = expenses[i].getId() + " " + expenses[i].getName();
            expenseAmounts[i] = af.formatAmount(
                BookkeepingService.getAccountBalance(database, expenses[i], operationalResult.getDate()));
        }

        String[] revenueNames = new String[revenues.length];
        String[] revenueAmounts = new String[revenues.length];
        for (int i=0; i<revenues.length; i++) {
            revenueNames[i] = revenues[i].getId() + " " + revenues[i].getName();
            revenueAmounts[i] = af.formatAmount(
                BookkeepingService.getAccountBalance(database, revenues[i], operationalResult.getDate()));
        }

        String totalExpenses = af.formatAmount(operationalResult.getTotalExpenses());
        String totalRevenues = af.formatAmount(operationalResult.getTotalRevenues());

        // The component may have been initialized before. Therefore, remove all components.
        panel.removeAll();

        // Add label for the date
        int row = 0;
        JLabel titleLabel = new JLabel(tr.getString("operationalResultComponent.title", operationalResult.getDate()));
        Font f = titleLabel.getFont();
        titleLabel.setFont(f.deriveFont(Font.BOLD).deriveFont(f.getSize() * 140.0f / 100.0f));

        panel.add(titleLabel,
                SwingUtils.createGBConstraints(0, row, 4, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        10, 0, 10, 0));
        row++;

        // Add labels for the table header
        Border bottomBorder = new LineBorder(LineBorder.LB_BOTTOM, 3);
        JLabel label = new JLabel(tr.getString("gen.expenses"));
        label.setBorder(bottomBorder);
        panel.add(label,
                SwingUtils.createGBConstraints(0, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        10, 0, 0, 0));

        label = new JLabel();
        label.setBorder(bottomBorder);
        panel.add(label,
                SwingUtils.createGBConstraints(1, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        10, 0, 0, 0));

        label = new JLabel();
        label.setBorder(bottomBorder);
        panel.add(label,
                SwingUtils.createGBConstraints(2, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        10, 0, 0, 0));

        label = new JLabel(tr.getString("gen.revenues"), SwingConstants.RIGHT);
        label.setBorder(bottomBorder);
        panel.add(label,
                SwingUtils.createGBConstraints(3, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        10, 0, 0, 0));
        row++;

        // Add the table rows
        int firstRow = row;
        Border rightBorder = new CompoundBorder(new LineBorder(LineBorder.LB_RIGHT, 1),
                new EmptyBorder(0, 0, 0, 5));
        for (int i=0; i<expenseNames.length; i++) {
            panel.add(new JLabel(expenseNames[i]),
                    SwingUtils.createGBConstraints(0, row, 1, 1, 1.0, 1.0,
                            GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                            0, 0, 0, 20));
            label = new JLabel(expenseAmounts[i], SwingConstants.RIGHT);
            label.setBorder(rightBorder);
            panel.add(label,
                    SwingUtils.createGBConstraints(1, row, 1, 1, 1.0, 1.0,
                            GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                            0, 0, 0, 0));
            row++;
        }

        Border leftBorder = new CompoundBorder(new LineBorder(LineBorder.LB_LEFT, 1),
                new EmptyBorder(0, 5, 0, 0));
        int row2 = firstRow;
        for (int i=0; i<revenueNames.length; i++)
        {
            label = new JLabel(revenueNames[i]);
            label.setBorder(leftBorder);
            panel.add(label,
                    SwingUtils.createGBConstraints(2, row2, 1, 1, 1.0, 1.0,
                            GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                            0, 0, 0, 20));
            panel.add(new JLabel(revenueAmounts[i], SwingConstants.RIGHT),
                    SwingUtils.createGBConstraints(3, row2, 1, 1, 1.0, 1.0,
                            GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                            0, 0, 0, 0));
            row2++;
        }

        while (row < row2) {
            label = new JLabel(" ", SwingConstants.RIGHT);
            label.setBorder(rightBorder);
            panel.add(label,
                    SwingUtils.createGBConstraints(1, row, 1, 1, 1.0, 1.0,
                            GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                            0, 0, 0, 0));
            row++;
        }
        while (row2 < row) {
            label = new JLabel(" ");
            label.setBorder(leftBorder);
            panel.add(label,
                    SwingUtils.createGBConstraints(2, row2, 1, 1, 1.0, 1.0,
                            GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                            0, 0, 0, 0));
            row2++;
        }

        Border topBorder = new LineBorder(LineBorder.LB_TOP, 1);
        label = new JLabel(tr.getString("gen.total"));
        label.setBorder(topBorder);
        panel.add(label,
                SwingUtils.createGBConstraints(0, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        0, 0, 10, 0));

        label = new JLabel(totalExpenses, SwingConstants.RIGHT);
        label.setBorder(topBorder);
        panel.add(label,
                SwingUtils.createGBConstraints(1, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        0, 0, 10, 5));

        label = new JLabel(tr.getString("gen.total"));
        label.setBorder(topBorder);
        panel.add(label,
                SwingUtils.createGBConstraints(2, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        0, 5, 10, 0));

        label = new JLabel(totalRevenues, SwingConstants.RIGHT);
        label.setBorder(topBorder);
        panel.add(label,
                SwingUtils.createGBConstraints(3, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        0, 0, 10, 0));
        row++;

        // Add label to push other labels to the left and top.
        panel.add(new JLabel(""),
                SwingUtils.createGBConstraints(5, row, 1, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        0, 0, 0, 0));
    }

    /**
     * Sets the background color.
     * @param color the background color
     */
    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        getViewport().setBackground(color);
        if (panel != null) {
            panel.setBackground(color);
        }
        if (tempPanel != null) {
            tempPanel.setBackground(color);
        }
    }
}
