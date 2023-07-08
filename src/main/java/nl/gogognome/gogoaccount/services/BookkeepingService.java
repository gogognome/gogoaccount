package nl.gogognome.gogoaccount.services;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.businessobjects.ReportBuilder;
import nl.gogognome.gogoaccount.component.directdebit.DirectDebitService;
import nl.gogognome.gogoaccount.component.directdebit.DirectDebitSettings;
import nl.gogognome.gogoaccount.component.directdebit.PartyDirectDebitSettings;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceSending;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.lib.collections.DefaultValueMap;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.util.DateUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class BookkeepingService {

    private final DirectDebitService directDebitService;
    private final ConfigurationService configurationService;
    private final DocumentService documentService;
    private final LedgerService ledgerService;
    private final InvoiceService invoiceService;
    private final PartyService partyService;

    public BookkeepingService(DirectDebitService directDebitService, LedgerService ledgerService,
							  ConfigurationService configurationService, DocumentService documentService,
							  InvoiceService invoiceService, PartyService partyService) {
        this.directDebitService = directDebitService;
        this.ledgerService = ledgerService;
        this.configurationService = configurationService;
        this.documentService = documentService;
        this.invoiceService = invoiceService;
        this.partyService = partyService;
    }

    public Document closeBookkeeping(Document document, File newBookkeepingFile, String description, Date date, Account equity) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            document.ensureDocumentIsWriteable();

            markBookkeepingAsClosed(document);
            Date dayBeforeStart = DateUtil.addDays(date, -1);
            Document newDocument = createNewBookkeeping(document, newBookkeepingFile, description, date);
            copyDirectDebitSettings(document, newDocument);
            copyParties(document, newDocument);
            copyAccounts(document, newDocument);
            createStartBalance(document, newDocument, dayBeforeStart, equity);
            copyOpenInvoices(document, newDocument, dayBeforeStart);
            copyRemainingJournalEntries(document, newDocument, date);
            copySettings(document, newDocument);

            newDocument.notifyChange();
            return newDocument;
        });
    }

    private void markBookkeepingAsClosed(Document document) throws ServiceException {
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        bookkeeping.setClosed(true);
        configurationService.updateBookkeeping(document, bookkeeping);
        document.setReadonly(true);
    }

    private void copyDirectDebitSettings(Document document, Document newDocument) throws ServiceException {
        DirectDebitSettings settings = directDebitService.getSettings(document);
        directDebitService.setSettings(newDocument, settings);
    }

    private Document createNewBookkeeping(Document document, File newBookkeepingFile, String description, Date date) throws ServiceException {
        Document newDocument = documentService.createNewDocument(newBookkeepingFile, "New bookkeeping");
        newDocument.setDatabaseFile(newBookkeepingFile);
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        Bookkeeping newBookkeeping = configurationService.getBookkeeping(newDocument);
        newBookkeeping.setDescription(description);
        newBookkeeping.setCurrency(bookkeeping.getCurrency());
        newBookkeeping.setInvoiceIdFormat(bookkeeping.getInvoiceIdFormat());
        newBookkeeping.setPartyIdFormat(bookkeeping.getPartyIdFormat());
        newBookkeeping.setEnableSepaDirectDebit(bookkeeping.isEnableSepaDirectDebit());
        newBookkeeping.setOrganizationAddress(bookkeeping.getOrganizationAddress());
        newBookkeeping.setOrganizationCity(bookkeeping.getOrganizationCity());
        newBookkeeping.setOrganizationCountry(bookkeeping.getOrganizationCountry());
        newBookkeeping.setOrganizationName(bookkeeping.getOrganizationName());
        newBookkeeping.setOrganizationZipCode(bookkeeping.getOrganizationZipCode());
        newBookkeeping.setStartOfPeriod(date);
        configurationService.updateBookkeeping(newDocument, newBookkeeping);
        return newDocument;
    }

    private void copyParties(Document document, Document newDocument) throws ServiceException {
        Map<String, List<String>> partyIdToTags = partyService.findPartyIdToTags(document);
        for (Party party : partyService.findAllParties(document)) {
            partyService.createPartyWithSpecifiedId(newDocument, party, partyIdToTags.get(party.getId()));
            PartyDirectDebitSettings settings = directDebitService.findSettings(document, party);
            if (settings != null) {
                directDebitService.setDirectDebitSettings(newDocument, settings);
            }
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
        List<InvoiceSending> invoiceSendings = invoiceService.findAllInvoiceSendings(document);
        for (Invoice invoice : invoiceService.findAllInvoices(document)) {
            if (!invoiceService.isPaid(document, invoice.getId(), dayBeforeStart)) {
                invoiceService.createInvoice(newDocument, invoice);
                invoiceService.createDetails(newDocument, invoice,
                        invoiceService.findDescriptions(document, invoice), invoiceService.findAmounts(document, invoice));
                invoiceService.createPayments(newDocument, invoiceService.findPayments(document, invoice));

                List<InvoiceSending> invoiceSendingsForInvoice = invoiceSendings.stream()
                        .filter(invoiceSending -> invoiceSending.getInvoiceId().equals(invoice.getId()))
                        .toList();
                for (InvoiceSending invoiceSending : invoiceSendingsForInvoice) {
                    invoiceService.createInvoiceSending(newDocument, invoiceSending);
                }
            }
        }
    }

    // TODO: move to new report component
    public Report createReport(Document document, Date date) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            ReportBuilder reportBuilder = new ReportBuilder(document, configurationService, invoiceService, partyService);
            reportBuilder.init();
            reportBuilder.setEndDate(date);
            reportBuilder.setAssets(configurationService.findAssets(document));
            reportBuilder.setLiabilities(configurationService.findLiabilities(document));
            reportBuilder.setExpenses(configurationService.findExpenses(document));
            reportBuilder.setRevenues(configurationService.findRevenues(document));

            List<JournalEntry> journalEntries = ledgerService.findJournalEntries(document);
            DefaultValueMap<Long, List<JournalEntryDetail>> journalEntryIdToDetailsMap = ledgerService.getJournalEntryIdToDetailsMap(document);
            for (JournalEntry journalEntry : journalEntries) {
                if (DateUtil.compareDayOfYear(journalEntry.getDate(), date) <= 0) {
                    reportBuilder.addJournal(journalEntry, journalEntryIdToDetailsMap.get(journalEntry.getUniqueId()));
                }
            }

            DefaultValueMap<String, List<Payment>> invoiceIdToPaymentsMap = invoiceService.getInvoiceIdToPaymentsMap(document);
            for (Invoice invoice : invoiceService.findAllInvoices(document)) {
                if (DateUtil.compareDayOfYear(invoice.getIssueDate(), date) <= 0) {
                    reportBuilder.addInvoice(invoice, invoiceIdToPaymentsMap.get(invoice.getId()));
                }
            }

            return reportBuilder.build();
        });
    }

    private void copySettings(Document document, Document newDocument) throws ServiceException {
        SettingsService settingsService = new SettingsService();
        Map<String, String> keyToValue = settingsService.findAllSettings(document);
        for (Map.Entry<String, String> keyValue : keyToValue.entrySet()) {
            settingsService.save(newDocument, keyValue.getKey(), keyValue.getValue());
        }
    }

}
