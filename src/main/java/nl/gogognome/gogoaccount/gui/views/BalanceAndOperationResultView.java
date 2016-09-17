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
package nl.gogognome.gogoaccount.gui.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.gui.components.BalanceComponent;
import nl.gogognome.gogoaccount.gui.components.OperationalResultComponent;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsRow;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.views.View;

/**
 * This class implements a view the shows a balance and operation result.
 */
public class BalanceAndOperationResultView extends View {

	private static final long serialVersionUID = 1L;

	private final static Color BACKGROUND_COLOR = new Color(255, 255, 209);

    private final Document document;
    private final BookkeepingService bookkeepingService;
    private DateModel dateModel;

    public BalanceAndOperationResultView(Document document, BookkeepingService bookkeepingService) {
        this.document = document;
        this.bookkeepingService = bookkeepingService;
    }

    @Override
	public String getTitle() {
        return textResource.getString("balanceAndOperationalResultView.title");
    }

    @Override
	public void onInit() {
        try {
            initModels();
            addComponents();
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            close();
        }
    }

    @Override
    public void onClose() {
    }

	private void initModels() {
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
	}

	private void addComponents() throws ServiceException {
		setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel northPanel = createInputFieldsPanel();
		northPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));

        add(northPanel, BorderLayout.NORTH);
        add(createBalanceAndOperationalResultPanel(), BorderLayout.CENTER);
	}

    private JPanel createInputFieldsPanel() {
		InputFieldsRow row = new InputFieldsRow();
		addCloseable(row);
		row.addField("balanceAndOperationalResultView.selectDate", dateModel);
		return row;
	}

    private JPanel createBalanceAndOperationalResultPanel() throws ServiceException {
        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBackground(BACKGROUND_COLOR);
        BalanceComponent balanceComponent = new BalanceComponent(document, bookkeepingService, dateModel);
        addCloseable(balanceComponent);
        balanceComponent.setBackground(BACKGROUND_COLOR);
        panel.add(balanceComponent, createConstraints(0, 0));

        OperationalResultComponent operationalResultComponent =
            new OperationalResultComponent(document, bookkeepingService, dateModel);
        addCloseable(operationalResultComponent);
        operationalResultComponent.setBackground(BACKGROUND_COLOR);
        panel.add(operationalResultComponent, createConstraints(0, 1));

        return panel;
    }

    public static GridBagConstraints createConstraints( int gridx, int gridy) {
    	return SwingUtils.createGBConstraints(gridx, gridy, 1, 1, 1.0, 1.0,
    			GridBagConstraints.WEST, GridBagConstraints.BOTH,
    			gridy == 0 ? 0 : 10, 0, 0, 0);
    }
}
