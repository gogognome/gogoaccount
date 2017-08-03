package nl.gogognome.gogoaccount.test;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.textsearch.criteria.StringLiteral;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class PartyServiceTest extends AbstractBookkeepingTest {

    private final PartyService partyService = new PartyService(configurationService, settingsService);

    @Test
    public void testAddingNonExistentParty() throws Exception {
        Party party = new Party();
        party.setName("Hendrik Erikszoon");
        party.setAddress("Willemstraat 5");
        party.setZipCode("6122 CC");
        party.setCity("Heerenveen");
        party.setBirthDate(DateUtil.createDate(1976, 4, 23));
        party.setRemarks("This is an excellent person!");

        Party createdParty = partyService.createPartyWithNewId(document, party, asList("excellent", "party"));
        party = overrideId(party, createdParty.getId());
        assertEqualParty(party, partyService.getParty(document, createdParty.getId()));
    }

    @Test
    public void createPartyWithSpecifiedId_existingPartyId_fails() throws Exception {
        assertThrows(ServiceException.class, () -> partyService.createPartyWithSpecifiedId(document, pietPuk, asList("already", "exists")));
    }

    @Test
    public void updateParty_updatesPartyAndTags() throws Exception {
        pietPuk.setBirthDate(null);
        List<String> newTags = singletonList("updated");
        partyService.updateParty(document, pietPuk, newTags);

        Party updatedParty = partyService.getParty(document, pietPuk.getId());
        assertEqualParty(pietPuk, updatedParty);
        assertEquals(newTags, partyService.findTagsForParty(document, pietPuk));
    }

    @Test
    public void updateParty_nonExistingPartyId_fails() throws Exception {
        assertThrows(ServiceException.class, () -> partyService.updateParty(document, overrideId(pietPuk, "1234"), singletonList("updated")));
    }

    @Test
    public void deleteParty_existingParty_succeeds() throws Exception {
        partyService.deleteParty(document, pietPuk);
        assertFalse(partyService.existsParty(document, pietPuk.getId()));
    }

    @Test
    public void removeNonExistingPartyFails() throws Exception {
        Party party = overrideId(pietPuk, "1234");

        assertThrows(ServiceException.class, () -> partyService.deleteParty(document, party));
    }

    @Test
    public void findPartyTags_returnsUniqueTagsForAllParties() throws Exception {
        Party party = new Party();
        party.setName("Hendrik Erikszoon");
        partyService.createPartyWithNewId(document, party, singletonList("Type 2"));

        party = new Party();
        party.setName("Hendrika Eriksdochter");
        partyService.createPartyWithNewId(document, party, asList("Type 1", "Type 2"));

        party = new Party();
        party.setName("Jan Jansen");
        partyService.createPartyWithNewId(document, party, emptyList());

        assertEquals("[Type 1, Type 2]", partyService.findPartyTags(document).toString());
    }

    @Test
    public void testPartySearchCriteria() throws Exception {
        assertEquals("[0001 Pietje Puk]", partyService.findParties(document, new StringLiteral("Puk")).toString());
        assertEquals("[0002 Jan Pieterszoon]", partyService.findParties(document, new StringLiteral("Sterrenlaan")).toString());
        assertEquals("[0001 Pietje Puk]", partyService.findParties(document, new StringLiteral("19800223")).toString());
        assertEquals("[0002 Jan Pieterszoon]", partyService.findParties(document, new StringLiteral("Eind")).toString());
        assertEquals("[0001 Pietje Puk]", partyService.findParties(document, new StringLiteral("15")).toString());
    }

    private Party overrideId(Party party, String partyId) {
        party = spy(party);
        when(party.getId()).thenReturn(partyId);
        return party;
    }

}
