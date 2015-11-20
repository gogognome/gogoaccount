package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

class PartyAutomaticCollectionSettingsDAO extends AbstractDomainClassDAO<PartyAutomaticCollectionSettings> {

    private Document document;
    private List<String> partyId;

    public PartyAutomaticCollectionSettingsDAO(Document document) {
        super("party_automatic_collection_settings", null, document.getBookkeepingId());
    }

    public Map<String, PartyAutomaticCollectionSettings> getIdToParty(List<String> partyIds) throws SQLException {
        return execute("select * from " + tableName + " where party_id in (?)", partyIds)
                .toHashMap(r -> r.getString(getPkColumn()), r -> getObjectFromResultSet(r));
    }

    @Override
    protected PartyAutomaticCollectionSettings getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        PartyAutomaticCollectionSettings settings = new PartyAutomaticCollectionSettings(result.getString("party_id"));
        settings.setName(result.getString("name"));
        settings.setAddress(result.getString("address"));
        settings.setZipCode(result.getString("zip_code"));
        settings.setCity(result.getString("city"));
        settings.setCountry(result.getString("country"));
        settings.setIban(result.getString("iban"));
        settings.setMandateDate(result.getDate("mandate_date"));
        return settings;
    }

    @Override
    protected NameValuePairs getNameValuePairs(PartyAutomaticCollectionSettings settings) throws SQLException {
        return new NameValuePairs()
                .add("party_id", settings.getPartyId())
                .add("name", settings.getName())
                .add("address", settings.getAddress())
                .add("zip_code", settings.getZipCode())
                .add("city", settings.getCity())
                .add("country", settings.getCountry())
                .add("iban", settings.getIban())
                .add("mandate_date", settings.getMandateDate());
    }

    @Override
    protected String getPkColumn() {
        return "party_id";
    }
}
