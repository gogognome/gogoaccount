package nl.gogognome.gogoaccount.component.configuration;

import java.util.List;

import com.google.common.collect.Lists;

public enum AccountType {

    ASSET(true, true), DEBTOR(true, true),
    LIABILITY(false, true), CREDITOR(false, true), EQUITY(false, true),
    EXPENSE(true, false), REVENUE(false, false);
    
    private AccountType(boolean debet, boolean balanceAccount) {
        this.debet = debet;
        this.balanceAccount = balanceAccount;
    }
    
    private final boolean debet;
    private final boolean balanceAccount;

    public boolean isDebet() {
        return debet;
    }

    public boolean isBalanceAccount() {
        return balanceAccount;
    }

    public static List<AccountType> get(boolean debet, boolean balanceAccount) {
        List<AccountType> matchingTypes = Lists.newArrayList();
        for (AccountType type : values()) {
            if (type.isDebet() == debet && type.isBalanceAccount() == balanceAccount) {
                matchingTypes.add(type);
            }
        }
        return matchingTypes;
    }
}