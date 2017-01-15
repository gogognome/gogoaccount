package nl.gogognome.gogoaccount.component.party;

import nl.gogognome.gogoaccount.component.criterion.ObjectCriterionMatcher;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.textsearch.criteria.Criterion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class PartyService {

    private final ObjectCriterionMatcher objectCriterionMatcher = new ObjectCriterionMatcher();

    public Party createParty(Document document, Party party, List<String> tags) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            document.ensureDocumentIsWriteable();
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
            document.ensureDocumentIsWriteable();
            new PartyDAO(document).update(party);
            new TagDAO(document).saveTags(party.getId(), tags);
            document.notifyChange();
        });
    }

    public void deleteParty(Document document, Party party) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            document.ensureDocumentIsWriteable();
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

    public List<Party> findParties(Document document, Criterion criterion) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Map<String, List<String>> partyIdToTags = new TagDAO(document).findPartyIdToTags();
            return new PartyDAO(document).findAll().stream()
                    .filter(party -> matches(criterion, party, partyIdToTags.get(party.getId())))
                    .sorted()
                    .collect(toList());
        });
    }

    private boolean matches(Criterion criterion, Party party, List<String> tags) {
        if (criterion == null) {
            return true;
        }

        Object[] criteria = new Object[8 + tags.size()];
        int index = 0;
        criteria[index++] = party.getId();
        criteria[index++] = party.getName();
        criteria[index++] = party.getAddress();
        criteria[index++] = party.getZipCode();
        criteria[index++] = party.getCity();
        criteria[index++] = party.getEmailAddress();
        criteria[index++] = party.getRemarks();
        criteria[index++] = party.getBirthDate();

        for (String tag : tags) {
            criteria[index++] = tag;
        }
        return objectCriterionMatcher.matches(criterion, criteria);
    }

    public boolean existsParty(Document document, String partyId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).exists(partyId));
    }

    public Map<String, Party> getIdToParty(Document document, List<String> partyIds) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).getIdToParty(partyIds));
    }
}


