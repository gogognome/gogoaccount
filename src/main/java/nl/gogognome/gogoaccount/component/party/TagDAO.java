package nl.gogognome.gogoaccount.component.party;

import nl.gogognome.dataaccess.dao.AbstractDAO;
import nl.gogognome.dataaccess.dao.NameValuePairs;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.lib.collections.DefaultValueMap;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

class TagDAO extends AbstractDAO {

    public TagDAO(Document document) {
        super(document.getBookkeepingId());
    }

    public void saveTags(String partyId, List<String> tags) throws SQLException {
        execute("DELETE FROM party_tag WHERE party_id=?", partyId).ignoreResult();
        int index = 1;
        for (String tag : tags) {
            insert("party_tag", new NameValuePairs()
                    .add("party_id", partyId)
                    .add("index", index++)
                    .add("tag", tag));
        }
    }

    public List<String> findTagsForParty(String partyId) throws SQLException {
        return execute("SELECT tag FROM party_tag WHERE party_id=? ORDER BY index", partyId)
                .toList(r -> r.getString(1));
    }

    public Map<String, List<String>> findPartyIdToTags() throws SQLException {
        return new DefaultValueMap<>(execute("SELECT * FROM party_tag ORDER BY party_id, index")
                .toHashMapOfLists(r -> r.getString("party_id"), r -> r.getString("tag")),
                emptyList());
    }

    public List<String> findAllTags() throws SQLException {
        return execute("SELECT DISTINCT tag FROM party_tag ORDER BY tag")
                .toList(r -> r.getString(1));
    }
}
