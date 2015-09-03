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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.print.Label;
import nl.gogognome.lib.util.StringUtil;

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
    public void printLabel(Graphics2D g2d, double x, double y, double width, double height,
            PageFormat format, int pageIndex) throws PrinterException {
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

        float textX = (float)x + ((float)width - maxWidth) / 2.0f;
        float textY = (float)y + ((float)height - textHeight) / 2.0f + fontHeight;
        g2d.drawString(name, textX, textY);
        g2d.drawString(address, textX, textY + fontHeight);
        g2d.drawString(zipAndCity, textX, textY + 2 * fontHeight);
    }

}
