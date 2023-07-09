package nl.gogognome.gogoaccount.component.importer;

import nl.gogognome.dataaccess.dao.AbstractDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.gogoaccount.component.document.Document;

import java.sql.SQLException;
import java.util.Map;

class ImportedAccountDAO extends AbstractDAO {

    public ImportedAccountDAO(Document document) {
        super(document.getBookkeepingId());
    }

    public void setImportedAccount(String importedAccount, String accountId) throws SQLException {
        importedAccount = escapeNull(importedAccount);
        execute("delete from import_account where import_account=?", importedAccount).ignoreResult();

        NameValuePairs nvps = new NameValuePairs()
                .add("import_account", importedAccount)
                .add("account_id", accountId);
        insert("import_account", nvps);
    }

    public String findAccountIdByFrom(String importedAccount) throws SQLException {
        importedAccount = escapeNull(importedAccount);
        return execute("select account_id from import_account where import_account=?", importedAccount)
                .findFirst(r -> r.getString(1));
    }

    private String escapeNull(String importedAccount) {
        return importedAccount != null ? importedAccount : "<null>";
    }

    public Map<String, String> findImportedTransactionAccountToAccountMap() throws SQLException {
        return execute("select import_account, account_id from import_account")
                .toTreeMap(
                        r -> r.getString(1),
                        r -> r.getString(2));
    }
}
