package nl.gogognome.gogoaccount.component.configuration;

import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;

public class Bookkeeping {

    private String description;
    private Date startOfPeriod;
    private Currency currency = Currency.getInstance("EUR");

    private String organizationName;
    private String organizationAddress;
    private String organizationZipCode;
    private String organizationCity;
    private String organizationCountry;
    private boolean enableAutomaticCollection;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getOrganizationAddress() {
        return organizationAddress;
    }

    public void setOrganizationAddress(String organizationAddress) {
        this.organizationAddress = organizationAddress;
    }

    public String getOrganizationCity() {
        return organizationCity;
    }

    public void setOrganizationCity(String organizationCity) {
        this.organizationCity = organizationCity;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationZipCode() {
        return organizationZipCode;
    }

    public void setOrganizationZipCode(String organizationZipCode) {
        this.organizationZipCode = organizationZipCode;
    }

    public String getOrganizationCountry() {
        return organizationCountry;
    }

    public void setOrganizationCountry(String organizationCountry) {
        this.organizationCountry = organizationCountry;
    }

    public Date getStartOfPeriod() {
        return startOfPeriod;
    }

    public void setStartOfPeriod(Date startOfPeriod) {
        this.startOfPeriod = startOfPeriod;
    }

    public boolean isEnableAutomaticCollection() {
        return enableAutomaticCollection;
    }

    public void setEnableAutomaticCollection(boolean enableAutomaticCollection) {
        this.enableAutomaticCollection = enableAutomaticCollection;
    }
}
