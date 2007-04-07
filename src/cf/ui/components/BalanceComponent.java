/*
 * $Id: BalanceComponent.java,v 1.9 2007-04-07 15:27:25 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JComponent;

import nl.gogognome.framework.models.AbstractModel;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.framework.models.ModelChangeListener;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import cf.engine.Account;
import cf.engine.Balance;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.ui.TextPainter;

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
    private String totalLiabilitiesAndResult;
    
    private final static int WIDTH = 800;
    
    /**
     * Creates a new <code>BalanceComponent</code>. 
     * @param database the datebase used to create the balance
     * @param dateModel the date model used to determine the date of the balance 
     */
    public BalanceComponent(Database database, DateModel dateModel) 
    {
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
        totalLiabilitiesAndResult = af.formatAmount(balance.getTotalLiabilities());
    }
    
    public Dimension getPreferredSize()
    {
        return new Dimension(WIDTH + 20, 
                30 * (4 + Math.max(assetAmounts.length, liabilityAmounts.length)));
    }
    
    public void paint(Graphics g)
    {
        TextResource tr = TextResource.getInstance();
        FontMetrics fm = g.getFontMetrics(TextPainter.getFont(TextPainter.NORMAL_FONT));
        
        int x = 10;
        int y = 10;
        
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                TextResource.getInstance().getString("gen.dateFormat"));
        TextPainter.paintText(g, TextPainter.NORMAL_FONT, 
                dateFormat.format(balance.getDate()), x, y);
     
        y += fm.getHeight();
        
        TextPainter.paintText(g, TextPainter.NORMAL_FONT, 
                tr.getString("gen.assets"), x, y);
        TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, 
                tr.getString("gen.liabilities"), x, y, WIDTH);
        
        y += fm.getHeight();
        
        g.fillRect(x, y, WIDTH, 3);
        int lineY = y;
        
        y += 5;
        
        for (int i=0; i<assetNames.length; i++)
        {
            TextPainter.paintText(g, TextPainter.NORMAL_FONT, assetNames[i], x, y);
            TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, assetAmounts[i], x, y, WIDTH / 2 - 10);
            y += fm.getHeight();
        }
        
        int y2 = lineY + 5;
        int x2 = x + WIDTH / 2 + 10; 
        for (int i=0; i<liabilityNames.length; i++)
        {
            TextPainter.paintText(g, TextPainter.NORMAL_FONT, liabilityNames[i], x2, y2);
            TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, liabilityAmounts[i], x2, y2, WIDTH / 2 - 10);
            y2 += fm.getHeight();
        }
        
        y = Math.max(y, y2);
        
        y += 2;
        g.drawLine(x, y, x + WIDTH, y);
        y += 2;
        
        TextPainter.paintText(g, TextPainter.NORMAL_FONT, 
                tr.getString("gen.total"), x, y);
        TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, 
                totalAssets, x, y, WIDTH / 2 - 10);
        TextPainter.paintText(g, TextPainter.NORMAL_FONT, 
                tr.getString("gen.total"), x2, y);
        TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, 
                totalLiabilitiesAndResult, x2, y, WIDTH / 2 - 10);
        
        y += fm.getHeight();

        g.fillRect(x + WIDTH / 2 - 1, lineY, 3, y - lineY);
    }
}
