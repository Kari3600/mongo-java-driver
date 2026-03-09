package org.bson.internal;

import java.util.EnumMap;

public class EnumCodecHelper {
    @SuppressWarnings("unchecked")
    public static <K, V, T extends Enum<T>> EnumMap<T, V> getEnumMap(final Class<K> keyClass) {
        return new EnumMap<>((Class<T>) keyClass);
    }
}
