package nl.gogognome.gogoaccount.component.text;

import java.util.Map;
import java.util.function.Function;

public class KeyValueReplacer {

    /**
     * Applies replacements to a text. Each of the keys specified in the replacements map is searched for in the text.
     * Each occurrence is replaced by the value of the corresponding supplier.
     *
     * <p>Ensure that no replacement key is a prefix of another key. For example, the keys "foo" and "foobar"
     * must be omitted in the replacements, because this method makes no guarantee about the order in which the
     * replacements take place.
     *
     * @param text the text
     * @param replacements map with replacements to be applied to the text
     * @return the text with replacements applied
     */
    public String applyReplacements(String text, Map<String, String> replacements) {
        return applyReplacements(text, replacements, Function.identity());
    }

    /**
     * Applies replacements to a text. Each of the keys specified in the replacements map is searched for in the text.
     * Each occurrence is replaced by the value of the corresponding supplier.
     *
     * <p>Ensure that no replacement key is a prefix of another key. For example, the keys "foo" and "foobar"
     * must be omitted in the replacements, because this method makes no guarantee about the order in which the
     * replacements take place.
     *
     * @param text the text
     * @param replacements map with replacements to be applied to the text
     * @param escapeFunction function applied to each replacement string. Can be used to escape special characters
     * @return the text with replacements applied
     */
    public String applyReplacements(String text, Map<String, String> replacements, Function<String, String> escapeFunction) {
        validateParameters(text, replacements);

        StringBuilder result = new StringBuilder(text);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            applyReplacement(result, entry.getKey(), escapeFunction.apply(entry.getValue()));
        }
        return result.toString();
    }

    private void validateParameters(String text, Map<String, String> replacements) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (replacements == null) {
            throw new IllegalArgumentException("replacements must not be null");
        }
    }

    private void applyReplacement(StringBuilder result, String key, String value) {
        int index;
        do {
            index = result.indexOf(key);
            if (index != -1) {
                result.replace(index, index + key.length(), value);
            }
        } while (index != -1);
    }
}
