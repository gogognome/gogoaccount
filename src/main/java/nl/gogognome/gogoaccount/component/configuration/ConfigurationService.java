package nl.gogognome.gogoaccount.component.configuration;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;

import java.sql.SQLException;
import java.util.List;

public class ConfigurationService {

    public boolean hasAccounts(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).hasAny());
    }

    public Account getAccount(Document document, String accountId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).get(accountId));
    }

    public List<Account> findAllAccounts(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).findAll("id"));
    }

    public List<Account> findAccountsOfType(Document document, AccountType accountType) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).findAccountsOfType(accountType));
    }

    public List<Account> findAssets(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).findAssets());
    }

    public List<Account> findLiabilities(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).findLiabilities());
    }

    public List<Account> findExpenses(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).findExpenses());
    }

    public List<Account> findRevenues(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AccountDAO(document).findRevenues());
    }

    public void createAccount(Document document, Account account) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            new AccountDAO(document).create(account);
            document.notifyChange();
        });
    }

    public void updateAccount(Document document, Account account) throws ServiceException {
        ServiceTransaction.withoutResult(() -> new AccountDAO(document).update(account));
    }

    public void deleteAccount(Document document, Account account) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            try {
                new AccountDAO(document).delete(account.getId());
            } catch (SQLException e) {
                if (e.getErrorCode() == 23505) {
                    throw new DocumentModificationFailedException("The account " + account + " is in use and can therefore not be deleted!");
                }
                throw e;
            }
        });
    }

    public Bookkeeping getBookkeeping(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new BookkeepingDAO(document).getSingleton());
    }

    public void updateBookkeeping(Document document, Bookkeeping bookkeeping) throws ServiceException {
        ServiceTransaction.withoutResult(() -> new BookkeepingDAO(document).update(bookkeeping));
    }

}
