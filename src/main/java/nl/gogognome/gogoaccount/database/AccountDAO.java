package nl.gogognome.gogoaccount.database;

import com.google.common.base.Joiner;
import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.NoRecordFoundException;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.businessobjects.AccountType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static nl.gogognome.gogoaccount.businessobjects.AccountType.*;

public class AccountDAO extends AbstractDomainClassDAO<Account> {

    public AccountDAO() {
        super("account", "domain_class_sequence", "bookkeeping");
    }

    public Account get(String accountId) throws SQLException {
        Account account = find(new NameValuePairs().add("id", accountId));
        if (account == null) {
            throw new NoRecordFoundException("No account exists with id " + accountId);
        }
        return account;
    }

    public void delete(String accountId) throws SQLException {
        delete(new NameValuePairs().add("id", accountId));
    }

    public List<Account> findAssets() throws SQLException {
        return findAll(new NameValuePairs().add("type", ASSET.name(), DEBTOR.name()), "id");
    }

    public List<Account> findLiabilities() throws SQLException {
        return findAll(new NameValuePairs().add("type", LIABILITY.name(), CREDITOR.name(), EQUITY.name()), "id");
    }

    public List<Account> findExpenses() throws SQLException {
        return findAll(new NameValuePairs().add("type", EXPENSE), "id");
    }

    public List<Account> findRevenues() throws SQLException {
        return findAll(new NameValuePairs().add("type", REVENUE), "id");
    }

    public List<Account> findAccountsOfType(AccountType accountType) throws SQLException {
        return findAll(new NameValuePairs().add("type", accountType));
    }

    @Override
    protected Account getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        String id = result.getString("id");
        String name = result.getString("name");
        AccountType type = result.getEnum(AccountType.class, "type");
        return new Account(id, name, type);
    }

    @Override
    protected NameValuePairs getNameValuePairs(Account account) throws SQLException {
        return new NameValuePairs()
                .add("id", account.getId())
                .add("name", account.getName())
                .add("type", account.getType());
    }
}
