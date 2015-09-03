package nl.gogognome.gogoaccount.test;

import nl.gogognome.dataaccess.dao.NoRecordFoundException;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartySearchCriteria;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

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
        party.setType("excellent");
        party.setBirthDate(DateUtil.createDate(1976, 4, 23));
        party.setRemarks("This is an excellent person!");

        assertFalse(partyService.existsParty(document, party.getId()));
        partyService.createParty(document, party);
        assertEqualParty(party, partyService.getParty(document, party.getId()));
    }

    @Test(expected = ServiceException.class)
    public void testAddingExistentPartyFails() throws Exception {
        Party party = createParty();
        partyService.createParty(document, party);
    }

    @Test
    public void updateExistingParty() throws Exception {
        Party party = createParty();

        party.setType("failure");
        party.setBirthDate(null);
        partyService.updateParty(document, party);

        Party updatedParty = partyService.getParty(document, party.getId());
        assertEqualParty(party, updatedParty);
    }

    @Test(expected = ServiceException.class)
    public void updateNonExistingPartyFails() throws Exception {
        Party party = new Party("1115");
        party.setName("Hendrik Erikszoon");
        assertFalse(partyService.existsParty(document, party.getId()));

        partyService.updateParty(document, party);
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
    public void testPartyTypes() throws Exception {
        Party party = new Party("1115");
        party.setName("Hendrik Erikszoon");
        party.setType("Type 1");
        partyService.createParty(document, party);

        party = new Party("1116");
        party.setName("Hendrika Eriksdochter");
        party.setType("Type 2");
        partyService.createParty(document, party);

        party = new Party("1117");
        party.setName("Jan Jansen");
        party.setType(null);
        partyService.createParty(document, party);

        assertEquals("[Type 1, Type 2]", partyService.findPartyTypes(document).toString());
    }

    private Party createParty() throws ServiceException {
        Party party = new Party("12345");
        party.setName("Hendrik Erikszoon");
        return partyService.createParty(document, party);
    }

    @Test
    public void testPartySearchCriteria() throws Exception {
        PartySearchCriteria searchCriteria = new PartySearchCriteria();
        searchCriteria.setName("Puk");
        assertEquals("[1101 Pietje Puk]", partyService.findParties(document, searchCriteria).toString());

        searchCriteria = new PartySearchCriteria();
        searchCriteria.setAddress("Sterrenlaan");
        assertEquals("[1102 Jan Pieterszoon]", partyService.findParties(document, searchCriteria).toString());

        searchCriteria = new PartySearchCriteria();
        searchCriteria.setBirthDate(DateUtil.createDate(1980, 2, 23));
        assertEquals("[1101 Pietje Puk]", partyService.findParties(document, searchCriteria).toString());

        searchCriteria = new PartySearchCriteria();
        searchCriteria.setCity("Eind");
        assertEquals("[1102 Jan Pieterszoon]", partyService.findParties(document, searchCriteria).toString());

        searchCriteria = new PartySearchCriteria();
        searchCriteria.setZipCode("15");
        assertEquals("[1101 Pietje Puk]", partyService.findParties(document, searchCriteria).toString());
    }

}
