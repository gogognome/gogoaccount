package nl.gogognome.gogoaccount.component.party;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.component.document.Document;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

class PartyDAO extends AbstractDomainClassDAO<Party> {

    public PartyDAO(Document document) {
        super("party", null, document.getBookkeepingId());
    }

    /**
     * @return the types of the parties. Each type occurs exactly ones. The types are sorted lexicographically.
     */
    public List<String> findPartyTypes() throws SQLException {
        return execute("SELECT DISTINCT type FROM " + tableName + " WHERE type IS NOT NULL ORDER BY type").toList(r -> r.getString(1));
    }

    public Map<String, Party> getIdToParty(List<String> partyIds) throws SQLException {
        return execute("SELECT * FROM " + tableName + " WHERE id IN (?)", partyIds).toHashMap(r -> r.getString("id"), r -> getObjectFromResultSet(r));
    }

    @Override
    protected Party getObjectFromResultSet(ResultSetWrapper result) throws SQLException {
        Party party = new Party(result.getString("id"));
        party.setName(result.getString("name"));
        party.setAddress(result.getString("address"));
        party.setCity(result.getString("city"));
        party.setZipCode(result.getString("zip_code"));
        party.setBirthDate(result.getDate("birth_date"));
        party.setType(result.getString("type"));
        party.setRemarks(result.getString("remarks"));
        return party;
    }

    @Override
    protected NameValuePairs getNameValuePairs(Party party) throws SQLException {
        return new NameValuePairs()
                .add("id", party.getId())
                .add("name", party.getName())
                .add("address", party.getAddress())
                .add("city", party.getCity())
                .add("zip_code", party.getZipCode())
                .add("birth_date", party.getBirthDate())
                .add("type", party.getType())
                .add("remarks", party.getRemarks());
    }
}
