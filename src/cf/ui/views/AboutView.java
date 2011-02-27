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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

import nl.gogognome.framework.View;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;

/**
 * This class shows information about the ClubFinance application.
 *
 * @author Sander Kooijmans
 */
public class AboutView extends View {

    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("aboutView.title");
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onInit() {
        TextResource tr = TextResource.getInstance();
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new GridBagLayout());
        int n = 1;
        while (n != Integer.MAX_VALUE) {
            String line = tr.getString("aboutView.line" + n);
            if (line == null) {
                break;
            } else {
                JLabel label = new JLabel(line);
                if (n == 1) {
                    // Give first line a bigger font.
                    label.setFont(label.getFont().deriveFont(Font.BOLD, 20.0f));
                }
                add(label, SwingUtils.createLabelGBConstraints(0, n));
                n += 1;
            }
        }

        JButton closeButton = WidgetFactory.getInstance().createButton("gen.ok", closeAction);
        add(closeButton, SwingUtils.createGBConstraints(0, n, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
            GridBagConstraints.NONE, 5, 0, 0, 0));
    }

}
