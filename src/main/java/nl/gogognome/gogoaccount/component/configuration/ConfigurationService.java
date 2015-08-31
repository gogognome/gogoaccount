package nl.gogognome.gogoaccount.component.configuration;

import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;

import java.util.List;

public class ConfigurationService {

    public boolean hasAccounts(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).hasAny());
    }

    public List<Account> findAllAccounts(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).findAll("id"));
    }

    public void createAccount(Document document, Account account) throws ServiceException {
        ServiceTransaction.withoutResult(() -> new AccountDAO(document).create(account));
    }

    public void updateAccount(Document document, Account account) throws ServiceException {
        ServiceTransaction.withoutResult(() -> new AccountDAO(document).update(account));
    }

    public void deleteAccount(Document document, Account account) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            // TODO: Remove dependency on BookkeepingService. Will disappear when foreign keys are used
            if (BookkeepingService.inUse(document, account)) {
                throw new DocumentModificationFailedException("The account " + account + " is in use and can therefore not be deleted!");
            }
            new AccountDAO(document).delete(account.getId());
        });
    }


}
