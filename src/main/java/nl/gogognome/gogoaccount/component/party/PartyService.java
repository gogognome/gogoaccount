package nl.gogognome.gogoaccount.component.party;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class PartyService {

    public Party createParty(Document document, Party party, List<String> tags) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Party createdParty = new PartyDAO(document).create(party);
            new TagDAO(document).saveTags(createdParty.getId(), tags);
            document.notifyChange();
            return createdParty;
        });
    }

    public Party getParty(Document document, String id) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).get(id));
    }

    public void updateParty(Document document, Party party, List<String> tags) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            new PartyDAO(document).update(party);
            new TagDAO(document).saveTags(party.getId(), tags);
            document.notifyChange();
        });
    }

    public void deleteParty(Document document, Party party) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            new PartyDAO(document).delete(party.getId());
            document.notifyChange();
        });
    }

    public List<String> findPartyTags(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new TagDAO(document).findAllTags());
    }

    public List<Party> findAllParties(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).findAll().stream().sorted().collect(toList()));
    }

    public List<String> findTagsForParty(Document document, Party party) throws ServiceException {
        return ServiceTransaction.withResult(() -> new TagDAO(document).findTagsForParty(party.getId()));
    }

    public Map<String, List<String>> findPartyIdToTags(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new TagDAO(document).findPartyIdToTags());
    }

    public List<Party> findParties(Document document, PartySearchCriteria searchCriteria) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Map<String, List<String>> partyIdToTags = new TagDAO(document).findPartyIdToTags();
            return new PartyDAO(document).findAll().stream()
                    .filter(party -> searchCriteria.matches(party, partyIdToTags.get(party.getId())))
                    .sorted()
                    .collect(toList());
        });
    }

    public boolean existsParty(Document document, String partyId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).exists(partyId));
    }

    public Map<String, Party> getIdToParty(Document document, List<String> partyIds) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).getIdToParty(partyIds));
    }
}


