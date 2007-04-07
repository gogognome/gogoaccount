/*
 * $Id: BalanceComponent.java,v 1.10 2007-04-07 17:28:42 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

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
public class BalanceComponent extends JComponent
{
    /** The balance shown in the component. */
    private Balance balance;

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
    
    private String[] assetNames;
    private String[] assetAmounts;
    private String[] liabilityNames;
    private String[] liabilityAmounts;
    private String totalAssets;
    private String totalLiabilities;
    
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
                repaint();
            }
        });
        
        dateModel.addModelChangeListener(new ModelChangeListener() {
            public void modelChanged(AbstractModel model) {
                initializeValues();
                repaint();
            }
        });
        
        setLayout(new GridBagLayout());
        initializeValues();
    }

    private void initializeValues() {
        Date date = dateModel.getDate();
        if (date == null) {
            return; // do not change the current balance if the date is invalid
        }
        
        balance = new Balance(database, date);
        TextResource tr = TextResource.getInstance();
        AmountFormat af = tr.getAmountFormat();
        Account[] assets = balance.getAssets();
        Account[] liabilities = balance.getLiabilities();

        assetNames = new String[assets.length];
        assetAmounts = new String[assets.length];
        for (int i=0; i<assets.length; i++) 
        {
            assetNames[i] = assets[i].getId() + " " + assets[i].getName();
            assetAmounts[i] = af.formatAmount(assets[i].getBalance(balance.getDate()));
        }
        
        liabilityNames = new String[liabilities.length];
        liabilityAmounts = new String[liabilities.length];
        for (int i=0; i<liabilities.length; i++) 
        {
            liabilityNames[i] = liabilities[i].getId() + " " + liabilities[i].getName();
            liabilityAmounts[i] = af.formatAmount(liabilities[i].getBalance(balance.getDate()));
        }
        
        totalAssets = af.formatAmount(balance.getTotalAssets());
        totalLiabilities = af.formatAmount(balance.getTotalLiabilities());
        
        // The component may have been initialized before. Therefore, remove all components.
        removeAll(); 

        // Add label for the date
        int row = 0;
        add(new JLabel(tr.formatDate("gen.dateFormat", balance.getDate())),
                SwingUtils.createLabelGBConstraints(0, row));
        row++;
        
        // Add labels for the table header
        Border bottomBorder = new LineBorder(LineBorder.LB_BOTTOM, 3);
        JLabel label = new JLabel(tr.getString("gen.assets"));
        label.setBorder(bottomBorder);
        add(label,
                SwingUtils.createGBConstraints(0, row, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        10, 0, 0, 0));
        
        label = new JLabel();
        label.setBorder(bottomBorder);
        add(label,
                SwingUtils.createGBConstraints(1, row, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        10, 0, 0, 0));
        
        label = new JLabel();
        label.setBorder(bottomBorder);
        add(label,
                SwingUtils.createGBConstraints(2, row, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        10, 0, 0, 0));
        
        label = new JLabel(tr.getString("gen.liabilities"), SwingConstants.RIGHT); 
        label.setBorder(bottomBorder);
        add(label, 
                SwingUtils.createGBConstraints(3, row, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        10, 0, 0, 0));
        row++;
        
        // Add the table rows
        int firstRow = row;
        for (int i=0; i<assetNames.length; i++) {
            add(new JLabel(assetNames[i]), 
                    SwingUtils.createGBConstraints(0, row, 1, 1, 1.0, 1.0, 
                            GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                            0, 0, 0, 0));
            add(new JLabel(assetAmounts[i], SwingConstants.RIGHT), 
                    SwingUtils.createGBConstraints(1, row, 1, 1, 1.0, 1.0, 
                            GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                            0, 0, 0, 5));
            row++;
        }
        
        int row2 = firstRow;
        for (int i=0; i<liabilityNames.length; i++)
        {
            add(new JLabel(liabilityNames[i]), 
                    SwingUtils.createGBConstraints(2, row2, 1, 1, 1.0, 1.0, 
                            GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                            0, 5, 0, 0));
            add(new JLabel(liabilityAmounts[i], SwingConstants.RIGHT), 
                    SwingUtils.createGBConstraints(3, row2, 1, 1, 1.0, 1.0, 
                            GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                            0, 0, 0, 0));
            row2++;
        }

        row = Math.max(row, row2);
        
        Border topBorder = new LineBorder(LineBorder.LB_TOP, 1);
        label = new JLabel(tr.getString("gen.total"));
        label.setBorder(topBorder);
        add(label, 
                SwingUtils.createGBConstraints(0, row, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        0, 0, 10, 0));

        label = new JLabel(totalAssets, SwingConstants.RIGHT);
        label.setBorder(topBorder);
        add(label, 
                SwingUtils.createGBConstraints(1, row, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        0, 0, 10, 5));

        label = new JLabel(tr.getString("gen.total"));
        label.setBorder(topBorder);
        add(label, 
                SwingUtils.createGBConstraints(2, row, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        0, 5, 10, 0));

        label = new JLabel(totalLiabilities, SwingConstants.RIGHT);
        label.setBorder(topBorder);
        add(label, 
                SwingUtils.createGBConstraints(3, row, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        0, 0, 10, 0));
        row++;
        
        // Add label to push other labels to the left and top.
        add(new JLabel(""),
                SwingUtils.createGBConstraints(5, row, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        0, 0, 0, 0));
    }
}
