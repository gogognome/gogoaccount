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
package cf.engine;

import java.util.Date;
import java.util.Locale;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;

/**
 * This class represents an operational result.
 *
 * @author Sander Kooijmans
 */
public class OperationalResult {

    private Date date;

    private Amount totalExpenses;

    private Amount totalRevenues;

    /**
     * Constructor.
     * @param database the datebase from which the operational result is constructed
     * @param date the date of the operational result
     */
    protected OperationalResult(Database database, Date date) {
        this.date = date;
        totalExpenses = Amount.getZero(database.getCurrency());
        totalRevenues = totalExpenses;

        Account[] expenses = getExpenses();

        for (int i = 0; i < expenses.length; i++) {
            totalExpenses = totalExpenses.add(
                BookkeepingService.getAccountBalance(database, expenses[i], date));
        }

        Account[] revenues = getRevenues();
        for (int i = 0; i < revenues.length; i++) {
            totalRevenues = totalRevenues.add(
                BookkeepingService.getAccountBalance(database, revenues[i], date));
        }
    }

    public Date getDate()
    {
        return date;
    }

    public Amount getTotalExpenses()
    {
        return totalExpenses;
    }

    public Amount getTotalRevenues()
    {
        return totalRevenues;
    }

    /**
     * Gets the result of operations.
     * @return the result of operations (positive indicates profit, negative
     *          indicates loss)
     */
    public Amount getResultOfOperations()
    {
        return totalRevenues.subtract(totalExpenses);
    }

    public Account[] getExpenses()
    {
        return Database.getInstance().getExpenses();
    }

    public Account[] getRevenues()
    {
        return Database.getInstance().getRevenues();
    }

    /**
     * Gets a string representation of the operational result.
     * @return a string representation of the operational result
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        AmountFormat af = new AmountFormat(Locale.getDefault());

        result.append("operation result of ");
        result.append(date);
        result.append('\n');
        int columnWidth = 45;

        StringBuilder sb = null;
        Account[] expenses = getExpenses();
        Account[] revenues = getRevenues();

        Database database = Database.getInstance();
        int n = Math.max(expenses.length, revenues.length);
        int index = 0;
        for (int i=0; i<n; i++)
        {
            sb = new StringBuilder();
            if (i < expenses.length)
            {
	            sb.append(expenses[i].getId());
	            sb.append(' ');
	            sb.append(expenses[i].getName());
	            index = sb.length();
	            sb.append(af.formatAmount(
	                BookkeepingService.getAccountBalance(database, expenses[i], date)));
            }
            else
            {
                index = 0;
            }
            while (sb.length() < columnWidth)
            {
                sb.insert(index, ' ');
            }
            result.append(sb);
            result.append(" | ");

            sb = new StringBuilder();

            if (i < revenues.length)
            {
	            sb.append(revenues[i].getId());
	            sb.append(' ');
	            sb.append(revenues[i].getName());
	            index = sb.length();
	            sb.append(af.formatAmount(
	                BookkeepingService.getAccountBalance(database, revenues[i], date)));
            }
            else
            {
                index = 0;
            }
            while (sb.length() < columnWidth)
            {
                sb.insert(index, ' ');
            }
            result.append(sb);
            result.append('\n');
        }

        result.append('\n');
        sb = new StringBuilder();

        sb.append(af.formatAmount(getTotalExpenses()));
        while (sb.length() < columnWidth)
        {
            sb.insert(0, ' ');
        }
        result.append(sb);
        result.append(" | ");

        sb = new StringBuilder();
        sb.append(af.formatAmount(getTotalRevenues()));
        while (sb.length() < columnWidth)
        {
            sb.insert(0, ' ');
        }
        result.append(sb);
        result.append('\n');

        return result.toString();
    }
}
