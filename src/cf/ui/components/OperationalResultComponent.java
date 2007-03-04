/*
 * $Id: OperationalResultComponent.java,v 1.5 2007-02-10 16:28:46 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.components;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.text.SimpleDateFormat;

import javax.swing.JComponent;

import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;

import cf.engine.Account;
import cf.engine.Database;
import cf.engine.OperationalResult;
import cf.ui.TextPainter;

/**
 * This class implements a graphical component that shows an operational result.
 *
 * @author Sander Kooijmans
 */
public class OperationalResultComponent extends JComponent
{
    /** The balance shown in the component. */
    private OperationalResult operationalResult;

    private String[] expenseNames;
    private String[] expenseAmounts;
    private String[] revenueNames;
    private String[] revenueAmounts;
    private String totalRevenues;
    private String totalExpenses;
    
    private final static int WIDTH = 800;
    
    /**
     * Creates a new <code>OperationalResultComponent</code>.  
     */
    public OperationalResultComponent(OperationalResult operationalResult) 
    {
        super();
        this.operationalResult = operationalResult;
        
        initializeValues();
    }

    private void initializeValues()
    {
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        Database database = Database.getInstance();
        Account[] expenses = database.getExpenses();
        Account[] revenues = database.getRevenues();

        expenseNames = new String[expenses.length];
        expenseAmounts = new String[expenses.length];
        for (int i=0; i<expenses.length; i++) 
        {
            expenseNames[i] = expenses[i].getId() + " " + expenses[i].getName();
            expenseAmounts[i] = af.formatAmount(expenses[i].getBalance(operationalResult.getDate()));
        }
        
        revenueNames = new String[revenues.length];
        revenueAmounts = new String[revenues.length];
        for (int i=0; i<revenues.length; i++) 
        {
            revenueNames[i] = revenues[i].getId() + " " + revenues[i].getName();
            revenueAmounts[i] = af.formatAmount(revenues[i].getBalance(operationalResult.getDate()));
        }
        
        totalExpenses = af.formatAmount(operationalResult.getTotalExpenses());
        totalRevenues = af.formatAmount(operationalResult.getTotalRevenues());
    }
    
    public Dimension getPreferredSize()
    {
        return new Dimension(WIDTH + 20, 
                30 * (2 + Math.max(expenseAmounts.length, revenueAmounts.length)));
    }
    
    public void paint(Graphics g)
    {
        TextResource tr = TextResource.getInstance();
        FontMetrics fm = g.getFontMetrics(TextPainter.getFont(TextPainter.NORMAL_FONT));
        
        int x = 10;
        int y = 10;
        
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                tr.getString("gen.dateFormat"));
        TextPainter.paintText(g, TextPainter.NORMAL_FONT, 
                dateFormat.format(operationalResult.getDate()), x, y);
     
        y += fm.getHeight();
        
        TextPainter.paintText(g, TextPainter.NORMAL_FONT, 
                tr.getString("gen.expenses"), x, y);
        TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, 
                tr.getString("gen.revenues"), x, y, WIDTH);
        
        y += fm.getHeight();
        
        g.fillRect(x, y, WIDTH, 3);
        int lineY = y;
        
        y += 5;
        
        for (int i=0; i<expenseNames.length; i++)
        {
            TextPainter.paintText(g, TextPainter.NORMAL_FONT, expenseNames[i], x, y);
            TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, expenseAmounts[i], x, y, WIDTH / 2 - 10);
            y += fm.getHeight();
        }
        
        int y2 = lineY + 5;
        int x2 = x + WIDTH / 2 + 10; 
        for (int i=0; i<revenueNames.length; i++)
        {
            TextPainter.paintText(g, TextPainter.NORMAL_FONT, revenueNames[i], x2, y2);
            TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, revenueAmounts[i], x2, y2, WIDTH / 2 - 10);
            y2 += fm.getHeight();
        }
        
        y = Math.max(y, y2);
        
        y += 2;
        g.drawLine(x, y, x + WIDTH, y);
        y += 2;
        
        TextPainter.paintText(g, TextPainter.NORMAL_FONT, 
                tr.getString("gen.total"), x, y);
        TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, 
                totalExpenses, x, y, WIDTH / 2 - 10);
        TextPainter.paintText(g, TextPainter.NORMAL_FONT, 
                tr.getString("gen.total"), x2, y);
        TextPainter.paintTextRightAligned(g, TextPainter.NORMAL_FONT, 
                totalRevenues, x2, y, WIDTH / 2 - 10);
        
        y += fm.getHeight();

        g.fillRect(x + WIDTH / 2 - 1, lineY, 3, y - lineY);
    }
}
