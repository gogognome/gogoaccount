package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;

import java.sql.SQLException;

class PartyAutomaticCollectionSettingsDAO extends AbstractDomainClassDAO<PartyAutomaticCollectionSettings> {

    public PartyAutomaticCollectionSettingsDAO(Document document) {
        super("party_automatic_collection_settings", null, document.getBookkeepingId());
    }

    @Override
    protected PartyAutomaticCollectionSettings getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        PartyAutomaticCollectionSettings settings = new PartyAutomaticCollectionSettings(result.getString("party_id"));
        settings.setName(result.getString("name"));
        settings.setAddress(result.getString("address"));
        settings.setZipCode(result.getString("zip_code"));
        settings.setCity(result.getString("city"));
        settings.setIban(result.getString("iban"));
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
                .add("iban", settings.getIban());
    }

    @Override
    protected String getPkColumn() {
        return "party_id";
    }
}
