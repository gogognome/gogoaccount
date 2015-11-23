package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.task.TaskProgressListener;

import javax.print.Doc;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class AutomaticCollectionService {

    public AutomaticCollectionSettings getSettings(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AutomaticCollectionSettingsDAO(document).getSettings());
    }

    public void setSettings(Document document, AutomaticCollectionSettings settings) throws ServiceException {
        ServiceTransaction.withoutResult(() -> new AutomaticCollectionSettingsDAO(document).setSettings(settings));
    }

    public PartyAutomaticCollectionSettings findSettings(Document document, Party party) throws ServiceException {
        return ServiceTransaction.withResult(() -> new PartyAutomaticCollectionSettingsDAO(document).find(party.getId()));
    }

    public void setAutomaticCollectionSettings(Document document, PartyAutomaticCollectionSettings settings)
            throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            PartyAutomaticCollectionSettingsDAO dao = new PartyAutomaticCollectionSettingsDAO(document);
            if (dao.exists(settings.getPartyId())) {
                dao.update(settings);
            } else {
                dao.create(settings);
            }
        });
    }

    public void createSepaAutomaticCollectionFile(Document document, File fileToCreate, List<Invoice> invoices, Date collectionDate, TaskProgressListener progressListener)
            throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            progressListener.onProgressUpdate(0);

            if (invoices.isEmpty()) {
                progressListener.onProgressUpdate(100);
                return;
            }

            // Get data needed for generating the SEPA file
            AutomaticCollectionSettings settings = getSettings(document);
            PartyService partyService = ObjectFactory.create(PartyService.class);
            List<String> partyIds = invoices.stream().map(i -> i.getConcerningPartyId()).collect(toList());
            Map<String, Party> idToParty = partyService.getIdToParty(document, partyIds);
            Map<String, PartyAutomaticCollectionSettings> idToPartyAutomaticCollectionSettings =
                    new PartyAutomaticCollectionSettingsDAO(document).getIdToParty(partyIds);
            SepaFileGenerator sepaFileGenerator = new SepaFileGenerator();

            // Determine which invoices that lead to validation errors in the SEPA file
            List<String> invalidInvoices = determineInvalidInvoices(document, fileToCreate, invoices, collectionDate,
                    settings, idToParty, idToPartyAutomaticCollectionSettings, sepaFileGenerator, progressListener);

            if (!invalidInvoices.isEmpty()) {
                fileToCreate.delete();
                throw new ServiceException("Invoices with incomplete or incorrect data: " + String.join(", ", invalidInvoices));
            }

            progressListener.onProgressUpdate(100);

            // Generate the SEPA file for all invoices
            sepaFileGenerator.generate(document, settings, invoices, fileToCreate, collectionDate,
                    idToParty, idToPartyAutomaticCollectionSettings);

            // Increase sequence number for next SEPA file
            settings.setSequenceNumber(settings.getSequenceNumber() + 1);
            setSettings(document, settings);
        });
    }

    private List<String> determineInvalidInvoices(Document document, File fileToCreate, List<Invoice> invoices, Date collectionDate, AutomaticCollectionSettings settings, Map<String, Party> idToParty, Map<String, PartyAutomaticCollectionSettings> idToPartyAutomaticCollectionSettings, SepaFileGenerator sepaFileGenerator, TaskProgressListener progressListener) throws Exception {
        List<String> invalidInvoices = new ArrayList<String>();
        for (int i=0; i<invoices.size(); i++) {
            progressListener.onProgressUpdate((i+1) * 100 / invoices.size());
            try {
                sepaFileGenerator.generate(document, settings, invoices.subList(i, i+1), fileToCreate, collectionDate,
                        idToParty, idToPartyAutomaticCollectionSettings);
                validateSepaAutomaticCollectionFile(fileToCreate);
            } catch (ServiceException e) {
                invalidInvoices.add(invoices.get(i).getId() + " (" + idToParty.get(invoices.get(i).getConcerningPartyId())
                        + "): " + e.getMessage());
            }
        }
        return invalidInvoices;
    }

    public void validateSepaAutomaticCollectionFile(File sepaFile) throws ServiceException {
        try {
            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try (InputStream xsdStream = getClass().getResourceAsStream("/sepa/pain.008.001.02.xsd")) {
                Schema schema = factory.newSchema(new StreamSource(xsdStream));
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(sepaFile));
            }
        } catch (Exception e) {
            throw new ServiceException("SEPA file " + sepaFile.getAbsolutePath() + " is not valid: " + e.getMessage());
        }
    }

    public void createJournalEntryForAutomaticCollection(Document document, Date collectionDate, String journalEntryId,
                                                         String journalEntryDescription, List<Invoice> invoices,
                                                         Account bankAccount, Account debtorAccount) throws ServiceException {
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
        ObjectFactory.create(LedgerService.class).createJournalEntry(document, journalEntry, details);
    }
}
