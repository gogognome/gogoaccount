package nl.gogognome.gogoaccount.component.text;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.google.common.collect.*;

public class KeyValueReplacerTest {

    private final KeyValueReplacer keyValueReplacer = new KeyValueReplacer();

    @Test
    public void whenTextIsNullAnExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () -> keyValueReplacer.applyReplacements(null, emptyMap()));
    }

    @Test
    public void whenReplacementsMapIsNullAnExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () -> keyValueReplacer.applyReplacements("some text", null));
    }

    @Test
    public void whenNoReplacementsSpecifiedThenTextIsReturnedUnmodified() {
        assertEquals("foo bar", keyValueReplacer.applyReplacements("foo bar", emptyMap()));
    }

    @Test
    public void whenReplacementKeyDoesNotOccurInTextThenTextIsReturnedUnmodified() {
        assertEquals("foo bar", keyValueReplacer.applyReplacements("foo bar", ImmutableMap.of("bla", "BLA")));
    }

    @Test
    public void whenReplacementKeyOccursOnceInTextThenKeyIsReplaced() {
        assertEquals("FOO bar", keyValueReplacer.applyReplacements("foo bar", ImmutableMap.of("foo", "FOO")));
    }

    @Test
    public void whenReplacementKeyOccursMultipleTimesInTextThenAllKeyOccurrencesAreReplaced() {
        assertEquals("FOO bar FOObar FOOFOO", keyValueReplacer.applyReplacements("foo bar foobar foofoo", ImmutableMap.of("foo", "FOO")));
    }

    @Test
    public void whenMultipleReplacementKeysOccurInTextThenAllReplacementsAreApplied() {
        assertEquals("FOO BAR FOOBAR", keyValueReplacer.applyReplacements("foo bar foobar", ImmutableMap.of("foo", "FOO", "bar", "BAR")));
    }

    @Test
    public void whenEscapeFunctionIsSpecifiedItIsAppliedToReplacementText() {
        assertEquals("'FOO' bar", keyValueReplacer.applyReplacements("foo bar", ImmutableMap.of("foo", "FOO"), s -> "'" + s + "'"));
    }

}