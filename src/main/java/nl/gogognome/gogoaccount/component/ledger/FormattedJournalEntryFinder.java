package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.gogoaccount.component.criterion.ObjectCriterionMatcher;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.textsearch.criteria.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FormattedJournalEntryFinder {

    private final ObjectCriterionMatcher objectCriterionMatcher = new ObjectCriterionMatcher();
    private final LedgerService ledgerService;
    private final InvoiceService invoiceService;
    private final PartyService partyService = new PartyService();
    private Logger logger = LoggerFactory.getLogger(LedgerService.class);

    public FormattedJournalEntryFinder(LedgerService ledgerService, InvoiceService invoiceService) {
        this.ledgerService = ledgerService;
        this.invoiceService = invoiceService;
    }

    FormattedJournalEntry format(Document document, JournalEntry journalEntry) {
        FormattedJournalEntry formattedJournalEntry = new FormattedJournalEntry();
        formattedJournalEntry.id = journalEntry.getId();
        formattedJournalEntry.date = journalEntry.getDate();
        formattedJournalEntry.description = journalEntry.getDescription();
        formattedJournalEntry.journalEntry = journalEntry;
        try {
            List<JournalEntryDetail> details = ledgerService.findJournalEntryDetails(document, journalEntry);
            for (JournalEntryDetail detail : details) {
                if (detail.getInvoiceId() != null) {
                    Invoice invoice = invoiceService.getInvoice(document, detail.getInvoiceId());
                    addInvoice(document, formattedJournalEntry, invoice, detail.getAmount());
                }
            }
            if (journalEntry.getIdOfCreatedInvoice() != null) {
                Invoice invoice = invoiceService.getInvoice(document, journalEntry.getIdOfCreatedInvoice());
                addInvoice(document, formattedJournalEntry, invoice, invoice.getAmountToBePaid());
            }
        } catch (ServiceException e) {
            logger.warn(e.getMessage(), e);
            formattedJournalEntry.invoiceDescription = "???";
            formattedJournalEntry.party = "???";
        }
        return formattedJournalEntry;
    }

    private void addInvoice(Document document, FormattedJournalEntry formattedJournalEntry, Invoice invoice, Amount amount) throws ServiceException {
        AmountFormat af = Factory.getInstance(AmountFormat.class);
        formattedJournalEntry.invoiceDescription = append(formattedJournalEntry.invoiceDescription, invoice.getId()
                + " - " + invoice.getDescription() + " - " +  af.formatAmountWithoutCurrency(amount.toBigInteger()));
        Party party = partyService.getParty(document, invoice.getPartyId());
        formattedJournalEntry.party = append(formattedJournalEntry.party, party.getId() + " - " + party.getName());
    }

    private String append(String a, String b) {
        if (a == null) {
            return b;
        }
        return a + "; " + b;
    }

    boolean matches(Criterion criterion, FormattedJournalEntry journalEntry) {
        return criterion == null || objectCriterionMatcher.matches(criterion, journalEntry.date, journalEntry.id, journalEntry.description, journalEntry.invoiceDescription, journalEntry.party);
    }

}
