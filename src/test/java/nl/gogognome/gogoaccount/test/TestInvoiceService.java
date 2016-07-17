package nl.gogognome.gogoaccount.test;

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

import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;

public class TestInvoiceService extends AbstractBookkeepingTest {

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
                singletonList(new InvoiceLineDefinition(amount, configurationService.getAccount(document, "400"))));

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
        Amount a = createAmount(20);
        Account debtor = configurationService.getAccount(document, "190");
        invoiceService.createInvoiceAndJournalForParties(document, debtor, "auto", parties, issueDate, "Invoice for {name}",
                singletonList(new InvoiceLineDefinition(a, configurationService.getAccount(document, "400"))));

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

    @Test(expected = ServiceException.class)
    public void cannotCreateInvoicesWithoutAmountToBePaidSelected() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        List<InvoiceLineDefinition> lines = singletonList(
                new InvoiceLineDefinition(null, configurationService.getAccount(document, "400")));

        Account debtor = configurationService.getAccount(document, "190");
        invoiceService.createInvoiceAndJournalForParties(document, debtor, "inv-{id}", parties, issueDate, "Invoice for {name}", lines);
    }

    @Test(expected = ServiceException.class)
    public void cannotCreateInvoicesWithoutAmountOnSingleLine() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Amount a = createAmount(20);
        List<InvoiceLineDefinition> lines = Arrays.asList(
            new InvoiceLineDefinition(null, configurationService.getAccount(document, "400")),
            new InvoiceLineDefinition(a, configurationService.getAccount(document, "400")));

        Account debtor = configurationService.getAccount(document, "190");
        invoiceService.createInvoiceAndJournalForParties(document, debtor, "inv-{id}", parties, issueDate, "Invoice for {name}", lines);
    }

    @Test(expected = ServiceException.class)
    public void cannotCreateInvoicesWithoutDebtorOrCreditor() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Amount a = createAmount(20);
        List<InvoiceLineDefinition> lines = Arrays.asList(
                new InvoiceLineDefinition(null, configurationService.getAccount(document, "400")),
                new InvoiceLineDefinition(a, configurationService.getAccount(document, "400")));

        invoiceService.createInvoiceAndJournalForParties(document, null, "inv-{id}", parties, issueDate, "Invoice for {name}", lines);
    }

    @Test(expected = ServiceException.class)
    public void cannotCreateInvoicesWithoutDebtorOrCreditor2() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Amount a = createAmount(20);
        List<InvoiceLineDefinition> lines = Arrays.asList(
                new InvoiceLineDefinition(null, configurationService.getAccount(document, "400")),
                new InvoiceLineDefinition(a, configurationService.getAccount(document, "400")));

        Account nonDebtorAndNonCreditor = configurationService.getAccount(document, "400");
        invoiceService.createInvoiceAndJournalForParties(document, nonDebtorAndNonCreditor, "inv-{id}", parties, issueDate, "Invoice for {name}", lines);
    }

    @Test
    public void updateExistingInvoice() throws Exception {
        List<String> descriptions = Arrays.asList("Sponsoring 2011", "Sponsoring");
        List<Amount> amounts = Arrays.asList(null, createAmount(30));
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
        List<String> descriptions = Arrays.asList("Sponsoring 2011", "Sponsoring");
        List<Amount> amounts = Arrays.asList(null, createAmount(30));
        Party party = partyService.getParty(document, "1102");
        Invoice invoice = new Invoice("inv421");
        invoice.setConcerningPartyId(party.getId());
        invoice.setPayingPartyId(party.getId());
        invoice.setAmountToBePaid(createAmount(30));
        invoice.setIssueDate(DateUtil.createDate(2011, 5, 6));

        invoiceService.updateInvoice(document, invoice, descriptions, amounts);
    }
}
