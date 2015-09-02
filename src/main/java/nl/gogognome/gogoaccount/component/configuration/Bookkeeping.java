package nl.gogognome.gogoaccount.component.configuration;

import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;

public class Bookkeeping {

    /**
     * Contains the start date of the account period.
     */
    private Date startOfPeriod;

    /** The currency of all amounts. */
    private Currency currency = Currency.getInstance("EUR");

    public Date getStartOfPeriod() {
        return startOfPeriod;
    }

    public void setStartOfPeriod(Date startOfPeriod) {
        this.startOfPeriod = startOfPeriod;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
