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
package nl.gogognome.gogoaccount.gui.components;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.components.document.DocumentListener;
import nl.gogognome.gogoaccount.gui.components.BalanceSheet.Row;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.Closeable;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

/**
 * This class implements a graphical component that shows a balance.
 *
 * @author Sander Kooijmans
 */
public class BalanceComponent extends JScrollPane implements Closeable {

	private static final long serialVersionUID = 1L;

    private final Document document;
    private final DateModel dateModel;
    private final BalanceSheet balanceSheet;

    private Report report;

    private DocumentListener documentListener;
    private ModelChangeListener modelChangeListener;

    private final TextResource textResource = Factory.getInstance(TextResource.class);

    /**
     * Creates a new <code>BalanceComponent</code>.
     * @param document the datebase used to create the balance
     * @param dateModel the date model used to determine the date of the balance
     */
    public BalanceComponent(Document document, DateModel dateModel) throws ServiceException {
        super();
        this.document = document;
        this.dateModel = dateModel;

        Bookkeeping bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
        balanceSheet = new BalanceSheet(textResource.getString("gen.assets"),
        		textResource.getString("gen.liabilities"), bookkeeping.getCurrency());
        balanceSheet.setOpaque(false);
        balanceSheet.setBorder(new EmptyBorder(10, 10, 10, 10));
        setViewportView(balanceSheet);

        initComponents();
        addListeners();
    }

    private void addListeners() {
        documentListener = new DocumentListenerImpl();
        document.addListener(documentListener);

        modelChangeListener = new ModelChangeListenerImpl();
        dateModel.addModelChangeListener(modelChangeListener);
    }

    private void removeListeners() {
    	dateModel.removeModelChangeListener(modelChangeListener);
    	document.removeListener(documentListener);
    }

    private void initComponents() {
        Date date = dateModel.getDate();
        if (date == null) {
            return; // do not change the current balance if the date is invalid
        }

        try {
			report = ObjectFactory.create(BookkeepingService.class).createReport(document, date);

            setBorder(Factory.getInstance(WidgetFactory.class)
                    .createTitleBorder("balanceComponent.title", report.getEndDate()));

            List<Row> leftRows = convertAccountsToRows(report.getAssetsInclLossAccount());
            List<Row> rightRows = convertAccountsToRows(report.getLiabilitiesInclProfitAccount());

            balanceSheet.setLeftRows(leftRows);
            balanceSheet.setRightRows(rightRows);
            balanceSheet.update();

            int row = 5 + Math.max(leftRows.size(), rightRows.size());

            balanceSheet.add(new JLabel(textResource.getString("balanceComponent.totalDebtors")),
                    SwingUtils.createGBConstraints(0, row, 2, 1, 0.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 0, 0, 0));
            balanceSheet.add(new JLabel(Factory.getInstance(AmountFormat.class).formatAmount(report.getTotalDebtors())),
                    SwingUtils.createGBConstraints(2, row, 2, 1, 0.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 0, 0, 0));
            row++;

            balanceSheet.add(new JLabel(textResource.getString("balanceComponent.totalCreditors")),
                    SwingUtils.createGBConstraints(0, row, 2, 1, 1.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 0, 0, 0));
            balanceSheet.add(new JLabel(Factory.getInstance(AmountFormat.class).formatAmount(report.getTotalCreditors())),
                    SwingUtils.createGBConstraints(2, row, 2, 1, 1.0, 0.0,
                            GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 0, 0, 0));
            row++;
		} catch (ServiceException e) {
			MessageDialog.showErrorMessage(this, e, "gen.internalError");
            close();
		}
    }

    private List<Row> convertAccountsToRows(List<Account> accounts) {
        List<Row> rows = new ArrayList<>();

        for (Account a : accounts) {
        	Row row = new Row();
        	row.description = a.getId() + ' ' + a.getName();
            row.amount = report.getAmount(a);
            rows.add(row);
        }

        return rows;
    }

    /**
     * Sets the background color.
     * @param color the background color
     */
    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        getViewport().setBackground(color);
        if (balanceSheet != null) {
            balanceSheet.setBackground(color);
        }
    }

    @Override
    public void close() {
    	removeListeners();
    }

    private final class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
		    if (((DateModel)(model)).getDate() != null) {
		        initComponents();
		        validate();
		    }
		}
	}

	private final class DocumentListenerImpl implements DocumentListener {
		@Override
		public void documentChanged(Document document) {
		    initComponents();
		    validate();
		}
	}
}
