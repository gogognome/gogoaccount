package nl.gogognome.gogoaccount.component.party;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.lib.util.StringUtil;
import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.criteria.Parser;
import nl.gogognome.textsearch.string.CriterionMatcher;
import nl.gogognome.textsearch.string.StringSearchFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static nl.gogognome.lib.util.StringUtil.isNullOrEmpty;

public class PartyService {

    private final CriterionMatcher criterionMatcher = new StringSearchFactory().caseInsensitiveCriterionMatcher();

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

    public List<Party> findParties(Document document, String searchCriterion) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Criterion criterion = isNullOrEmpty(searchCriterion) ? null : new Parser().parse(searchCriterion);
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

        String[] criteria = new String[10 + tags.size()];
        int index = 0;
        criteria[index++] = party.getId();
        criteria[index++] = party.getName();
        criteria[index++] = party.getAddress();
        criteria[index++] = party.getZipCode();
        criteria[index++] = party.getCity();
        criteria[index++] = party.getRemarks();
        criteria[index++] = formatDate("yyyyMMdd", party.getBirthDate());
        criteria[index++] = formatDate("yyyy-MM-dd", party.getBirthDate());
        criteria[index++] = formatDate("ddMMyyyy", party.getBirthDate());
        criteria[index++] = formatDate("dd-MM-yyyy", party.getBirthDate());

        for (String tag : tags) {
            criteria[index++] = tag;
        }
        return criterionMatcher.matches(criterion, criteria);
    }

    private String formatDate(String dateFormat, Date date) {
        return date == null ? null : new SimpleDateFormat(dateFormat).format(date);
    }

    public boolean existsParty(Document document, String partyId) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).exists(partyId));
    }

    public Map<String, Party> getIdToParty(Document document, List<String> partyIds) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDAO(document).getIdToParty(partyIds));
    }
}


