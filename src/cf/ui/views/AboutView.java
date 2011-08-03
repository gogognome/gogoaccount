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
package cf.ui.views;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.views.View;

/**
 * This class shows information about the ClubFinance application.
 *
 * @author Sander Kooijmans
 */
public class AboutView extends View {

    @Override
    public String getTitle() {
        return textResource.getString("aboutView.title");
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onInit() {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new GridBagLayout());

        URL url = ClassLoader.getSystemResource("about.png");
        Image image = Toolkit.getDefaultToolkit().createImage(url);
        add(new JLabel(new ImageIcon(image)), SwingUtils.createLabelGBConstraints(0, 0));

        int n = 1;
        while (n != Integer.MAX_VALUE) {
        	String lineId = "aboutView.line" + n;
        	if (textResource.containsString(lineId)) {
	            String line = textResource.getString(lineId);
                JLabel label = new JLabel(line);
                add(label, SwingUtils.createLabelGBConstraints(0, n));
                n += 1;
        	} else {
        		break;
        	}
        }

        JButton closeButton = widgetFactory.createButton("gen.ok", closeAction);
        add(closeButton, SwingUtils.createGBConstraints(0, n, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
            GridBagConstraints.NONE, 5, 0, 0, 0));
    }

}
