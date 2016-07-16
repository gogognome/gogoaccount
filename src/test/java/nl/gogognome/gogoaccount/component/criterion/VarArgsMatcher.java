package nl.gogognome.gogoaccount.component.criterion;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.VarargMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.mockito.Matchers.argThat;

public class VarArgsMatcher<T> extends ArgumentMatcher<T[]> implements VarargMatcher {

    private final Predicate<List<T>> predicate;

    public VarArgsMatcher(Predicate<List<T>> predicate) {
        this.predicate = predicate;
    }

    public static <T> T[] varArg(Predicate<List<T>> predicate) {
        argThat(new VarArgsMatcher<T>(predicate));
        return null;
    }

    public static <T> T[] varArgEquals(List<T> expectedArguments) {
        return varArg(actualArguments -> expectedArguments.equals(actualArguments));
    }

    @Override
    public boolean matches(Object argument) {
        if (argument != null && !argument.getClass().isArray()) {
            return predicate.test(Collections.singletonList((T) argument));
        }
        return predicate.test(Arrays.asList((T[]) argument));
    }
}
