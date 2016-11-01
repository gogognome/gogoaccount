package nl.gogognome.gogoaccount.component.text;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;

public class KeyValueReplacerTest {

    private KeyValueReplacer keyValueReplacer = new KeyValueReplacer();

    @Test(expected = IllegalArgumentException.class)
    public void whenTextIsNullAnExceptionIsThrown() {
        keyValueReplacer.applyReplacements(null, emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenReplacementsMapIsNullAnExceptionIsThrown() {
        keyValueReplacer.applyReplacements("some text", null);
    }

    @Test
    public void whenNoReplacementsSpecifiedThenTextIsReturnedUnmodified() {
        assertEquals("foo bar", keyValueReplacer.applyReplacements("foo bar", emptyMap()));
    }

    @Test
    public void whenReplacementKeyDoesNotOccurInTextThenTextIsReturnedUnmodified() {
        assertEquals("foo bar", keyValueReplacer.applyReplacements("foo bar", ImmutableMap.of("bla", () -> "BLA")));
    }

    @Test
    public void whenReplacementKeyOccursOnceInTextThenKeyIsReplaced() {
        assertEquals("FOO bar", keyValueReplacer.applyReplacements("foo bar", ImmutableMap.of("foo", () -> "FOO")));
    }

    @Test
    public void whenReplacementKeyOccursMultipleTimesInTextThenAllKeyOccurrencesAreReplaced() {
        assertEquals("FOO bar FOObar FOOFOO", keyValueReplacer.applyReplacements("foo bar foobar foofoo", ImmutableMap.of("foo", () -> "FOO")));
    }

    @Test
    public void whenMultipleReplacementKeysOccurInTextThenAllReplacementsAreApplied() {
        assertEquals("FOO BAR FOOBAR", keyValueReplacer.applyReplacements("foo bar foobar", ImmutableMap.of("foo", () -> "FOO", "bar", () -> "BAR")));
    }


}