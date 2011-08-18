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
package nl.gogognome.gogoaccount.services;

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

import nl.gogognome.gogoaccount.businessobjects.Party;
import nl.gogognome.lib.util.StringUtil;

/**
 * This class prints address labels. Currently only A4-sized sheets with 3x8 labels
 * are supported.
 *
 * <p>The addresses are taken from an array of parties.
 *
 * @author Sander Kooijmans
 */
public class AddressLabelPrinter implements Printable {

    /** The parties whose addresses are to be printed. */
    private Party[] parties;

    /**
     * Constructor.
     * @param parties the parties whose addresses are to be printed
     */
    public AddressLabelPrinter(Party[] parties) {
        this.parties = parties;
    }

    /**
     * Prints the labels.
     * @throws PrinterException if a problem occurs while printing
     */
    public void printAddressLabels() throws PrinterException {
    	PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
		pras.add(MediaSizeName.ISO_A4);
		pras.add(new MediaPrintableArea(5, 10, 210 - 2*5, 297 - 2*10, MediaPrintableArea.MM));

        PrinterJob printerJob = PrinterJob.getPrinterJob();

        Book book = new Book();
        book.append(this, new PageFormat(), (parties.length + getNrPartiesPerPage() - 1) / getNrPartiesPerPage());
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
        // It seems that under Linux always the size of Letter is used instead of A4.
        // This is a workaround:
        paperWidth = (210.0 / 25.4) * 72.0;
        paperHeight = (297.0 / 25.4) * 72.0;

        Font font = new Font("Serif", Font.PLAIN, 10);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        int partyIndex = pageIndex * getNrPartiesPerPage();
        double labelWidth = paperWidth / getNrColumnsPerPage();
        double labelHeight = paperHeight / (getNrRowsPerPage() + (isTypeOfPaperOverruled() ? 1 : 0));
        int fontHeight = fm.getHeight();
        int y = 0;
        int x = 0;
        for (int i=partyIndex; i < parties.length && i<partyIndex + getNrPartiesPerPage(); i++) {
            double labelX = labelWidth * x;
            double labelY = labelHeight* y;
//            g2d.drawRect((int)labelX, (int)labelY, (int)labelWidth, (int)labelHeight);

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

            float textX = (float)labelX + ((float)labelWidth - maxWidth) / 2.0f;
            float textY = (float)labelY + ((float)labelHeight - height) / 2.0f + fontHeight;
            g2d.drawString(name, textX, textY);
            g2d.drawString(address, textX, textY + fontHeight);
            g2d.drawString(zipAndCity, textX, textY + 2 * fontHeight);

            x++;
            if (x == getNrColumnsPerPage()) {
                x = 0;
                y++;
            }
        }
        return Printable.PAGE_EXISTS;
    }

    public int getNrColumnsPerPage() {
        return 3;
    }

    public int getNrRowsPerPage() {
        if (isTypeOfPaperOverruled()) {
            return 7;
        }
        return 8;
    }

    public int getNrPartiesPerPage() {
        return getNrColumnsPerPage() * getNrRowsPerPage();
    }

    private boolean isTypeOfPaperOverruled() {
        return "A4".equals(System.getProperty("forcePaper"));
    }
}