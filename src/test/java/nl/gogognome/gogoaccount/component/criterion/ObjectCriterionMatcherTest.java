package nl.gogognome.gogoaccount.component.criterion;

import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.mockito.VarArgsMatcher;
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
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public void convertsParametersToStrings() {
        assertConvertsParameters(emptyList(), emptyList());
        assertConvertsParameters(asList("bla"), asList("bla"));
        assertConvertsParameters(asList("foo", "bar"), asList("foo", "bar"));
        assertConvertsParameters(asList(123, 5.6), asList("123", "5.6"));
        assertConvertsParameters(asList(DateUtil.createDate(2016, 7, 16)), asList("20160716", "2016-07-16", "16072016", "16-07-2016"));
    }

    private void assertConvertsParameters(List<Object> inputArguments, List<String> expectedArgumens) {
        CriterionMatcher criterionMatcher = mock(CriterionMatcher.class);
        ObjectCriterionMatcher matcher = new ObjectCriterionMatcher(criterionMatcher);
        matcher.matches(criterion, inputArguments.toArray(new Object[inputArguments.size()]));
        verify(criterionMatcher).matches(any(), VarArgsMatcher.varArgEquals(expectedArgumens));
    }
}