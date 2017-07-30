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

    public Party createWithNewId(Party party) throws SQLException {
        String nextPartyId = getNextId();

        NameValuePairs nvp = getNameValuePairs(party);
        nvp.remove("id");
        nvp.add("id", nextPartyId);
        insert(tableName, nvp);

        return this.getObjectFromResultSet(convertNameValuePairsToResultSet(nvp));
    }

    private String getNextId() throws SQLException {
        String previousId = execute("SELECT MAX(id) FROM " + tableName).findFirst(r -> r.getString(1));
        int previousIdNumber;
        if (previousId == null) {
            previousIdNumber = 0;
        } else {
            try {
                previousIdNumber = Integer.parseInt(previousId);
            } catch (NumberFormatException e) {
                throw new SQLException("Unexpected party id found while determining previous party id: " + previousId);
            }
        }
        return Integer.toString(previousIdNumber + 1);
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
        party.setEmailAddress(result.getString("email_address"));
        party.setZipCode(result.getString("zip_code"));
        party.setBirthDate(result.getDate("birth_date"));
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
                .add("email_address", party.getEmailAddress())
                .add("zip_code", party.getZipCode())
                .add("birth_date", party.getBirthDate())
                .add("remarks", party.getRemarks());
    }
}
