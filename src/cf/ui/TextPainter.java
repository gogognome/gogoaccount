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
package cf.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * This is a utility class for painting texts on a graphics context.
 *
 * @author Sander Kooijmans
 */
public class TextPainter {

	public final static int HEADER_FONT = 0;
	public final static int NORMAL_FONT = 1;
	public final static int TITLE_FONT = 2;

	/** The font definitions. */
	private final static Font[] FONTS = new Font[] {
		new Font("Helvetica", Font.BOLD, 15), // HEADER_FONT
		new Font("Helvetica", Font.PLAIN, 15), // NORMAL_FONT
		new Font("Helvetica", Font.BOLD, 20), // TITLE_FONT
	};

	/**
	 * Gets the specified font.
	 * @param f the index of the font used to paint the text.
	 * @return the font.
	 */
	public static Font getFont( int f ) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}

		return FONTS[f];
	}

	/**
	 * Gets the dimensions of the specified text.
	 *
	 * @param g the graphics context.
	 * @param f the index of the font used to paint the text.
	 * @param text the text.
	 */
	public static Dimension getDimensions( Graphics g, int f, String text ) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}

		FontMetrics fm = g.getFontMetrics(FONTS[f]);
		int height = Math.round( fm.getLineMetrics(text, g).getAscent() );
		int width = fm.stringWidth(text);
		return new Dimension( width, height );
	}

	/**
	 * Paints a text on the specified coordinates.
	 *
	 * @param g the graphics context.
	 * @param f the index of the font used to paint the text.
	 * @param text the text.
	 * @param x the x coordinate of the left-most top of the first letter of the text.
	 * @param y the y coordinate.
	 */
	public static void paintText( Graphics g, int f, String text, int x, int y) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}

		g.setFont(FONTS[f]);
		FontMetrics fm = g.getFontMetrics(FONTS[f]);
		int delta = Math.round( fm.getLineMetrics(text, g).getAscent() );
		g.drawString(text, x, y+delta);
	}

	/**
	 * Paints a text on the specified coordinates within a maximum width.
	 *
	 * @param g the graphics context.
	 * @param f the index of the font used to paint the text.
	 * @param text the text.
	 * @param x the x coordinate of the left-most top of the first letter of the text.
	 * @param y the y coordinate.
	 * @param w the mamimum width of the text. The text will be truncated if its with exceeds
	 *        <tt>w</tt> pixels.
	 */
	public static void paintText( Graphics g, int f, String text, int x, int y, int w) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}

		g.setFont(FONTS[f]);
		FontMetrics fm = g.getFontMetrics(FONTS[f]);
		text = truncateToWidth(text, fm, w);
		int delta = Math.round( fm.getLineMetrics(text, g).getAscent() );
		g.drawString(text, x, y+delta);
	}

	/**
	 * Paints a text right aligned on the specified coordinates within a maximum width.
	 *
	 * @param g the graphics context.
	 * @param f the index of the font used to paint the text.
	 * @param text the text.
	 * @param x the x coordinate of the left-most top of the first letter of the text.
	 * @param y the y coordinate.
	 * @param w the mamimum width of the text. The text will be truncated if its with exceeds
	 *        <tt>w</tt> pixels.
	 */
	public static void paintTextRightAligned( Graphics g, int f, String text, int x, int y, int w) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}

		g.setFont(FONTS[f]);
		FontMetrics fm = g.getFontMetrics(FONTS[f]);
		text = truncateToWidth(text, fm, w);
		int delta = Math.round( fm.getLineMetrics(text, g).getAscent() );
		int xOffset = w - fm.stringWidth(text);
		g.drawString(text, x+xOffset, y+delta);
	}

	/**
	 * Paints a text on the specified coordinates within a maximum width. If needed, the
	 * font size is reduced to make the text fit within the maximum width.
	 *
	 * @param g the graphics context.
	 * @param f the index of the font used to paint the text.
	 * @param text the text.
	 * @param x the x coordinate of the left-most top of the first letter of the text.
	 * @param y the y coordinate.
	 * @param w the mamimum width of the text. Must be a postive number.
	 */
	public static void paintTextDownsized( Graphics g, int f, String text, int x, int y, int w) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}
		if (w <= 0) {
			throw new IllegalArgumentException("Maximum width is not positive.");
		}

		g.setFont(FONTS[f]);
		Font font = FONTS[f];
		FontMetrics fm = g.getFontMetrics(FONTS[f]);
		while (fm.stringWidth(text) > w) {
			font = font.deriveFont((float)(font.getSize()-1));
			g.setFont(font);
			fm = g.getFontMetrics();
		}
		int delta = Math.round( fm.getLineMetrics(text, g).getAscent() );
		g.drawString(text, x, y+delta);
	}

	/**
	 * Paints a text centered on the specified coordinates within a maximum width.
	 * If needed, the font size is reduced to make the text fit within the maximum width.
	 *
	 * @param g the graphics context.
	 * @param f the index of the font used to paint the text.
	 * @param text the text.
	 * @param x the x coordinate of the left-most top of the first letter of the text.
	 * @param y the y coordinate.
	 * @param w the mamimum width of the text. Must be a postive number.
	 */
	public static void paintTextDownsizedCentered( Graphics g, int f, String text,
		int x, int y, int w) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}
		if (w <= 0) {
			throw new IllegalArgumentException("Maximum width is not positive.");
		}

		Font font = FONTS[f];
        g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		while (fm.stringWidth(text) > w) {
			font = font.deriveFont((float)(font.getSize()-1));
			g.setFont(font);
			fm = g.getFontMetrics();
		}
		int delta = Math.round( fm.getLineMetrics(text, g).getAscent() );
		g.drawString(text, x + (w-fm.stringWidth(text))/2, y+delta);
	}

	/**
	 * Paints a text on the specified coordinates.
	 *
	 * @param g the graphics context.
	 * @param f the index of the font used to paint the text.
	 * @param text the text.
	 * @param x the x coordinate of the left-most top of the first letter of the text.
	 * @param y the y coordinate.
	 * @param w the mamimum width of the text. The text will be truncated if its with exceeds
	 *        <tt>w</tt> pixels. The (truncated) text will be painted centered around the
	 *        point <tt>(x + w/2, y)</tt>.
	 */
	public static void paintCenteredText( Graphics g, int f, String text, int x, int y, int w) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}

		FontMetrics fm = g.getFontMetrics(FONTS[f]);
		text = truncateToWidth(text, fm, w);
		int delta = Math.round( fm.getLineMetrics(text, g).getAscent() );
		g.drawString(text, x + (w-fm.stringWidth(text))/2, y+delta);
	}

	/**
	 * Truncates a text so that it fits within a specified width.
	 *
	 * @param text the text.
	 * @param fm the font metrics used to determine the size of the text.
	 * @param w the maximum width.
	 * @return the truncated text. If the text was truncated, then an ellipsis is added to
	 *         the end of the text.
	 */
	private static String truncateToWidth( String text, FontMetrics fm, int w) {
		String result = text;
		if (fm.stringWidth(result) > w) {
			StringBuffer sb = new StringBuffer(text);
			sb.append("...");
			while (sb.length() > 4 && fm.stringWidth(sb.toString()) > w) {
				sb.deleteCharAt(sb.length()-4); // delete last character in front of ellipsis
			}

			// remove white spaces in front of ellipsis
			while (sb.length() > 3 && Character.isWhitespace(sb.charAt(sb.length()-4))) {
				sb.deleteCharAt(sb.length()-4); // delete white space in front of ellipsis
			}

			result = sb.toString();
		}

		return result;
	}

	/**
	 * Paints a rotated text on the specified coordinates.
	 *
	 * @param g the graphics context.
	 * @param f the index of the font used to paint the text.
	 * @param text the text.
	 * @param x the x coordinate of the left-most top of the first letter of the text.
	 * @param y the y coordinate.
	 * @param angle the angle over which to rotate the text. The rotation axis is
	 *        <tt>(x,y)</tt>.
	 */
	public static void paintRotatedText( Graphics g, int f, String text, int x, int y, double angle) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}

		FontMetrics fm = g.getFontMetrics(FONTS[f]);
		int delta = Math.round( fm.getLineMetrics(text, g).getAscent() );
		y += delta;

		Graphics2D g2d = (Graphics2D)g;
		AffineTransform oldTransform = g2d.getTransform();
		AffineTransform at = AffineTransform.getTranslateInstance(x, y);

		at.rotate(Math.toRadians(angle));
		at.translate(-x, -y);
		g2d.setTransform(at);

		g2d.drawString(text, x, y+delta);

		g2d.setTransform(oldTransform);
	}

	/**
	 * Paints a rotated text on the specified coordinates.
	 *
	 * @param g the graphics context.
	 * @param f the index of the font used to paint the text.
	 * @param text the text.
	 * @param x the x coordinate of the left-most top of the first letter of the text.
	 * @param y the y coordinate.
	 * @param angle the angle over which to rotate the text. The rotation axis is
	 *        <tt>(x,y)</tt>.
	 * @param w the mamimum width of the text. The text will be truncated if its with exceeds
	 *        <tt>w</tt> pixels.
	 */
	public static void paintRotatedText( Graphics g, int f, String text, int x, int y,
		double angle, int w) {
		if (f < 0 || f >= FONTS.length) {
			throw new IllegalArgumentException("Font index " + f + " is invalid.");
		}

		FontMetrics fm = g.getFontMetrics(FONTS[f]);
		text = truncateToWidth(text, fm, w);
		paintRotatedText(g, f, text, x, y, angle);
	}
}
