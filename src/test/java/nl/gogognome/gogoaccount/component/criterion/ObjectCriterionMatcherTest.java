package nl.gogognome.gogoaccount.component.criterion;

import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.string.CriterionMatcher;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;

public class ObjectCriterionMatcherTest {

    private Criterion criterion = mock(Criterion.class);

    @Test
    public void convertsParametersToStrings() {
        assertConvertsParameters(emptyList(), emptyList());
        assertConvertsParameters(asList("bla"), asList("bla"));
        assertConvertsParameters(asList("foo", "bar"), asList("foo", "bar"));
        assertConvertsParameters(asList(123, 5.6), asList("123", "5.6"));
    }

    private void assertConvertsParameters(List<Object> inputArguments, List<String> expectedArgumens) {
        CriterionMatcher criterionMatcher = mock(CriterionMatcher.class);
        ObjectCriterionMatcher matcher = new ObjectCriterionMatcher(criterionMatcher);
        matcher.matches(criterion, inputArguments.toArray(new Object[inputArguments.size()]));
        verify(criterionMatcher).matches(any(), VarArgsMatcher.varArgEquals(expectedArgumens.toArray(new String[expectedArgumens.size()])));
    }
}