package nl.gogognome.gogoaccount.test;

import junit.framework.Assert;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.*;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.textsearch.criteria.StringLiteral;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class InvoiceServiceTest extends AbstractBookkeepingTest {

    private final ConfigurationService configurationService = new ConfigurationService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final LedgerService ledgerService = new LedgerService();
    private final PartyService partyService = new PartyService();

    @Test
    public void whenCreatingInvoiceIdAndNameShouldBeFilledInInvoicesIdAndDescription() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Amount amount = createAmount(20);
        Account debtor = configurationService.getAccount(document, "190");
        invoiceService.createInvoiceAndJournalForParties(document, debtor, "inv-{id}", parties, issueDate, "Invoice for {name}",
                singletonList(new InvoiceLineDefinition(amount, "Zaalhuur", configurationService.getAccount(document, "400"))));

        for (Party party : parties) {
            Invoice invoice = invoiceService.getInvoice(document, "inv-" + party.getId());
            assertEquals("Invoice for " + party.getName(), invoice.getDescription());
            assertEquals(singletonList("Zaalhuur"), invoiceService.findDescriptions(document, invoice));
            assertEquals(amount, invoice.getAmountToBePaid());
        }
    }

    @Test
    public void whenCreatingInvoicesIdsMustBeUnique() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Account debtor = configurationService.getAccount(document, "190");
        invoiceService.createInvoiceAndJournalForParties(document, debtor, "auto", parties, issueDate, "Invoice for {name}",
                singletonList(new InvoiceLineDefinition(createAmount(20), "Zaalhuur", configurationService.getAccount(document, "400"))));

        Set<String> ids = new HashSet<>();
        for (Party p : parties) {
            InvoiceSearchCriteria searchCriteria = new InvoiceSearchCriteria();
            searchCriteria.setName(p.getName());
            searchCriteria.setId("auto");
            List<Invoice> invoices = invoiceService.findInvoices(document, searchCriteria);
            assertEquals(1, invoices.size());
            ids.add(invoices.get(0).getId());
        }

        assertEquals(parties.size(), ids.size());
    }

    @Test
    public void cannotCreateInvoicesWithoutAmountToBePaidSelected() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        List<InvoiceLineDefinition> lines = singletonList(
                new InvoiceLineDefinition(null, "Zaalhuur", configurationService.getAccount(document, "400")));

        Account debtor = configurationService.getAccount(document, "190");
        ServiceException exception = assertThrows(ServiceException.class, () ->
                invoiceService.createInvoiceAndJournalForParties(document, debtor, "inv-{id}", parties, issueDate, "Invoice for {name}", lines));
        assertEquals("nl.gogognome.dataaccess.DataAccessException: A line without amount has been found!", exception.getMessage());
    }

    @Test
    public void cannotCreateInvoicesWithoutId() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        List<InvoiceLineDefinition> lines = singletonList(
                new InvoiceLineDefinition(createAmount(20), "Zaalhuur", configurationService.getAccount(document, "400")));

        Account debtor = configurationService.getAccount(document, "190");
        ServiceException exception = assertThrows(ServiceException.class, () ->
                invoiceService.createInvoiceAndJournalForParties(document, debtor, null, parties, issueDate, "Invoice for {name}", lines));
        assertEquals("nl.gogognome.dataaccess.DataAccessException: An invoice must have an id!", exception.getMessage());
    }

    @Test
    public void cannotCreateInvoicesWithoutAmountOnSingleLine() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        List<InvoiceLineDefinition> lines = asList(
            new InvoiceLineDefinition(null, "Zaalhuur", configurationService.getAccount(document, "400")));

        Account debtor = configurationService.getAccount(document, "190");
        ServiceException exception = assertThrows(ServiceException.class, () ->
            invoiceService.createInvoiceAndJournalForParties(document, debtor, "inv-{id}", parties, issueDate, "Invoice for {name}", lines));
        assertEquals("nl.gogognome.dataaccess.DataAccessException: A line without amount has been found!", exception.getMessage());
    }

    @Test
    public void cannotCreateInvoicesWithoutDebtorOrCreditor() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Amount a = createAmount(20);
        List<InvoiceLineDefinition> lines = asList(
                new InvoiceLineDefinition(a, "Zaalhuur", configurationService.getAccount(document, "400")));

        ServiceException exception = assertThrows(ServiceException.class, () ->
            invoiceService.createInvoiceAndJournalForParties(document, null, "inv-{id}", parties, issueDate, "Invoice for {name}", lines));
        assertEquals("nl.gogognome.dataaccess.DataAccessException: Selected account muet have type Debtor or Creditor!", exception.getMessage());
    }

    @Test
    public void cannotCreateInvoicesWithAccountThatIsNotDebtorNorCreditor() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Amount a = createAmount(20);
        List<InvoiceLineDefinition> lines = asList(
                new InvoiceLineDefinition(a, "Zaalhuur", configurationService.getAccount(document, "400")));

        Account nonDebtorAndNonCreditor = configurationService.getAccount(document, "400");
        ServiceException exception = assertThrows(ServiceException.class, () ->
            invoiceService.createInvoiceAndJournalForParties(document, nonDebtorAndNonCreditor, "inv-{id}", parties, issueDate, "Invoice for {name}", lines));
        assertEquals("nl.gogognome.dataaccess.DataAccessException: Selected account muet have type Debtor or Creditor!", exception.getMessage());
    }

    @Test
    public void updateExistingInvoice() throws Exception {
        List<String> descriptions = asList("Sponsoring 2011", "Sponsoring");
        List<Amount> amounts = asList(null, createAmount(30));
        Party party = partyService.getParty(document, "1102");
        Invoice invoice = new Invoice("inv1");
        invoice.setConcerningPartyId(party.getId());
        invoice.setPayingPartyId(party.getId());
        invoice.setAmountToBePaid(createAmount(30));
        invoice.setIssueDate(DateUtil.createDate(2011, 5, 6));

        invoiceService.updateInvoice(document, invoice, descriptions, amounts);

        assertEqualInvoice(invoice, invoiceService.getInvoice(document, "inv1"));
        assertEquals(descriptions, invoiceService.findDescriptions(document, invoice));
        assertEquals(amounts, invoiceService.findAmounts(document, invoice));
    }

    @Test(expected = ServiceException.class)
    public void updateNonExistingInvoiceFails() throws Exception {
        List<String> descriptions = asList("Sponsoring 2011", "Sponsoring");
        List<Amount> amounts = asList(null, createAmount(30));
        Party party = partyService.getParty(document, "1102");
        Invoice invoice = new Invoice("inv421");
        invoice.setConcerningPartyId(party.getId());
        invoice.setPayingPartyId(party.getId());
        invoice.setAmountToBePaid(createAmount(30));
        invoice.setIssueDate(DateUtil.createDate(2011, 5, 6));

        invoiceService.updateInvoice(document, invoice, descriptions, amounts);
    }

    @Test
    public void findInvoiceOverviewsWithoutCriterionIncludeClosedInvoices() throws ServiceException {
        createInvoiceWithPayment("I-001", 100, 100, pietPuk);
        createInvoiceWithPayment("I-002", 100, 80, janPieterszoon);
        createInvoiceWithPayment("I-003", 100, 120, janPieterszoon);
        List<InvoiceOverview> overviews = invoiceService.findInvoiceOverviews(document, null, true);
        assertInvoiceOverviewsEqual(overviews,
                "inv1 Contributie 2011 Pietje Puk",
                "I-001 Invoice I-001 to be paid: 100, paid: 100 Pietje Puk",
                "I-002 Invoice I-002 to be paid: 100, paid: 80 Jan Pieterszoon",
                "I-003 Invoice I-003 to be paid: 100, paid: 120 Jan Pieterszoon");
    }

    @Test
    public void findInvoiceOverviewsWithoutCriterionExcludeClosedInvoices() throws ServiceException {
        createInvoiceWithPayment("I-001", 100, 100, pietPuk);
        createInvoiceWithPayment("I-003", 100, 120, janPieterszoon);
        List<InvoiceOverview> overviews = invoiceService.findInvoiceOverviews(document, null, false);
        assertInvoiceOverviewsEqual(overviews,
                "inv1 Contributie 2011 Pietje Puk",
                "I-003 Invoice I-003 to be paid: 100, paid: 120 Jan Pieterszoon");
    }

    @Test
    public void findInvoiceOverviewsWitCriterionIncludeClosedInvoices() throws ServiceException {
        createInvoiceWithPayment("I-001", 100, 100, pietPuk);
        createInvoiceWithPayment("I-002", 100, 80, janPieterszoon);
        createInvoiceWithPayment("I-003", 100, 120, janPieterszoon);
        List<InvoiceOverview> overviews = invoiceService.findInvoiceOverviews(document, new StringLiteral("puk"), true);
        assertInvoiceOverviewsEqual(overviews,
                "inv1 Contributie 2011 Pietje Puk",
                "I-001 Invoice I-001 to be paid: 100, paid: 100 Pietje Puk");
    }

    private void createInvoiceWithPayment(String invoiceId, int amountToBePaid, int amountPaid, Party party) throws ServiceException {
        invoiceService.createInvoiceAndJournalForParties(document,
                debtors,
                invoiceId,
                Collections.singletonList(party),
                DateUtil.createDate(2011, 8, 7),
                "Invoice " + invoiceId + " to be paid: " + amountToBePaid + ", paid: " + amountPaid,
                Collections.singletonList(new InvoiceLineDefinition(createAmount(amountToBePaid), "Contribution", contribution)));

        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setId("P9543");
        journalEntry.setDescription("Payment");
        journalEntry.setDate(DateUtil.createDate(2011, 8, 30));
        List<JournalEntryDetail> details = Arrays.asList(
                buildJournalEntryDetail(amountPaid, cash.getId(), true, invoiceId, null),
                buildJournalEntryDetail(amountPaid, debtors.getId(), false)
        );
        ledgerService.addJournalEntry(document, journalEntry, details, true);
    }

    private void assertInvoiceOverviewsEqual(List<InvoiceOverview> overviews, String... expectedOverviews) {
        assertEquals(
                Arrays.stream(expectedOverviews).reduce("", String::concat),
                overviews.stream().map(o -> o.getInvoiceId() + ' ' + o.getDescription() + ' ' + o.getPartyName()).reduce("", String::concat));
    }
}
