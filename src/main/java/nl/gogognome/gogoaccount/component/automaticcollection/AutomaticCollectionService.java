package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.gogoaccount.util.ObjectFactory;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

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

    public void createSepaAutomaticCollectionFile(Document document, File fileToCreate, List<Invoice> invoices, Date collectionDate,
                                                  String description) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            AutomaticCollectionSettings settings = getSettings(document);
            PartyService partyService = ObjectFactory.create(PartyService.class);
            List<String> partyIds = invoices.stream().map(i -> i.getConcerningPartyId()).collect(toList());
            new SepaFileGenerator().generate(document, settings, invoices, fileToCreate, collectionDate,
                    partyService.getIdToParty(document, partyIds),
                    new PartyAutomaticCollectionSettingsDAO(document).getIdToParty(partyIds),
                    description);

            settings.setSequenceNumber(settings.getSequenceNumber() + 1);
            setSettings(document, settings);
        });
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
}
