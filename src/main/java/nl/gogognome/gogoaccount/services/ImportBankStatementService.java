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
package nl.gogognome.gogoaccount.services;

import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.services.importers.ImportedTransaction;

/**
 * Service for importing bank statements.
 *
 * @author Sander Kooijmans
 */
public class ImportBankStatementService {

	private final Database database;

	public ImportBankStatementService(Database database) {
		this.database = database;
	}

	/**
	 * @return the account corresponding to the "from account" of the imported transaction;
	 *         null if no account was found
	 */
	public Account getFromAccount(ImportedTransaction transaction) {
		return getAccountForImportedAccount(transaction.getFromAccount(), transaction.getFromName());
	}

	/**
	 * @return the account corresponding to the "to account" of the imported transaction;
	 *         null if no account was found
	 */
	public Account getToAccount(ImportedTransaction transaction) {
		return getAccountForImportedAccount(transaction.getToAccount(), transaction.getToName());
	}

	private Account getAccountForImportedAccount(String account, String name) {
		String key = getKey(account, name);
		return database.getAccountForImportedAccount(key);
	}

	public void setImportedToAccount(ImportedTransaction transaction, Account account) {
		setAccountForImportedAccount(transaction.getToAccount(), transaction.getToName(), account);
	}

	public void setImportedFromAccount(ImportedTransaction transaction,	Account account) {
		setAccountForImportedAccount(transaction.getFromAccount(), transaction.getFromName(), account);
	}

	private void setAccountForImportedAccount(String importedAccount, String name, Account account) {
		String key = getKey(importedAccount, name);
		database.setImportedAccount(key, account.getId());
	}

	private String getKey(String account, String name) {
		return account != null ? account : name;
	}
}
