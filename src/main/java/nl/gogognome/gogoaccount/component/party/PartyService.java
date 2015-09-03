package nl.gogognome.gogoaccount.component.party;

import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class PartyService {

    public Party createParty(Document document, Party party) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Party createdParty = new PartyDAO(document).create(party);
            document.notifyChange();
            return createdParty;
        });
    }

    public Party getParty(Document document, String id) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).get(id));
    }

    public void updateParty(Document document, Party party) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            new PartyDAO(document).update(party);
            document.notifyChange();
        });
    }

    public void deleteParty(Document document, Party party) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            new PartyDAO(document).delete(party.getId());
            document.notifyChange();
        });
    }

    /**
     * @param document the document
     * @return the types of the parties. Each type occurs exactly ones. The types are sorted lexicographically.
     */
    public List<String> findPartyTypes(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).findPartyTypes());
    }

    public List<Party> findAllParties(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).findAll().stream().sorted().collect(toList()));
    }

    public List<Party> findParties(Document document, PartySearchCriteria searchCriteria) throws ServiceException {
        // TODO: use criteria to build a query
        return ServiceTransaction.withResult(() ->
                new PartyDAO(document).findAll().stream().filter(party -> searchCriteria.matches(party)).sorted().collect(toList()));
    }

    public boolean existsParty(Document document, String partyId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).exists(partyId));
    }
}


