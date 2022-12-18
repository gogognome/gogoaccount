package nl.gogognome.gogoaccount.component.criterion;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import java.util.*;
import org.junit.jupiter.api.*;
import nl.gogognome.lib.util.*;
import nl.gogognome.textsearch.criteria.*;
import nl.gogognome.textsearch.string.*;

public class ObjectCriterionMatcherTest {

    private final Criterion criterion = new StringLiteral("foobar");

    @Test
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public void convertsParametersToStrings() {
        assertConvertsParameters(emptyList(), emptyList());
        assertConvertsParameters(asList("bla"), asList("bla"));
        assertConvertsParameters(asList("foo", "bar"), asList("foo", "bar"));
        assertConvertsParameters(asList(123, 5.6), asList("123", "5.6"));
        assertConvertsParameters(asList(DateUtil.createDate(2016, 7, 16)), asList("20160716", "2016-07-16", "16072016", "16-07-2016"));
    }

    private void assertConvertsParameters(List<Object> inputArguments, List<String> expectedArgumens) {
        CriterionMatcherSpy criterionMatcher = new CriterionMatcherSpy(new StringSearchFactory().caseInsensitiveStringSearch());
        ObjectCriterionMatcher matcher = new ObjectCriterionMatcher(criterionMatcher);
        matcher.matches(criterion, inputArguments.toArray(new Object[0]));
        Assertions.assertEquals(expectedArgumens, criterionMatcher.getTextElements());
    }

    private static class CriterionMatcherSpy extends CriterionMatcher {

        private List<String> textElements;

        public CriterionMatcherSpy(StringSearch stringSearch) {
            super(stringSearch);
        }

        @Override
        public boolean matches(Criterion criterion, String... textElements) {
            this.textElements = Arrays.asList(textElements);
            return super.matches(criterion, textElements);
        }

        public List<String> getTextElements() {
            return textElements;
        }
    }
}