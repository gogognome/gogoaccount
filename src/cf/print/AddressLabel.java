/*
 * $Id: AddressLabel.java,v 1.1 2007-07-26 19:37:16 sanderk Exp $
 *
 * Copyright (C) 2005 Sander Kooijmans
 *
 */

package cf.print;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;

import nl.gogognome.print.Label;
import nl.gogognome.util.StringUtil;
import cf.engine.Party;

/**
 * This class implements an address label. The address is taken from a <code>Party</code>.
 */
public class AddressLabel implements Label {

    private Party party;
    
    public AddressLabel(Party party) {
        this.party = party;
    }
    
    /**
     * Prints the address of the party in the graphics context.
     * 
     * @see Label#printLabel(Graphics2D, double, double, double, double, PageFormat, int)
     */
    public void printLabel(Graphics2D g, double x, double y, double width, double height, 
            PageFormat format, int pageIndex) throws PrinterException {
        Graphics2D g2d = (Graphics2D)g;
        String name = StringUtil.nullToEmptyString(party.getName());
        String address = StringUtil.nullToEmptyString(party.getAddress());
        String zipAndCity = party.getZipCode();
        if (zipAndCity == null) {
            zipAndCity = "";
        } else {
            zipAndCity += "  ";
        }
        zipAndCity += StringUtil.nullToEmptyString(party.getCity()).toUpperCase();
        
        Font font = new Font("Serif", Font.PLAIN, 10);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int fontHeight = fm.getHeight();
        
        int maxWidth = Math.max(fm.stringWidth(name), fm.stringWidth(address));
        maxWidth = Math.max(maxWidth, fm.stringWidth(zipAndCity));
        int textHeight = 3 * fontHeight;
        
        float textX = (float)x + ((float)width - (float)maxWidth) / 2.0f;
        float textY = (float)y + ((float)height - (float)textHeight) / 2.0f + fontHeight;
        g2d.drawString(name, textX, textY);
        g2d.drawString(address, textX, textY + fontHeight);
        g2d.drawString(zipAndCity, textX, textY + 2 * fontHeight);
    }

}
