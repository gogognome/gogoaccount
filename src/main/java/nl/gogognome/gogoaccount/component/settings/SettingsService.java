package nl.gogognome.gogoaccount.component.settings;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;

public class SettingsService {

    public void save(Document document, String key, String value) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            document.ensureDocumentIsWriteable();
            Setting setting = new Setting(key);
            setting.setValue(value);
            SettingsDAO settingsDAO = new SettingsDAO(document);
            if (settingsDAO.exists(key)) {
                settingsDAO.update(setting);
            } else {
                settingsDAO.create(setting);
            }
        });
    }

    public String findValueForSetting(Document document, String key) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            Setting setting = new SettingsDAO(document).find(key);
            return setting != null ? setting.getValue() : null;
        });
    }
}
