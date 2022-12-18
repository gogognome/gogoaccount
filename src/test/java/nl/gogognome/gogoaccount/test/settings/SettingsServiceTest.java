package nl.gogognome.gogoaccount.test.settings;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import nl.gogognome.gogoaccount.component.settings.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.gogoaccount.test.*;

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