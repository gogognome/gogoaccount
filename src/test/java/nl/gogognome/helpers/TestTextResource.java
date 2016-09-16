package nl.gogognome.helpers;

import nl.gogognome.lib.text.TextResource;

import java.util.Arrays;
import java.util.Locale;

public class TestTextResource extends TextResource {

    public TestTextResource() {
        super(Locale.US);
    }

    @Override
    public String getString(String id) {
        return "resource-id: " + id;
    }

    @Override
    public String getString(String id, Object... arguments) {
        return "resource-id: " + id + "-" + Arrays.toString(arguments);
    }
}
