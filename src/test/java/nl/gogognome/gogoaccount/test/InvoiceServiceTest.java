package nl.gogognome.gogoaccount.test;

import junit.framework.Assert;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceLineDefinition;
import nl.gogognome.gogoaccount.component.invoice.InvoiceSearchCriteria;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;
import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class InvoiceServiceTest extends AbstractBookkeepingTest {

    private final ConfigurationService configurationService = new ConfigurationService();
    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
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
}
