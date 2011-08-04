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
package cf.ui.components;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.lib.gui.Closeable;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.OperationalResult;
import cf.ui.components.BalanceSheet.Row;

/**
 * This class implements a graphical component that shows an operational result.
 *
 * @author Sander Kooijmans
 */
public class OperationalResultComponent extends JScrollPane implements Closeable {

	private static final long serialVersionUID = 1L;

    private Database database;
    private DateModel dateModel;
    private BalanceSheet balanceSheet;

    private DatabaseListener databaseListener;
    private ModelChangeListener modelChangeListener;

    private TextResource textResource = Factory.getInstance(TextResource.class);

    /**
     * Creates a new <code>OperationalResultComponent</code>.
     * @param database the database used to create the operational result
     * @param dateModel the date model used to determine the date of the operational result
     */
    public OperationalResultComponent(Database database, DateModel dateModel) {
        super();
        this.database = database;
        this.dateModel = dateModel;

        balanceSheet = new BalanceSheet(textResource.getString("gen.expenses"),
        		textResource.getString("gen.revenues"), database.getCurrency());
        balanceSheet.setOpaque(false);
        balanceSheet.setBorder(new EmptyBorder(10, 10, 10, 10));
        setViewportView(balanceSheet);

        initComponents();
        addListeners();
    }

    private void addListeners() {
        databaseListener = new DatabaseListenerImpl();
        database.addListener(databaseListener);

        modelChangeListener = new ModelChangeListenerImpl();
        dateModel.addModelChangeListener(modelChangeListener);
    }

    @Override
    public void close() {
    	removeListeners();
    }

    private void removeListeners() {
    	dateModel.removeModelChangeListener(modelChangeListener);
    	database.removeListener(databaseListener);
    }

    private void initComponents() {
        Date date = dateModel.getDate();
        if (date == null) {
            return; // do not change the current operational result if the date is invalid
        }

        OperationalResult operationalResult = database.getOperationalResult(date);

        setBorder(Factory.getInstance(WidgetFactory.class)
        		.createTitleBorder("operationalResultComponent.title", operationalResult.getDate()));

        List<Row> leftRows = convertAccountsToRows(database.getExpenses(), date);
        List<Row> rightRows = convertAccountsToRows(database.getRevenues(), date);

        balanceSheet.setLeftRows(leftRows);
        balanceSheet.setRightRows(rightRows);
        balanceSheet.update();
    }

    private List<Row> convertAccountsToRows(Account[] accounts, Date date) {
        List<Row> rows = new ArrayList<Row>();

        for (int i=0; i<accounts.length; i++) {
        	Row row = new Row();
        	row.description = accounts[i].getId() + " " + accounts[i].getName();
            row.amount = BookkeepingService.getAccountBalance(database, accounts[i], date);
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

    private final class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
		    if (((DateModel)(model)).getDate() != null) {
		        initComponents();
		        validateTree();
		    }
		}
	}

	private final class DatabaseListenerImpl implements DatabaseListener {
		@Override
		public void databaseChanged(Database db) {
		    initComponents();
		    validateTree();
		}
	}
}
