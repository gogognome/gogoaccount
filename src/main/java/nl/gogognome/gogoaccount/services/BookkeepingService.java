package nl.gogognome.gogoaccount.services;

import nl.gogognome.gogoaccount.businessobjects.*;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.DocumentService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import java.io.File;
import java.util.*;


/**
 * This class offers methods to manipulate the bookkeeping.
 */
public class BookkeepingService {

    private final ConfigurationService configurationService;
    private final DocumentService documentService;
    private final LedgerService ledgerService;
    private final InvoiceService invoiceService;
    private final PartyService partyService;

    public BookkeepingService(LedgerService ledgerService, ConfigurationService configurationService, DocumentService documentService,
                              InvoiceService invoiceService, PartyService partyService) {
        this.ledgerService = ledgerService;
        this.configurationService = configurationService;
        this.documentService = documentService;
        this.invoiceService = invoiceService;
        this.partyService = partyService;
    }

    public Document closeBookkeeping(Document document, File newBookkeepingFile, String description, Date date, Account equity) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Date dayBeforeStart = DateUtil.addDays(date, -1);
            Document newDocument = CreateNewBookkeeping(document, newBookkeepingFile, description, date);
            copyParties(document, newDocument);
            copyAccounts(document, newDocument);
            createStartBalance(document, newDocument, dayBeforeStart, equity);
            copyOpenInvoices(document, newDocument, dayBeforeStart);
            copyRemainingJournalEntries(document, newDocument, date);

            newDocument.notifyChange();
            return newDocument;
        });
    }

    private Document CreateNewBookkeeping(Document document, File newBookkeepingFile, String description, Date date) throws ServiceException {
        Document newDocument = documentService.createNewDocument(newBookkeepingFile, "New bookkeeping");
        newDocument.setFileName(null);
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        Bookkeeping newBookkeeping = configurationService.getBookkeeping(newDocument);
        newBookkeeping.setDescription(description);
        newBookkeeping.setCurrency(bookkeeping.getCurrency());
        newBookkeeping.setStartOfPeriod(date);
        configurationService.updateBookkeeping(newDocument, newBookkeeping);
        return newDocument;
    }

    private void copyParties(Document document, Document newDocument) throws ServiceException {
        Map<String, List<String>> partyIdToTags = partyService.findPartyIdToTags(document);
        for (Party party : partyService.findAllParties(document)) {
            partyService.createParty(newDocument, party, partyIdToTags.get(party.getId()));
        }
    }

    private void copyAccounts(Document document, Document newDocument) throws ServiceException {
        for (Account account : this.configurationService.findAllAccounts(document)) {
            this.configurationService.createAccount(newDocument, account);
        }
    }

    private void createStartBalance(Document document, Document newDocument, Date dayBeforeStart, Account equity) throws ServiceException {
        List<JournalEntryDetail> journalEntryDetails = new ArrayList<>(20);
        for (Account account : configurationService.findAssets(document)) {
            JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
            journalEntryDetail.setAmount(ledgerService.getAccountBalance(document, account, dayBeforeStart));
            journalEntryDetail.setAccountId(account.getId());
            journalEntryDetail.setDebet(true);
            if (!journalEntryDetail.getAmount().isZero()) {
                journalEntryDetails.add(journalEntryDetail);
            }
        }
        for (Account account : configurationService.findLiabilities(document)) {
            JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
            journalEntryDetail.setAmount(ledgerService.getAccountBalance(document, account, dayBeforeStart));
            journalEntryDetail.setAccountId(account.getId());
            if (!journalEntryDetail.getAmount().isZero()) {
                journalEntryDetails.add(journalEntryDetail);
            }
        }

        // Add the result of operations to the specified account.
        Report report = createReport(document, dayBeforeStart);
        Amount resultOfOperations = report.getResultOfOperations();
        if (resultOfOperations.isPositive()) {
            JournalEntryDetail profit = new JournalEntryDetail();
            profit.setAmount(resultOfOperations);
            profit.setAccountId(equity.getId());
            profit.setDebet(false);
            journalEntryDetails.add(profit);
        } else if (resultOfOperations.isNegative()) {
            JournalEntryDetail loss = new JournalEntryDetail();
            loss.setAmount(resultOfOperations.negate());
            loss.setAccountId(equity.getId());
            loss.setDebet(true);
            journalEntryDetails.add(loss);
        }
        try {
            JournalEntry startBalance = new JournalEntry();
            startBalance.setId("start");
            startBalance.setDescription("start balance");
            startBalance.setDate(dayBeforeStart);
            ledgerService.addJournalEntry(newDocument, startBalance, journalEntryDetails);
        } catch (IllegalArgumentException  e) {
            throw new ServiceException("Failed to create journal for start balance.", e);
        }
    }

    private void copyRemainingJournalEntries(Document document, Document newDocument, Date date) throws ServiceException {
        for (JournalEntry journalEntry : ledgerService.findJournalEntries(document)) {
            if (DateUtil.compareDayOfYear(date, journalEntry.getDate()) <= 0) {
                ledgerService.addJournalEntry(newDocument, journalEntry, ledgerService.findJournalEntryDetails(document, journalEntry));
            }
        }
    }

    private void copyOpenInvoices(Document document, Document newDocument, Date dayBeforeStart) throws ServiceException {
        for (Invoice invoice : invoiceService.findAllInvoices(document)) {
            if (!invoiceService.isPaid(document, invoice.getId(), dayBeforeStart)) {
                invoiceService.createInvoice(newDocument, invoice);
                invoiceService.createDetails(newDocument, invoice,
                        invoiceService.findDescriptions(document, invoice), invoiceService.findAmounts(document, invoice));
                invoiceService.createPayments(newDocument, invoiceService.findPayments(document, invoice));
            }
        }
    }

    // TODO: move to new report component
    public Report createReport(Document document, Date date) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            ReportBuilder reportBuilder = new ReportBuilder(document, configurationService, invoiceService, ledgerService, partyService);
            reportBuilder.init();
            reportBuilder.setEndDate(date);
            reportBuilder.setAssets(configurationService.findAssets(document));
            reportBuilder.setLiabilities(configurationService.findLiabilities(document));
            reportBuilder.setExpenses(configurationService.findExpenses(document));
            reportBuilder.setRevenues(configurationService.findRevenues(document));

            List<JournalEntry> journalEntries = ledgerService.findJournalEntries(document);
            for (JournalEntry journalEntry : journalEntries) {
                if (DateUtil.compareDayOfYear(journalEntry.getDate(), date) <= 0) {
                    reportBuilder.addJournal(journalEntry);
                }
            }

            for (Invoice invoice : invoiceService.findAllInvoices(document)) {
                if (DateUtil.compareDayOfYear(invoice.getIssueDate(), date) <= 0) {
                    reportBuilder.addInvoice(invoice);
                }
            }

            return reportBuilder.build();
        });
    }

}
