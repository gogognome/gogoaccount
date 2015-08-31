package nl.gogognome.gogoaccount.component.configuration;


import nl.gogognome.gogoaccount.businessobjects.AccountType;

/**
 * This class represents an account.
 */
public class Account implements Comparable<Account> {

    private String id;
    private String name;
    private AccountType type;

    /**
     * Constructs an account.
     * @param id the id of this account
     * @param name the name of this account
     * @param type the type of this account
     */
    public Account(String id, String name, AccountType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /**
     * Gets the id of this account.
     * @return the id of this account
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the name of this account.
     * @return the name of this account
     */
    public String getName() {
        return name;
    }

    /**
     * Checks whether this account is on the debet side of a balance or
     * of the results.
     * <code>true</code> indicates this account is an asset or a expense;
     * <code>false</code> indicates this account is a liability or a revenue.
     */
    public boolean isDebet() {
        return type.isDebet();
    }

    /**
     * Checks whether this account is on the credit side of a balance or
     * of the results.
     * <code>true</code> indicates this account is a liability or a revenue;
     * <code>false</code> indicates this account is a asset or an expense.
     */
    public boolean isCredit() {
    	return !isDebet();
    }

    /**
     * Gets the type of this account.
     * @return the type
     */
    public AccountType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Account) {
            return this.id.equals(((Account)o).getId());
        } else {
            return false;
        }
    }

    @Override
	public int compareTo(Account that) {
        return this.id.compareTo(that.id);
    }

    @Override
    public String toString() {
    	return id + ' ' + name;
    }
}
