package nl.gogognome.gogoaccount.component.settings;

class Setting {

    private final String key;
    private String value;

    Setting(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
