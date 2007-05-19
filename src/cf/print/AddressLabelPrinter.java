/*
 * $Id: AddressLabelPrinter.java,v 1.1 2007-05-19 17:34:07 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package cf.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;

import nl.gogognome.util.StringUtil;
import cf.engine.Party;

/**
 * This class prints address labels.
 *
 * @author Sander Kooijmans
 */
public class AddressLabelPrinter implements Printable {

    /** The parties to be printed. */
    Party[] parties;
    
    private final static int ROWS_PER_PAGE = 8;
    private final static int COLUMNS_PER_PAGE = 3;
    private final static int PARTIES_PER_PAGE = ROWS_PER_PAGE * COLUMNS_PER_PAGE;
    
    public AddressLabelPrinter(Party[] parties) {
        this.parties = parties;
    }
    
    public void printAddressLabels() throws PrinterException {
    	PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
		pras.add(MediaSizeName.ISO_A4);
		int x = 5; //left and right margin
		int y = 10; //top and bottom margin. Note that bottom margin cannot be less than 15 mm
		int w = 200; //Width
		int h = 272; //Height
		int units = MediaPrintableArea.MM;
		pras.add(new MediaPrintableArea(x, y, w, h, units));
		
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        
        Book book = new Book();
        book.append(this, new PageFormat(), (parties.length + PARTIES_PER_PAGE - 1) / PARTIES_PER_PAGE);
        printerJob.setPageable(book);
        boolean doPrint = printerJob.printDialog(pras);
        if (doPrint) {
            printerJob.print(pras);
        }
    }

    /* (non-Javadoc)
     * @see java.awt.print.Printable#print(java.awt.Graphics, java.awt.print.PageFormat, int)
     */
    public int print(Graphics g, PageFormat format, int pageIndex) throws PrinterException {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setPaint(Color.black);
        g2d.setClip(null);
        
        Paper paper = format.getPaper();
        double paperWidth = paper.getWidth();
        double paperHeight = paper.getHeight();
        Font font = new Font("Serif", Font.PLAIN, 10);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        
        int partyIndex = pageIndex * PARTIES_PER_PAGE;
        double labelWidth = paperWidth / COLUMNS_PER_PAGE;
        double labelHeight = paperHeight / ROWS_PER_PAGE;
        int fontHeight = fm.getHeight();
        int y = 0;
        int x = 0;
        for (int i=partyIndex; i < parties.length && i<partyIndex + PARTIES_PER_PAGE; i++) {
            double labelX = x * labelWidth;
            double labelY = y * labelHeight;
            g2d.drawRect((int)labelX, (int)labelY, (int)labelWidth, (int)labelHeight);
            
            Party party = parties[i];
            String name = StringUtil.nullToEmptyString(party.getName());
            String address = StringUtil.nullToEmptyString(party.getAddress());
            String zipAndCity = party.getZipCode();
            if (zipAndCity == null) {
                zipAndCity = "";
            } else {
                zipAndCity += "  ";
            }
            zipAndCity += StringUtil.nullToEmptyString(party.getCity()).toUpperCase();
            
            int maxWidth = Math.max(fm.stringWidth(name), fm.stringWidth(address));
            maxWidth = Math.max(maxWidth, fm.stringWidth(zipAndCity));
            int height = 3 * fontHeight;
            
            float textX = (float)labelX + ((float)labelWidth - (float)maxWidth) / 2.0f;
            float textY = (float)labelY + ((float)labelHeight - (float)height) / 2.0f + fontHeight;
            g2d.drawString(name, textX, textY);
            g2d.drawString(address, textX, textY + fontHeight);
            g2d.drawString(zipAndCity, textX, textY + 2 * fontHeight);
            
            x++;
            if (x == COLUMNS_PER_PAGE) {
                x = 0;
                y++;
            }
        }
        return Printable.PAGE_EXISTS;
    }
}