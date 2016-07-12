package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.textsearch.criteria.StringLiteral;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

public class PartyServiceTest extends AbstractBookkeepingTest {

    private final PartyService partyService = new PartyService();

    @Test
    public void testAddingNonExistentParty() throws Exception {
        Party party = new Party("1115");
        party.setName("Hendrik Erikszoon");
        party.setAddress("Willemstraat 5");
        party.setZipCode("6122 CC");
        party.setCity("Heerenveen");
        party.setBirthDate(DateUtil.createDate(1976, 4, 23));
        party.setRemarks("This is an excellent person!");

        assertFalse(partyService.existsParty(document, party.getId()));
        partyService.createParty(document, party, Arrays.asList("excellent", "party"));
        assertEqualParty(party, partyService.getParty(document, party.getId()));
    }

    @Test(expected = ServiceException.class)
    public void testAddingExistentPartyFails() throws Exception {
        Party party = createParty();
        partyService.createParty(document, party, Arrays.asList("already", "exists"));
    }

    @Test
    public void updateExistingParty() throws Exception {
        Party party = createParty();

        party.setBirthDate(null);
        List<String> newTags = singletonList("updated");
        partyService.updateParty(document, party, newTags);

        Party updatedParty = partyService.getParty(document, party.getId());
        assertEqualParty(party, updatedParty);
        assertEquals(newTags, partyService.findTagsForParty(document, party));
    }

    @Test(expected = ServiceException.class)
    public void updateNonExistingPartyFails() throws Exception {
        Party party = new Party("1115");
        party.setName("Hendrik Erikszoon");
        assertFalse(partyService.existsParty(document, party.getId()));

        partyService.updateParty(document, party, singletonList("updated"));
    }

    @Test
    public void removeExistingParty() throws Exception {
        Party party = createParty();
        partyService.deleteParty(document, party);
        assertFalse(partyService.existsParty(document, party.getId()));
    }

    @Test(expected = ServiceException.class)
    public void removeNonExistingPartyFails() throws Exception {
        Party party = new Party("1115");
        party.setName("Hendrik Erikszoon");
        assertFalse(partyService.existsParty(document, party.getId()));

        partyService.deleteParty(document, party);
    }

    @Test
    public void testPartyTags() throws Exception {
        Party party = new Party("1115");
        party.setName("Hendrik Erikszoon");
        partyService.createParty(document, party, singletonList("Type 2"));

        party = new Party("1116");
        party.setName("Hendrika Eriksdochter");
        partyService.createParty(document, party, singletonList("Type 1"));

        party = new Party("1117");
        party.setName("Jan Jansen");
        partyService.createParty(document, party, emptyList());

        assertEquals("[Type 1, Type 2]", partyService.findPartyTags(document).toString());
    }

    private Party createParty() throws ServiceException {
        Party party = new Party("12345");
        party.setName("Hendrik Erikszoon");
        return partyService.createParty(document, party, emptyList());
    }

    @Test
    public void testPartySearchCriteria() throws Exception {
        assertEquals("[1101 Pietje Puk]", partyService.findParties(document, new StringLiteral("Puk")).toString());
        assertEquals("[1102 Jan Pieterszoon]", partyService.findParties(document, new StringLiteral("Sterrenlaan")).toString());
        assertEquals("[1101 Pietje Puk]", partyService.findParties(document, new StringLiteral("19800223")).toString());
        assertEquals("[1102 Jan Pieterszoon]", partyService.findParties(document, new StringLiteral("Eind")).toString());
        assertEquals("[1101 Pietje Puk]", partyService.findParties(document, new StringLiteral("15")).toString());
    }

    @Test
    public void performanceTest() throws ServiceException {
        for (int i=0; i<10000; i++) {
            Party party = new Party("p-" + i);
            party.setName("Hendrik Erikszoon");
            party.setAddress("Willemstraat 5");
            party.setZipCode("6122 CC");
            party.setCity("Heerenveen");
            party.setBirthDate(DateUtil.createDate(1976, 4, 23));
            party.setRemarks("This is an excellent person!");

            partyService.createParty(document, party, Arrays.asList("excellent-" + i, "party-" + i));
        }

        for (int i=0; i<1000; i++) {
            partyService.getParty(document, "p-" + i);
            partyService.getParty(document, "p-123");
        }
    }

}
