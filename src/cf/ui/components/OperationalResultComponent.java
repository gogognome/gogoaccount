/*
 * $Id: OperationalResultComponent.java,v 1.9 2007-07-29 12:33:40 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import java.awt.Color;
import java.awt.FlowLayout;
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

import nl.gogognome.framework.models.AbstractModel;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.framework.models.ModelChangeListener;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
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
            public void databaseChanged(Database db) {
                initializeValues();
                validateTree();
            }
        });
        
        dateModel.addModelChangeListener(new ModelChangeListener() {
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
        TextResource tr = TextResource.getInstance();
        AmountFormat af = tr.getAmountFormat();
        Database database = Database.getInstance();
        Account[] expenses = database.getExpenses();
        Account[] revenues = database.getRevenues();

        String[] expenseNames = new String[expenses.length];
        String[] expenseAmounts = new String[expenses.length];
        for (int i=0; i<expenses.length; i++) {
            expenseNames[i] = expenses[i].getId() + " " + expenses[i].getName();
            expenseAmounts[i] = af.formatAmount(expenses[i].getBalance(operationalResult.getDate()));
        }
        
        String[] revenueNames = new String[revenues.length];
        String[] revenueAmounts = new String[revenues.length];
        for (int i=0; i<revenues.length; i++) {
            revenueNames[i] = revenues[i].getId() + " " + revenues[i].getName();
            revenueAmounts[i] = af.formatAmount(revenues[i].getBalance(operationalResult.getDate()));
        }
        
        String totalExpenses = af.formatAmount(operationalResult.getTotalExpenses());
        String totalRevenues = af.formatAmount(operationalResult.getTotalRevenues());
        
        // The component may have been initialized before. Therefore, remove all components.
        panel.removeAll(); 

        // Add label for the date
        int row = 0;
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datePanel.setOpaque(false);
        datePanel.add(new JLabel(tr.getString("gen.date")));

        datePanel.add(new JLabel(tr.formatDate("gen.dateFormat", operationalResult.getDate())));
        
        panel.add(datePanel,
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
