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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import nl.gogognome.lib.gui.beans.DateSelectionBean;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Database;
import cf.ui.components.BalanceComponent;

/**
 * This class implements a view the shows a balance.
 *
 * @author Sander Kooijmans
 */
public class BalanceView extends View {

    private final static Color BACKGROUND_COLOR = new Color(255, 255, 209);

    private DateSelectionBean dateSelectionBean;

    private Database database;

    private DateModel dateModel;

    public BalanceView(Database database) {
        this.database = database;
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#getTitle()
     */
    @Override
	public String getTitle() {
        return TextResource.getInstance().getString("balanceView.title");
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onInit()
     */
    @Override
	public void onInit() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);

        TextResource tr = TextResource.getInstance();

        JPanel datePanel = new JPanel(new GridBagLayout());
        datePanel.setOpaque(false);
        datePanel.setBorder(new EmptyBorder(5, 10, 5, 5));
        datePanel.add(new JLabel(tr.getString("balanceView.selectDate")),
                SwingUtils.createLabelGBConstraints(0, 0));

        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);

        dateSelectionBean = new DateSelectionBean(dateModel);
        datePanel.add(dateSelectionBean,
                SwingUtils.createLabelGBConstraints(1, 0));

        datePanel.add(new JLabel(),
                SwingUtils.createTextFieldGBConstraints(2, 0));
        add(datePanel, BorderLayout.NORTH);

		BalanceComponent balanceComponent = new BalanceComponent(database, dateModel);
        balanceComponent.setBackground(BACKGROUND_COLOR);

		add(balanceComponent, BorderLayout.CENTER);
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onClose()
     */
    @Override
	public void onClose() {
        dateSelectionBean = null;
        database = null;
    }
}
