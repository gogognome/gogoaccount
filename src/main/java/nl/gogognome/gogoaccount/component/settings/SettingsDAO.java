package nl.gogognome.gogoaccount.component.settings;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;

import java.sql.SQLException;

class SettingsDAO extends AbstractDomainClassDAO<Setting> {

    public SettingsDAO(Document document) {
        super("settings", null, document.getBookkeepingId());
    }

    public void save(Setting setting) throws SQLException {
        if (exists(setting.getKey())) {
            update(setting);
        } else {
            create(setting);
        }
    }

    @Override
    protected Setting getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        Setting setting = new Setting(result.getString("key"));
        setting.setValue(result.getString("value"));
        return setting;
    }

    @Override
    protected NameValuePairs getNameValuePairs(Setting setting) throws SQLException {
        return new NameValuePairs()
                .add("key", setting.getKey())
                .add("value", setting.getValue());
    }

    @Override
    protected String getPkColumn() {
        return "key";
    }
}
