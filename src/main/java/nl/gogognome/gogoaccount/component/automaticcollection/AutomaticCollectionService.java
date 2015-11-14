package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;

public class AutomaticCollectionService {

    public AutomaticCollectionSettings getSettings(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> new AutomaticCollectionSettingsDAO(document).getSettings());
    }

    public void setSettings(Document document, AutomaticCollectionSettings settings) throws ServiceException {
        ServiceTransaction.withoutResult(() -> new AutomaticCollectionSettingsDAO(document).setSettings(settings));
    }
}
