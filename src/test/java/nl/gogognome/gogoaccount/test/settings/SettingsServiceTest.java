package nl.gogognome.gogoaccount.test.settings;

import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.test.AbstractBookkeepingTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class SettingsServiceTest extends AbstractBookkeepingTest {

    private final SettingsService settingsService = new SettingsService();

    @Test
    public void whenSettingDoesNotExistThenGettingValueReturnsNull() throws ServiceException {
        assertNull(settingsService.findValueForSetting(document, "someKey"));
    }

    @Test
    public void whenSettingDoesExistThenGettingValueReturnsValue() throws ServiceException {
        settingsService.save(document, "someKey", "some value");
        assertEquals("some value", settingsService.findValueForSetting(document, "someKey"));
    }

    @Test
    public void settingValueNullIsAllowed() throws ServiceException {
        settingsService.save(document, "someKey", null);
        assertNull(settingsService.findValueForSetting(document, "someKey"));
    }

    @Test
    public void nullAsKeyIsNotAllowed() throws ServiceException {
        ServiceException exception = assertThrows(ServiceException.class, () -> settingsService.save(document, null, "some value"));
        assertTrue(exception.getMessage().contains("id must not be null"));
    }

    @Test
    public void whenSettingIsUpdatedThenGettingValueReturnsValue() throws ServiceException {
        settingsService.save(document, "someKey", "some value");
        settingsService.save(document, "someKey", "some new value");
        assertEquals("some new value", settingsService.findValueForSetting(document, "someKey"));
    }
}