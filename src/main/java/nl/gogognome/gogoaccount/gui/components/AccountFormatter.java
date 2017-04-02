package nl.gogognome.gogoaccount.gui.components;

import nl.gogognome.gogoaccount.component.configuration.Account;
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
