package nl.gogognome.gogoaccount.component.settings;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

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

    public Map<String, String> findAllSettings(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() ->
                new SettingsDAO(document).findAll()
                        .stream()
                        .collect(toMap(Setting::getKey, Setting::getValue)));
    }

    public String findNextId(Document document, String key, String format) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            SettingsDAO settingsDAO = new SettingsDAO(document);
            Setting previousId = settingsDAO.find(key);
            String nextId = new FormattedIdGenerator().findNextId(document, previousId != null ? previousId.getValue() : null, format);
            settingsDAO.save(new Setting(key, nextId));
            return nextId;
        });
    }
}
