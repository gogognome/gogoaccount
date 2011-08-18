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

import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.lib.gui.beans.ObjectFormatter;

/**
 * This class formats accounts in the format "<id> <name>".
 *
 * @author Sander Kooijmans
 */
public class AccountFormatter implements ObjectFormatter<Account> {

	@Override
	public String format(Account a) {
		return a != null ? a.getId() + ' ' + a.getName() : "";
	}

}
