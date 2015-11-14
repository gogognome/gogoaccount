package nl.gogognome.gogoaccount.component.importer;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;

import java.util.Map;

public class ImportBankStatementService {

    /**
     * @return the account corresponding to the "from account" of the imported transaction;
     *         null if no account was found
     */
    public Account getFromAccount(Document document, ImportedTransaction transaction) throws ServiceException {
        return getAccountForImportedAccount(document, transaction.getFromAccount(), transaction.getFromName());
    }

    /**
     * @return the account corresponding to the "to account" of the imported transaction;
     *         null if no account was found
     */
    public Account getToAccount(Document document, ImportedTransaction transaction) throws ServiceException {
        return getAccountForImportedAccount(document, transaction.getToAccount(), transaction.getToName());
    }

    private Account getAccountForImportedAccount(Document document, String account, String name) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            String key = getKey(account, name);
            String accountId = new ImportedAccountDAO(document).findAccountIdByFrom(key);
            if (accountId != null) {
                return ObjectFactory.create(ConfigurationService.class).getAccount(document, accountId);
            } else {
                return null;
            }
        });
    }

    public void setImportedToAccount(Document document, ImportedTransaction transaction, Account account) throws ServiceException {
        setAccountForImportedAccount(document, transaction.getToAccount(), transaction.getToName(), account.getId());
    }

    public void setImportedFromAccount(Document document, ImportedTransaction transaction,	Account account) throws ServiceException {
        setAccountForImportedAccount(document, transaction.getFromAccount(), transaction.getFromName(), account.getId());
    }

    public void setAccountForImportedAccount(Document document, String importedAccount, String name, String accountId) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            String key = getKey(importedAccount, name);
            new ImportedAccountDAO(document).setImportedAccount(key, accountId);
            document.notifyChange();
        });
    }

    private String getKey(String account, String name) {
        return account != null ? account : name;
    }

    public Map<String, String> getImportedTransactionAccountToAccountMap(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new ImportedAccountDAO(document).findImportedTransactionAccountToAccountMap());
    }

}
