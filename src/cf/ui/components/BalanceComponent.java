/*
 * $Id: BalanceComponent.java,v 1.15 2007-07-29 12:33:40 sanderk Exp $
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
import cf.engine.Balance;
import cf.engine.Database;
import cf.engine.DatabaseListener;

/**
 * This class implements a graphical component that shows a balance.
 *
 * @author Sander Kooijmans
 */
public class BalanceComponent extends JScrollPane {
    
    /** The panel that contains the components of the <code>BalanceComponent</code>. */
    private JPanel panel;

    private JPanel tempPanel;
    
    /** 
     * The database used to create the balance. Changes in this database will
     * lead to updates on this component.
     */
    private Database database;
    
    /** 
     * The date model used to create the balance. Changes in this model will
     * lead to updates on this component.
     */
    private DateModel dateModel;
    
    /**
     * Creates a new <code>BalanceComponent</code>. 
     * @param database the datebase used to create the balance
     * @param dateModel the date model used to determine the date of the balance 
     */
    public BalanceComponent(Database database, DateModel dateModel) {
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

    /** Initializes the components based on the values of the models. */ 
    private void initializeValues() {
        Date date = dateModel.getDate();
        if (date == null) {
            return; // do not change the current balance if the date is invalid
        }
        
        Balance balance = database.getBalance(date);
        TextResource tr = TextResource.getInstance();
        AmountFormat af = tr.getAmountFormat();
        Account[] assets = balance.getAssets();
        Account[] liabilities = balance.getLiabilities();

        String[] assetNames = new String[assets.length];
        String[] assetAmounts = new String[assets.length];
        for (int i=0; i<assets.length; i++) {
            assetNames[i] = assets[i].getId() + " " + assets[i].getName();
            assetAmounts[i] = af.formatAmount(assets[i].getBalance(balance.getDate()));
        }
        
        String[] liabilityNames = new String[liabilities.length];
        String[] liabilityAmounts = new String[liabilities.length];
        for (int i=0; i<liabilities.length; i++) {
            liabilityNames[i] = liabilities[i].getId() + " " + liabilities[i].getName();
            liabilityAmounts[i] = af.formatAmount(liabilities[i].getBalance(balance.getDate()));
        }
        
        String totalAssets = af.formatAmount(balance.getTotalAssets());
        String totalLiabilities = af.formatAmount(balance.getTotalLiabilities());
        
        // The component may have been initialized before. Therefore, remove all components.
        panel.removeAll(); 

        // Add label for the date
        int row = 0;
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datePanel.setOpaque(false);
        datePanel.add(new JLabel(tr.getString("gen.date")));

        datePanel.add(new JLabel(tr.formatDate("gen.dateFormat", balance.getDate())));
        
        panel.add(datePanel,
                SwingUtils.createGBConstraints(0, row, 4, 1, 1.0, 0.0, 
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        10, 0, 10, 0));
        row++;
        
        // Add labels for the table header
        Border bottomBorder = new LineBorder(LineBorder.LB_BOTTOM, 3);
        JLabel label = new JLabel(tr.getString("gen.assets"));
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
        
        label = new JLabel(tr.getString("gen.liabilities"), SwingConstants.RIGHT); 
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
        for (int i=0; i<assetNames.length; i++) {
            panel.add(new JLabel(assetNames[i]), 
                    SwingUtils.createGBConstraints(0, row, 1, 1, 1.0, 1.0, 
                            GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                            0, 0, 0, 20));
            label = new JLabel(assetAmounts[i], SwingConstants.RIGHT);
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
        for (int i=0; i<liabilityNames.length; i++)
        {
            label = new JLabel(liabilityNames[i]);
            label.setBorder(leftBorder);
            panel.add(label, 
                    SwingUtils.createGBConstraints(2, row2, 1, 1, 1.0, 1.0, 
                            GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                            0, 0, 0, 20));
            panel.add(new JLabel(liabilityAmounts[i], SwingConstants.RIGHT), 
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

        label = new JLabel(totalAssets, SwingConstants.RIGHT);
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

        label = new JLabel(totalLiabilities, SwingConstants.RIGHT);
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
