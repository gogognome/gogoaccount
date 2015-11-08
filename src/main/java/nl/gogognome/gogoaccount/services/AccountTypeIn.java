package nl.gogognome.gogoaccount.services;


import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;

import com.google.common.base.Predicate;


public class AccountTypeIn implements Predicate<Account> {

	private final List<AccountType> accountTypes;


	public AccountTypeIn(AccountType... accountTypes) {
		this.accountTypes = newArrayList(accountTypes);
	}

	@Override
	public boolean apply(Account account) {
		return accountTypes.contains(account.getType());
	}

}