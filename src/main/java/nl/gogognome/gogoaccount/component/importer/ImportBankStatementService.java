package nl.gogognome.gogoaccount.component.importer;

import java.util.*;
import nl.gogognome.gogoaccount.component.configuration.*;
import nl.gogognome.gogoaccount.component.document.*;
import nl.gogognome.gogoaccount.services.*;

public class ImportBankStatementService {

    private static final List<String> SUBSTRINGS_TO_BE_IGNORED = List.of(
            "januari", "februari", "maart", "april", "mei", "juni", "juli", "augustus", "september", "oktober", "november", "december",
            "jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            " ");
    private final ConfigurationService configurationService;

    public ImportBankStatementService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * @return the account corresponding to the "from account" of the imported transaction;
     *         null if no account was found
     */
    public Account getFromAccount(Document document, ImportedTransaction transaction) throws ServiceException {
        return getAccountForImportedAccount(document, transaction.fromAccount(), transaction.fromName(), transaction.description());
    }

    /**
     * @return the account corresponding to the "to account" of the imported transaction;
     *         null if no account was found
     */
    public Account getToAccount(Document document, ImportedTransaction transaction) throws ServiceException {
        return getAccountForImportedAccount(document, transaction.toAccount(), transaction.toName(), transaction.description());
    }

    private Account getAccountForImportedAccount(Document document, String account, String name, String description) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            ImportedAccountDAO dao = new ImportedAccountDAO(document);

            String accountId = dao.findAccountIdByFrom(getKey(account, name, description));
            if (accountId == null) {
                accountId = dao.findAccountIdByFrom(getKey(account, name));
            }

            if (accountId != null) {
                return configurationService.getAccount(document, accountId);
            } else {
                return null;
            }
        });
    }

    public void setImportedToAccount(Document document, ImportedTransaction transaction, Account account) throws ServiceException {
        setAccountForImportedAccount(document, transaction.toAccount(), transaction.toName(), transaction.description(), account.getId());
    }

    public void setImportedFromAccount(Document document, ImportedTransaction transaction,	Account account) throws ServiceException {
        setAccountForImportedAccount(document, transaction.fromAccount(), transaction.fromName(), transaction.description(), account.getId());
    }

    private void setAccountForImportedAccount(Document document, String importedAccount, String name, String description, String accountId) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            document.ensureDocumentIsWriteable();
            ImportedAccountDAO dao = new ImportedAccountDAO(document);

            dao.setImportedAccount(getKey(importedAccount, name), accountId);
            dao.setImportedAccount(getKey(importedAccount, name, description), accountId);

            document.notifyChange();
        });
    }

    private String getKey(String account, String name, String description) {
        String key = getKey(account, name) + "|" + stripSpacesNumbersAndMonths(description);
        return key.length() < 100
                ? key
                : key.substring(0, 100);
    }

    private String stripSpacesNumbersAndMonths(String description) {
        description = description.toLowerCase();

        for (String substringToBeIgnored : SUBSTRINGS_TO_BE_IGNORED) {
            description = description.replace(substringToBeIgnored, "");
        }

        return description;
    }

    private String getKey(String account, String name) {
        return account != null ? account : name;
    }


    public Map<String, String> getImportedTransactionAccountToAccountMap(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new ImportedAccountDAO(document).findImportedTransactionAccountToAccountMap());
    }

}
