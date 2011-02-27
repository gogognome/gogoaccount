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

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.ui.components.OperationalResultComponent;

/**
 * This class implements a view the shows an operational result.
 *
 * @author Sander Kooijmans
 */
public class OperationalResultView extends View {

    private final static Color BACKGROUND_COLOR = new Color(209, 255, 255);

    private DateSelectionBean dateSelectionBean;

    private Database database;

    private DateModel dateModel;

    public OperationalResultView(Database database) {
        this.database = database;
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#getTitle()
     */
    @Override
	public String getTitle() {
        return TextResource.getInstance().getString("operationalResultView.title");
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
        datePanel.add(new JLabel(tr.getString("operationalResultView.selectDate")),
                SwingUtils.createLabelGBConstraints(0, 0));
        datePanel.setOpaque(false);
        datePanel.setBorder(new EmptyBorder(5, 10, 5, 5));

        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);

        dateSelectionBean = new DateSelectionBean(dateModel);
        datePanel.add(dateSelectionBean,
                SwingUtils.createLabelGBConstraints(1, 0));

        datePanel.add(new JLabel(),
                SwingUtils.createTextFieldGBConstraints(2, 0));
        add(datePanel, BorderLayout.NORTH);

		OperationalResultComponent operationalResultComponent =
		    new OperationalResultComponent(database, dateModel);
        operationalResultComponent.setBackground(BACKGROUND_COLOR);

		add(operationalResultComponent, BorderLayout.CENTER);
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
