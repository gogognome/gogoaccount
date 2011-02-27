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
package nl.gogognome.cf.tool;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;

import javax.swing.SwingConstants;

import nl.gogognome.csv.CsvFileParser;
import nl.gogognome.csv.ui.CsvParseSettingsView;
import nl.gogognome.framework.View;
import nl.gogognome.framework.ViewFrame;
import nl.gogognome.framework.ViewListener;

/**
 * This class implements a tool that prints addresses on paper. It prints one address per sheet
 * at a specific location. This tool is useful for example to add addresses to the cover of
 * magazines or to letters.
 *
 * @author Sander Kooijmans
 */
public class AddressPrintTool {

    private ViewFrame frame;
    private CsvParseSettingsView csvParseSettingsView;

    /** Starts the tool. */
    public static void main(String args[]) {
        new AddressPrintTool().start();
    }

    /** Starts the tool. */
    private void start() {
        csvParseSettingsView = new CsvParseSettingsView("gen.continue", "gen.cancel");
        frame = new ViewFrame(csvParseSettingsView);
        csvParseSettingsView.addViewListener(new ViewListener() {
            public void onViewClosed(View view) {
                onCloseCsvParseSettingsView();
            }
        });
        frame.showFrame();
    }

    /** This method is called when the <code>CsvParseSettingsView</code> has been closed. */
    private void onCloseCsvParseSettingsView() {
        if (!("gen.continue".equals(csvParseSettingsView.getIdPressedButton()))) {
            return;
        }

        CsvFileParser parser = csvParseSettingsView.getParser();
        String[] texts;
        try {
            texts = parser.getFormattedValues(csvParseSettingsView.getOutputFormat());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Book book = new Book();
        boolean addExtraPage = true;
        PrintableImpl printable = new PrintableImpl(texts, SwingConstants.RIGHT, SwingConstants.TOP,
            "/var/lib/defoma/fontconfig.d/T/TimesNewRoman-Regular.ttf", 10, addExtraPage);
//        PrintableImpl printable = new PrintableImpl(texts, SwingConstants.CENTER, SwingConstants.BOTTOM,
//            "/usr/share/fonts/truetype/ttf-lucida/LucidaBrightDemiItalic.ttf", 20);
        book.append(printable, new PageFormat(), addExtraPage ? 2*texts.length : texts.length);
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        printerJob.setPageable(book);
        boolean doPrint = printerJob.printDialog();
        if (doPrint) {
            try {
                printerJob.print();
            } catch (PrinterException e) {
                e.printStackTrace();
            }
        }

    }

    private static class PrintableImpl implements Printable {

        /** The texts to be printed. */
        private String[] texts;

        private int horizontalAlignment;
        private int verticalAlignment;
        private String fontName;
        private int fontSize;
        private boolean addExtraPage;

        public PrintableImpl(String[] texts, int horizontalAlignment, int verticalAlignment,
                String fontName, int fontSize, boolean addExtraPage) {
            this.texts = texts;
            this.horizontalAlignment = horizontalAlignment;
            this.verticalAlignment = verticalAlignment;
            this.fontName = fontName;
            this.fontSize = fontSize;
            this.addExtraPage = addExtraPage;
        }

        /**
         * @see java.awt.print.Printable#print(java.awt.Graphics, java.awt.print.PageFormat, int)
         */
        public int print(Graphics g, PageFormat format, int pageIndex) throws PrinterException {
            if (addExtraPage) {
                if (pageIndex / 2 >= texts.length) {
                    return Printable.NO_SUCH_PAGE;
                }
            } else if (pageIndex >= texts.length) {
                return Printable.NO_SUCH_PAGE;
            }

            if (addExtraPage) {
                if (pageIndex % 2 == 0) {
                    // Print empty page
                    return Printable.PAGE_EXISTS;
                } else {
                    pageIndex = pageIndex / 2;
                }
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setClip(null);

            Paper paper = format.getPaper();

            Font font = g.getFont();
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, new File(fontName)).deriveFont((float)fontSize);
            } catch (IOException e) {
                System.out.println("Could not load font \"" + fontName + "\": " + e);
            } catch (FontFormatException e) {
                System.out.println("Could not load font \"" + fontName + "\": " + e);
            }
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            int fontHeight = fm.getHeight();

            // Determine size of bounding rectangle of the text.
            String[] lines = texts[pageIndex].split("\\\\n");
            int height = 0;
            int width = 0;
            for (int i = 0; i < lines.length; i++) {
                width = Math.max(width, fm.stringWidth(lines[i]));
                height += fontHeight;
            }

            // Determine the top-left corner of the bounding rectangle
            int topX;
            int topY;
            switch (horizontalAlignment) {
            case SwingConstants.LEFT:
                topX = (int)paper.getImageableX();
                break;
            case SwingConstants.RIGHT:
                topX = (int)(paper.getImageableX() + paper.getImageableWidth() - width);
                break;
            case SwingConstants.CENTER:
                topX = (int)(paper.getImageableX() + (paper.getImageableWidth() - width) / 2.0);
                break;
            default:
                throw new IllegalArgumentException("Invalid horizontalAlignment: " + horizontalAlignment);
            }

            switch (verticalAlignment) {
            case SwingConstants.TOP:
                topY = (int)paper.getImageableY() / 2;
                break;
            case SwingConstants.BOTTOM:
                topY = (int)(paper.getImageableY() + paper.getImageableHeight() - height);
                break;
            case SwingConstants.CENTER:
                topY = (int)(paper.getImageableY() + (paper.getImageableHeight() - height) / 2.0);
                break;
            default:
                throw new IllegalArgumentException("Invalid verticalAlignment: " + verticalAlignment);
            }

            // for test purposes: draw bounding rectangle
//            g.setColor(Color.RED);
//            g.drawRect(topX, topY, width, height);

            g.setColor(Color.BLACK);
            for (int i=0; i<lines.length; i++) {
                g.drawString(lines[i], topX, topY + (i+1)*fontHeight);
            }

            return Printable.PAGE_EXISTS;
        }

    }
}
