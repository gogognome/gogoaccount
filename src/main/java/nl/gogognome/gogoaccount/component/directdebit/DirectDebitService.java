package nl.gogognome.gogoaccount.component.directdebit;

import au.com.bytecode.opencsv.CSVWriter;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.lib.task.TaskProgressListener;
import nl.gogognome.lib.text.AmountFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class DirectDebitService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConfigurationService configurationService;
    private final LedgerService ledgerService;
    private final PartyService partyService;

    public DirectDebitService(ConfigurationService configurationService, LedgerService ledgerService,
                              PartyService partyService) {
        this.configurationService = configurationService;
        this.ledgerService = ledgerService;
        this.partyService = partyService;
    }

    public DirectDebitSettings getSettings(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new DirectDebitSettingsDAO(document).getSettings());
    }

    public void setSettings(Document document, DirectDebitSettings settings) throws ServiceException {
        ServiceTransaction.withoutResult(() -> new DirectDebitSettingsDAO(document).setSettings(settings));
    }

    public PartyDirectDebitSettings findSettings(Document document, Party party) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDirectDebitSettingsDAO(document).find(party.getId()));
    }

    public void setDirectDebitSettings(Document document, PartyDirectDebitSettings settings)
            throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            PartyDirectDebitSettingsDAO dao = new PartyDirectDebitSettingsDAO(document);
            if (dao.exists(settings.getPartyId())) {
                dao.update(settings);
            } else {
                dao.create(settings);
            }
        });
    }

    public List<PartyDirectDebitSettings> findSettingsForAllParties(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyDirectDebitSettingsDAO(document).findAll());
    }

    public void createSepaDirectDebitFile(Document document, File fileToCreate, List<Invoice> invoices, Date collectionDate, TaskProgressListener progressListener)
            throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            progressListener.onProgressUpdate(0);

            if (invoices.isEmpty()) {
                progressListener.onProgressUpdate(100);
                return;
            }

            // Get data needed for generating the SEPA file
            DirectDebitSettings settings = getSettings(document);
            List<String> partyIds = invoices.stream().map(Invoice::getPartyId).collect(toList());
            Map<String, Party> idToParty = partyService.getIdToParty(document, partyIds);
            Map<String, PartyDirectDebitSettings> idToPartyDirectDebitSettings =
                    new PartyDirectDebitSettingsDAO(document).getIdToParty(partyIds);

            // Determine which invoices that lead to validation errors in the SEPA file
            SepaFileGenerator sepaFileGenerator = new SepaFileGenerator(document, configurationService);
            List<String> invalidInvoices = determineInvalidInvoices(fileToCreate, invoices, collectionDate,
                    settings, idToParty, idToPartyDirectDebitSettings, sepaFileGenerator, progressListener);

            if (!invalidInvoices.isEmpty()) {
                if (!fileToCreate.delete()) {
                    logger.warn("Could not delete the file " + fileToCreate.getAbsolutePath() + ". It must be deleted because it contains invalid or incomplete data.");
                }
                throw new ServiceException("Invoices with incomplete or incorrect data: " + String.join(", ", invalidInvoices));
            }

            progressListener.onProgressUpdate(100);

            // Generate the SEPA file for all invoices
            sepaFileGenerator.generate(settings, invoices, fileToCreate, collectionDate,
                    idToParty, idToPartyDirectDebitSettings);

            // Increase sequence number for next SEPA file
            settings.setSequenceNumber(settings.getSequenceNumber() + 1);
            setSettings(document, settings);
        });
    }

    private List<String> determineInvalidInvoices(
            File fileToCreate,
            List<Invoice> invoices,
            Date collectionDate,
            DirectDebitSettings settings,
            Map<String, Party> idToParty,
            Map<String, PartyDirectDebitSettings> idToPartyDirectDebbitSettings,
            SepaFileGenerator sepaFileGenerator,
            TaskProgressListener progressListener) throws Exception {
        List<String> invalidInvoices = new ArrayList<>();
        for (int i=0; i<invoices.size(); i++) {
            progressListener.onProgressUpdate((i+1) * 100 / invoices.size());
            try {
                sepaFileGenerator.generate(settings, invoices.subList(i, i+1), fileToCreate, collectionDate,
                        idToParty, idToPartyDirectDebbitSettings);
                validateSepaDirectDebitFile(fileToCreate);
            } catch (ServiceException e) {
                invalidInvoices.add(invoices.get(i).getId() + " (" + idToParty.get(invoices.get(i).getPartyId())
                        + "): " + e.getMessage());
            }
        }
        return invalidInvoices;
    }

    public void validateSepaDirectDebitFile(File sepaFile) throws ServiceException {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try (InputStream xsdStream = getClass().getResourceAsStream("/sepa/pain.008.001.02.xsd")) {
                Schema schema = factory.newSchema(new StreamSource(xsdStream));
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(sepaFile));
            }
        } catch (Exception e) {
            throw new ServiceException("SEPA file " + sepaFile.getAbsolutePath() + " is not valid: " + e.getMessage());
        }
    }

    public void createJournalEntryForDirectDebit(
            Document document,
            Date collectionDate,
            String journalEntryId,
            String journalEntryDescription,
            List<Invoice> invoices,
            Account bankAccount,
            Account debtorAccount) throws ServiceException {
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setDate(collectionDate);
        journalEntry.setId(journalEntryId);
        journalEntry.setDescription(journalEntryDescription);

        List<JournalEntryDetail> details = new ArrayList<>();
        for (Invoice invoice : invoices) {
            JournalEntryDetail detail = new JournalEntryDetail();
            detail.setAccountId(bankAccount.getId());
            detail.setAmount(invoice.getAmountToBePaid());
            detail.setDebet(true);
            detail.setInvoiceId(invoice.getId());
            details.add(detail);

            detail = new JournalEntryDetail();
            detail.setAccountId(debtorAccount.getId());
            detail.setAmount(invoice.getAmountToBePaid());
            detail.setDebet(false);
            details.add(detail);
        }
        ledgerService.addJournalEntry(document, journalEntry, details, true);
    }

    public void createCsvForDirectDebitFile(Document document, File csvFile, List<Invoice> invoices) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            Currency currency = configurationService.getBookkeeping(document).getCurrency();
            AmountFormat amountFormat = new AmountFormat(new Locale("nl", "NL"), currency);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            PartyDirectDebitSettingsDAO partyDirectDebitSettingsDAO = new PartyDirectDebitSettingsDAO(document);
            try (CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFile), ';')) {
                for (Invoice invoice : invoices) {
                    String[] line = new String[11];
                    line[0] = amountFormat.formatAmountWithoutCurrency(invoice.getAmountToBePaid().toBigInteger());
                    line[1] = invoice.getPartyId();
                    PartyDirectDebitSettings partyDirectDebitSettings = partyDirectDebitSettingsDAO.get(invoice.getPartyId());
                    line[2] = dateFormat.format(partyDirectDebitSettings.getMandateDate());
                    line[3] = "";
                    line[4] = partyDirectDebitSettings.getIban();
                    line[5] = partyDirectDebitSettings.getName();
                    line[6] = partyDirectDebitSettings.getCountry();
                    line[7] = partyDirectDebitSettings.getAddress();
                    line[8] = (partyDirectDebitSettings.getZipCode() + ' ' + partyDirectDebitSettings.getCity()).trim();
                    line[9] = "OTHR";
                    line[10] = invoice.getDescription();
                    csvWriter.writeNext(line);
                }
            }
        });
    }
}
