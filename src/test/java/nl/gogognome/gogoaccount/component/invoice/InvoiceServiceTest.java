package nl.gogognome.gogoaccount.component.invoice;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static nl.gogognome.lib.util.DateUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import nl.gogognome.gogoaccount.component.configuration.*;
import nl.gogognome.gogoaccount.component.invoice.amountformula.*;
import nl.gogognome.gogoaccount.component.ledger.*;
import nl.gogognome.gogoaccount.component.party.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.gogoaccount.test.*;
import nl.gogognome.gogoaccount.test.builders.*;
import nl.gogognome.lib.text.*;
import nl.gogognome.lib.util.*;
import nl.gogognome.textsearch.criteria.*;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class InvoiceServiceTest extends AbstractBookkeepingTest {

    @Test
    public void createInvoicesForMultipleParties_allInvoicesHaveCorrectAmountsAndUniqueId() throws Exception {
        removeExistingInvoices();
        List<Party> parties = partyService.findAllParties(document);
        assertTrue(parties.size() > 1);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Amount amount = AmountBuilder.build(20);
        Account debtor = configurationService.getAccount(document, "190");
        InvoiceTemplate invoiceTemplate = new InvoiceTemplate(InvoiceTemplate.Type.SALE, null, issueDate, "Invoice for {name}", buildSomeLine());
        ledgerService.createInvoiceAndJournalForParties(document, debtor, invoiceTemplate, parties);

        int index = 0;
        Set<String> uniqueIds = new HashSet<>();
        for (Invoice invoice : invoiceService.findAllInvoices(document)) {
            uniqueIds.add(invoice.getId());
            assertEquals("Invoice for " + parties.get(index).getName(), invoice.getDescription());
            assertEquals(singletonList("Zaalhuur"), invoiceService.findDescriptions(document, invoice));
            assertEquals(amount, invoice.getAmountToBePaid());
            index++;
        }
        assertEquals(parties.size(), uniqueIds.size());
    }

    @Test
    public void testInvoiceIdPatternWhenCreatingInvoices() throws Exception {
        assertEquals("[01, 02]", getInvoiceIdsWithInvoiceIdPattern("nn"));
        assertEquals("[20110001, 20110002]", getInvoiceIdsWithInvoiceIdPattern("yyyynnnn"));
        assertEquals("[2011020001, 2011020002]", getInvoiceIdsWithInvoiceIdPattern("yyyymmnnnn"));
        assertEquals("[XX-201102-0001-YY, XX-201102-0002-YY]", getInvoiceIdsWithInvoiceIdPattern("XX-yyyymm-nnnn-YY"));
    }

    private String getInvoiceIdsWithInvoiceIdPattern(String invoiceIdPattern) throws ServiceException {
        removeExistingInvoices();
        setInvoiceIdPattern(invoiceIdPattern);

        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Account debtor = configurationService.getAccount(document, "190");
        InvoiceTemplate invoiceTemplate = new InvoiceTemplate(InvoiceTemplate.Type.SALE, null, issueDate, "Invoice for {name}", buildSomeLine());
        List<Invoice> createdInvoices = ledgerService.createInvoiceAndJournalForParties(document, debtor, invoiceTemplate, parties);
        return createdInvoices.stream().map(Invoice::getId).collect(toList()).toString();
    }

    private void setInvoiceIdPattern(String invoiceIdPattern) throws ServiceException {
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        bookkeeping.setInvoiceIdFormat(invoiceIdPattern);
        configurationService.updateBookkeeping(document, bookkeeping);
    }

    @Test
    public void cannotCreateInvoicesWithoutAmountOnSingleLine() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        List<InvoiceTemplateLine> lines = singletonList(new InvoiceTemplateLine((AmountFormula) null, "Zaalhuur", configurationService.getAccount(document, "400")));
        InvoiceTemplate invoiceTemplate = new InvoiceTemplate(InvoiceTemplate.Type.SALE, "1231", issueDate, "Invoice for {name}", lines);
        Account debtor = configurationService.getAccount(document, "190");

        ServiceException exception = assertThrows(ServiceException.class, () ->
            ledgerService.createInvoiceAndJournalForParties(document, debtor, invoiceTemplate, parties));
        assertEquals("Amount must be filled in for all lines.", exception.getMessage());
    }

    @Test
    public void cannotCreateInvoicesWithoutDebtorOrCreditor() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        InvoiceTemplate invoiceTemplate = new InvoiceTemplate(InvoiceTemplate.Type.SALE, null, issueDate, "Invoice for {name}", buildSomeLine());

        ServiceException exception = assertThrows(ServiceException.class, () ->
            ledgerService.createInvoiceAndJournalForParties(document, null, invoiceTemplate, parties));

        assertEquals("resource-id: InvoiceService.accountMustHaveDebtorOrCreditorType", exception.getMessage());
    }

    @Test
    public void cannotCreateInvoicesWithAccountThatIsNotDebtorNorCreditor() throws Exception {
        List<Party> parties = partyService.findAllParties(document);
        Date issueDate = DateUtil.createDate(2011, 8, 20);
        Account nonDebtorAndNonCreditor = sportsHallRent;
        InvoiceTemplate invoiceTemplate = new InvoiceTemplate(InvoiceTemplate.Type.SALE, "1231", issueDate, "Invoice for {name}", buildSomeLine());

        ServiceException exception = assertThrows(ServiceException.class, () ->
            ledgerService.createInvoiceAndJournalForParties(document, nonDebtorAndNonCreditor, invoiceTemplate, parties));

        assertEquals("resource-id: InvoiceService.accountMustHaveDebtorOrCreditorType", exception.getMessage());
    }

    @Test
    public void updateExistingInvoice() throws Exception {
        Invoice invoice = createSalesInvoiceAndJournalEntry(createDate(2011, 3, 15), janPieterszoon, "Subscription 2011 {name}", subscription, debtors, 123);

        List<String> descriptions = asList("Changed description 1", "Changed description 2");
        List<Amount> amounts = asList(null, AmountBuilder.build(30));

        invoice.setPartyId(pietPuk.getId());
        invoice.setAmountToBePaid(AmountBuilder.build(30));
        invoice.setIssueDate(DateUtil.createDate(2011, 5, 6));

        invoiceService.updateInvoice(document, invoice, descriptions, amounts);

        assertEqualInvoice(invoice, invoiceService.getInvoice(document, invoice.getId()));
        assertEquals(descriptions, invoiceService.findDescriptions(document, invoice));
        assertEquals(amounts, invoiceService.findAmounts(document, invoice));
    }

    @Test
    public void updateNonExistingInvoiceFails() throws Exception {
        List<String> descriptions = asList("Sponsoring 2011", "Sponsoring");
        List<Amount> amounts = asList(null, AmountBuilder.build(30));
        Invoice invoice = new Invoice();
        invoice.setPartyId(pietPuk.getId());
        invoice.setAmountToBePaid(AmountBuilder.build(30));
        invoice.setIssueDate(DateUtil.createDate(2011, 5, 6));

        assertThrows(ServiceException.class, () -> invoiceService.updateInvoice(document, invoice, descriptions, amounts));
    }

    @Test
    public void findInvoiceOverviewsWithoutCriterionIncludeClosedInvoices() throws ServiceException {
        removeExistingInvoices();
        createInvoiceWithPayment(100, 100, pietPuk);
        createInvoiceWithPayment(100, 80, janPieterszoon);
        createInvoiceWithPayment(100, 120, janPieterszoon);
        List<InvoiceOverview> overviews = invoiceService.findInvoiceOverviews(document, null, true);
        assertInvoiceOverviewsEqual(overviews,
                "201100001 Invoice to be paid: 100, paid: 100 Pietje Puk",
                "201100002 Invoice to be paid: 100, paid: 80 Jan Pieterszoon",
                "201100003 Invoice to be paid: 100, paid: 120 Jan Pieterszoon");
    }

    @Test
    public void findInvoiceOverviewsWithoutCriterionExcludeClosedInvoices_amountPaidEqualsAmountToBePaid() throws ServiceException {
        removeExistingInvoices();
        createInvoiceWithPayment(100, 100, pietPuk);
        List<InvoiceOverview> overviews = invoiceService.findInvoiceOverviews(document, null, false);
        assertTrue(overviews.isEmpty());
    }

    @Test
    public void findInvoiceOverviewsWithoutCriterionExcludeClosedInvoices_amountPaidSmallerThanAmountToBePaid() throws ServiceException {
        removeExistingInvoices();
        createInvoiceWithPayment(100, 80, janPieterszoon);
        List<InvoiceOverview> overviews = invoiceService.findInvoiceOverviews(document, null, false);
        assertInvoiceOverviewsEqual(overviews,
                "201100001 Invoice to be paid: 100, paid: 80 Jan Pieterszoon");
    }

    @Test
    public void findInvoiceOverviewsWithoutCriterionExcludeClosedInvoices_amountPaidLargerThanAmountToBePaid() throws ServiceException {
        removeExistingInvoices();
        createInvoiceWithPayment(100, 120, janPieterszoon);
        List<InvoiceOverview> overviews = invoiceService.findInvoiceOverviews(document, null, false);
        assertInvoiceOverviewsEqual(overviews,
                "201100001 Invoice to be paid: 100, paid: 120 Jan Pieterszoon");
    }

    @Test
    public void findInvoiceOverviewsWitCriterionIncludeClosedInvoices_CriterionIsPuk() throws ServiceException {
        removeExistingInvoices();
        createInvoiceWithPayment(100, 100, pietPuk);
        createInvoiceWithPayment(100, 80, janPieterszoon);
        createInvoiceWithPayment(100, 120, janPieterszoon);
        List<InvoiceOverview> overviews = invoiceService.findInvoiceOverviews(document, new StringLiteral("puk"), true);
        assertInvoiceOverviewsEqual(overviews,
                "201100001 Invoice to be paid: 100, paid: 100 Pietje Puk");
    }

    private void removeExistingInvoices() throws ServiceException {
        for (JournalEntry entry : ledgerService.findJournalEntries(document)) {
            for (JournalEntryDetail detail : ledgerService.findJournalEntryDetails(document, entry)) {
                if (detail.getPaymentId() != null) {
                    ledgerService.removeJournalEntry(document, entry);
                }
            }
        }
        for (JournalEntry entry : ledgerService.findJournalEntries(document)) {
            if (entry.getIdOfCreatedInvoice() != null) {
                ledgerService.removeJournalEntry(document, entry);
            }
        }
    }

    private void createInvoiceWithPayment(int amountToBePaid, int amountPaid, Party party) throws ServiceException {
        InvoiceTemplate invoiceTemplate = new InvoiceTemplate(InvoiceTemplate.Type.SALE, null, DateUtil.createDate(2011, 8, 7),
                "Invoice to be paid: " + amountToBePaid + ", paid: " + amountPaid,
                singletonList(new InvoiceTemplateLine(AmountBuilder.build(amountToBePaid), "Contribution", subscription)));
        List<Invoice> createdInvoices = ledgerService.createInvoiceAndJournalForParties(document,
                debtors,
                invoiceTemplate,
                singletonList(party));

        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setId("P9543");
        journalEntry.setDescription("Payment");
        journalEntry.setDate(DateUtil.createDate(2011, 8, 30));
        List<JournalEntryDetail> details = Arrays.asList(
                JournalEntryBuilder.buildDetail(amountPaid, cash.getId(), true, createdInvoices.get(0).getId()),
                JournalEntryBuilder.buildDetail(amountPaid, debtors.getId(), false)
        );
        ledgerService.addJournalEntry(document, journalEntry, details, true);
    }

    private void assertInvoiceOverviewsEqual(List<InvoiceOverview> overviews, String... expectedOverviews) {
        assertEquals(
                Arrays.stream(expectedOverviews).reduce("", String::concat),
                overviews.stream().map(o -> o.getId() + ' ' + o.getDescription() + ' ' + o.getPartyName()).reduce("", String::concat));
    }

    @Test
    public void testMatches() {
        InvoiceOverview overview = new InvoiceOverview("Invoice id");
        overview.setAmountPaid(AmountBuilder.build(123));
        overview.setAmountToBePaid(AmountBuilder.build(456));
        overview.setDescription("Description");
        overview.setIssueDate(DateUtil.createDate(2011, 5, 6));
        overview.setPartyId("Party id");
        overview.setPartyName("Party name");

        assertTrue(invoiceService.matches(null, overview, amountFormat));
        assertMatches(overview, "123");
        assertMatches(overview, "456");
        assertMatches(overview, "Description");
        assertMatches(overview, "Invoice id");
        assertMatches(overview, "20110506");
        assertMatches(overview, "Party id");
        assertMatches(overview, "Party name");
        assertNotMatches(overview, "789");
    }

    private void assertMatches(InvoiceOverview overview, String text) {
        assertTrue(invoiceService.matches(new StringLiteral(text), overview, amountFormat));
    }

    private void assertNotMatches(InvoiceOverview overview, String text) {
        assertFalse(invoiceService.matches(new StringLiteral(text), overview, amountFormat));
    }

    private List<InvoiceTemplateLine> buildSomeLine() throws ServiceException {
        return singletonList(new InvoiceTemplateLine(AmountBuilder.build(20), "Zaalhuur", sportsHallRent));
    }

}
