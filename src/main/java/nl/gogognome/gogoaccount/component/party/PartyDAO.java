package nl.gogognome.gogoaccount.component.party;

import nl.gogognome.dataaccess.dao.AbstractDomainClassDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.dataaccess.dao.NoRecordFoundException;
import nl.gogognome.dataaccess.dao.ResultSetWrapper;
import nl.gogognome.gogoaccount.components.document.Document;

import java.sql.SQLException;
import java.util.List;

class PartyDAO extends AbstractDomainClassDAO<Party> {

    public PartyDAO(Document document) {
        super("party", null, document.getBookkeepingId());
    }

    public Party get(String partyId) throws SQLException {
        Party party = find(new NameValuePairs().add("id", partyId));
        if (party == null) {
            throw new NoRecordFoundException("No party exists with id " + partyId);
        }
        return party;
    }

    public void delete(String partyId) throws SQLException {
        delete(new NameValuePairs().add("id", partyId));
    }

    public boolean exists(String partyId) throws SQLException {
        return exists(new NameValuePairs().add("id", partyId));
    }

    /**
     * @return the types of the parties. Each type occurs exactly ones. The types are sorted lexicographically.
     */
    public List<String> findPartyTypes() throws SQLException {
        return execute("SELECT DISTINCT type FROM " + tableName + " WHERE type IS NOT NULL ORDER BY type").toList(r -> r.getString(1));
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
