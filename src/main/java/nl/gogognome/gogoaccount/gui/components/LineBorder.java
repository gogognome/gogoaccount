package nl.gogognome.gogoaccount.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.Border;

/**
 * This class implements a border that consists of a line. For each of the four
 * sides of the border it can be determined whether the line must be drawn or not.
 * Also the width of the line is configurable.
 *
 * @author Sander Kooijmans
 */
public class LineBorder implements Border {

    public final static int LB_TOP = 1;
    public final static int LB_LEFT = 1 << 1;
    public final static int LB_BOTTOM = 1 << 2;
    public final static int LB_RIGHT = 1 << 3;

    /** Indicates which sides are to be drawn. */
    private int sides;

    /** The width of the lines. */
    private int lineWidth;

    /**
     * Constructor.
     *
     * @param sides the sides of the border (bitwise-or of one or more
     *         of <code>LB_TOP</code>, <code>LB_LEFTT</code>, <code>LB_BOTTOM</code> and
     *         <code>LB_RIGHT</code>.
     * @param lineWidth the width of the border lines in pixels
     */
    public LineBorder(int sides, int lineWidth) {
        this.sides = sides;
        this.lineWidth = lineWidth;
    }

    /* (non-Javadoc)
     * @see javax.swing.border.Border#isBorderOpaque()
     */
    public boolean isBorderOpaque() {
        return false;
    }

    /* (non-Javadoc)
     * @see javax.swing.border.Border#paintBorder(java.awt.Component, java.awt.Graphics, int, int, int, int)
     */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.BLACK);
        if ((sides & LB_TOP) != 0) {
            g.fillRect(x, y, width, lineWidth);
        }
        if ((sides & LB_LEFT) != 0) {
            g.fillRect(x, y, lineWidth, height);
        }
        if ((sides & LB_BOTTOM) != 0) {
            g.fillRect(x, y + height - lineWidth, width, lineWidth);
        }
        if ((sides & LB_RIGHT) != 0) {
            g.fillRect(x + width - lineWidth, y, lineWidth, height);
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.border.Border#getBorderInsets(java.awt.Component)
     */
    public Insets getBorderInsets(Component c) {
        int top = (sides & LB_TOP) != 0 ? lineWidth : 0;
        int left = (sides & LB_LEFT) != 0 ? lineWidth : 0;
        int bottom = (sides & LB_BOTTOM) != 0 ? lineWidth : 0;
        int right = (sides & LB_RIGHT) != 0 ? lineWidth : 0;

        return new Insets(top, left, bottom, right);
    }

}
