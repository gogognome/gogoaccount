package nl.gogognome.gogoaccount.component.directdebit;

import nl.gogognome.dataaccess.dao.AbstractDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.lib.util.StringUtil;

import java.sql.SQLException;

class DirectDebitSettingsDAO extends AbstractDAO {

    public DirectDebitSettingsDAO(Document document) {
        super(document.getBookkeepingId());
    }

    public DirectDebitSettings getSettings() throws SQLException {
        DirectDebitSettings settings = new DirectDebitSettings();
        settings.setIban(findValue("iban"));
        settings.setBic(findValue("bic"));
        String sequenceNumber = findValue("sequenceNumber");
        if (!StringUtil.isNullOrEmpty(sequenceNumber)) {
            settings.setSequenceNumber(Long.parseLong(sequenceNumber));
        }
        settings.setSepaDirectDebitContractNumber(findValue("sepaDirectDebitContractNumber"));
        return settings;
    }

    public void setSettings(DirectDebitSettings settings) throws SQLException {
        setValue("iban", settings.getIban());
        setValue("bic", settings.getBic());
        setValue("sequenceNumber", Long.toString(settings.getSequenceNumber()));
        setValue("sepaDirectDebitContractNumber", settings.SepaDirectDebitContractNumber());
    }

    private String findValue(String key) throws SQLException {
        return execute("select value from direct_debit_settings where key=?", key).findFirst(r -> r.getString(1));
    }

    private void setValue(String key, String value) throws SQLException {
        execute("delete from direct_debit_settings where key=?", key).ignoreResult();
        insert("direct_debit_settings", new NameValuePairs().add("key", key).add("value", value));
    }
}
