package nl.gogognome.gogoaccount.util;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

public class ObjectFactory {

    private static Map<Class<?>, Object> registredMocks = new HashMap<>();
    private final static Map<Class<?>, Constructor<?>> CLASS_TO_CONSTRUCTOR_WITHOUT_PARAMETERS = new ConcurrentHashMap<Class<?>, Constructor<?>>(100);
    private final static Map<Class<?>, Supplier<?>> CLASS_TO_CONSTRUCTOR_FUNCTION = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> requestedClass) {
        T instance = null;

        try {
            if (registredMocks.containsKey(requestedClass)) {
                instance = (T) registredMocks.get(requestedClass);
            } else if(CLASS_TO_CONSTRUCTOR_FUNCTION.keySet().contains(requestedClass)) {
                instance = (T) CLASS_TO_CONSTRUCTOR_FUNCTION.get(requestedClass).get();
            }
            else {
                Constructor<T> constructor = getConstructorWithoutParameters(requestedClass);
                instance = constructor.newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return instance;
    }

    private static <T> Constructor<T> getConstructorWithoutParameters(Class<T> requestedClass) {
        ensureConstructorWithoutParametersIsInMap(requestedClass);

        @SuppressWarnings("unchecked")
        Constructor<T> constructor = (Constructor<T>) CLASS_TO_CONSTRUCTOR_WITHOUT_PARAMETERS.get(requestedClass);
        return constructor;
    }

    private static <T> void ensureConstructorWithoutParametersIsInMap(Class<T> requestedClass) {
        if (CLASS_TO_CONSTRUCTOR_WITHOUT_PARAMETERS.containsKey(requestedClass)) {
            return;
        }

        for (Constructor<?> c : requestedClass.getConstructors()) {
            if (c.getParameterTypes().length == 0) {
                CLASS_TO_CONSTRUCTOR_WITHOUT_PARAMETERS.put(requestedClass, c);
            }
        }
    }

    public static <T> void registerMock(Class<T> requestedClass, Object mock) {
        registredMocks.put(requestedClass, mock);
    }

    public static void clearRegisteredMocks() {
        registredMocks.clear();
    }

    public static <T> void registerConstructor(Class<T> requestedClass, Supplier<T> supplier) {
        CLASS_TO_CONSTRUCTOR_FUNCTION.put(requestedClass, supplier);
    }

    public static void clearRegistedConstructors() {
        CLASS_TO_CONSTRUCTOR_FUNCTION.clear();
    }

}