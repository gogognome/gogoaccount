package nl.gogognome.gogoaccount.component.criterion;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.VarargMatcher;

import java.util.Arrays;
import java.util.function.Predicate;

import static org.mockito.Matchers.argThat;

public class VarArgsMatcher<T> extends ArgumentMatcher<T[]> implements VarargMatcher {

    private final Predicate<T[]> predicate;

    public VarArgsMatcher(Predicate<T[]> predicate) {
        this.predicate = predicate;
    }

    public static <T> T[] varArg(Predicate<T[]> predicate) {
        argThat(new VarArgsMatcher<T>(predicate));
        return null;
    }

    public static <T> T[] varArgEquals(T... expectedArguments) {
        return varArg(actualArguments -> Arrays.equals(expectedArguments, actualArguments));
    }

    @Override
    public boolean matches(Object argument) {
        if (argument != null && !argument.getClass().isArray()) {
            return predicate.test((T[]) new Object[] {(T) argument});
        }
        return predicate.test((T[]) argument);
    }
}
