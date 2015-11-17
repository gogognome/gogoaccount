package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;

import java.io.File;
import java.util.Date;
import java.util.List;

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

    public void createSepaAutomaticCollectionFile(Document document, File fileToCreate, List<Invoice> invoices, Date collectionDate)
            throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            AutomaticCollectionSettings settings = getSettings(document);
            new SepaFileGenerator().generate(document, settings, invoices, fileToCreate, collectionDate);

            settings.setSequenceNumber(settings.getSequenceNumber() + 1);
            setSettings(document, settings);
        });
    }
}
