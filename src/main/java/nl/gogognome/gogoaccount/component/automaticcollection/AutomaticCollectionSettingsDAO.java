package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.dataaccess.dao.AbstractDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.gogoaccount.component.document.Document;

import java.sql.SQLException;
import java.util.Map;

class AutomaticCollectionSettingsDAO extends AbstractDAO {

    public AutomaticCollectionSettingsDAO(Document document) {
        super(document.getBookkeepingId());
    }

    public AutomaticCollectionSettings getSettings() throws SQLException {
        AutomaticCollectionSettings settings = new AutomaticCollectionSettings();
        settings.setIban(findValue("iban"));
        settings.setBic(findValue("bic"));
        settings.setAutomaticCollectionContractNumber(findValue("automaticCollectionContractNumber"));
        return settings;
    }

    public void setSettings(AutomaticCollectionSettings settings) throws SQLException {
        setValue("iban", settings.getIban());
        setValue("bic", settings.getBic());
        setValue("automaticCollectionContractNumber", settings.getAutomaticCollectionContractNumber());
    }

    private String findValue(String key) throws SQLException {
        return execute("select value from automatic_collection_settings where key=?", key).findFirst(r -> r.getString(1));
    }

    private void setValue(String key, String value) throws SQLException {
        execute("delete from automatic_collection_settings where key=?", key).ignoreResult();
        insert("automatic_collection_settings", new NameValuePairs().add("key", key).add("value", value));
    }
}
