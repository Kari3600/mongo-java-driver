/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.configuration.CodecConfigurationException;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.bson.assertions.Assertions.notNull;
import static org.bson.internal.EnumCodecHelper.getEnumMap;

abstract class AbstractMapCodec<K, V, M extends Map<K, V>> implements Codec<M> {

    private final Supplier<M> supplier;
    private final Class<K> keyClass;
    private final Class<M> mapClass;
    private final Translator<K, String> translator;

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable", "rawtypes"})
    AbstractMapCodec(@Nullable final Class<K> keyClass, @Nullable final Class<M> mapClass) {
        this.mapClass = notNull("mapClass", mapClass);
        this.keyClass = notNull("keyClass", keyClass);
        Class rawClass = mapClass;
        if (rawClass == Map.class || rawClass == AbstractMap.class || rawClass == HashMap.class) {
            supplier = () -> (M) new HashMap<>();
            translator = getAutoTranslator(keyClass);
        } else if (rawClass == NavigableMap.class || rawClass == TreeMap.class) {
            supplier = () -> (M) new TreeMap<>();
            translator = getAutoTranslator(keyClass);
        } else if (rawClass == EnumMap.class && Enum.class.isAssignableFrom(keyClass)) {
            supplier = () -> (M) getEnumMap(keyClass);
            translator = new Translator<K, String>() {
                @Override
                public String encode(final K input) {
                    return ((Enum) input).name();
                }
                @Override
                public K decode(final String output) {
                    return (K) Enum.valueOf((Class<? extends Enum>) keyClass, output);
                }
            };
        } else {
            Constructor<? extends Map<?, ?>> constructor;
            Supplier<M> supplier;
            try {
                constructor = mapClass.getDeclaredConstructor();
                supplier = () -> {
                    try {
                        return (M) constructor.newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new CodecConfigurationException("Can not invoke no-args constructor for Map class %s", e);
                    }
                };
            } catch (NoSuchMethodException e) {
                supplier = () -> {
                    throw new CodecConfigurationException(format("Map class %s has no public no-args constructor", mapClass), e);
                };
            }
            this.supplier = supplier;
            translator = getAutoTranslator(keyClass);
        }
    }

    @SuppressWarnings("unchecked")
    private Translator<K, String> getAutoTranslator(final Class<K> keyClass) {
        if (keyClass == String.class) {
            return (Translator<K, String>) Translator.<String>identity();
        } else if (keyClass == Integer.class || keyClass == int.class) {
            return new Translator<K, String>() {
                @Override
                public String encode(final K input) {
                    return String.valueOf(input);
                }
                @Override
                public K decode(final String output) {
                    return (K) Integer.valueOf(output);
                }
            };
        } else if (keyClass == Long.class || keyClass == long.class) {
            return new Translator<K, String>() {
                @Override
                public String encode(final K input) {
                    return String.valueOf(input);
                }
                @Override
                public K decode(final String output) {
                    return (K) Long.valueOf(output);
                }
            };
        } else {
            throw new CodecConfigurationException(format("Illegal map key class %s.", keyClass));
        }
    }

    abstract V readValue(BsonReader reader, DecoderContext decoderContext);

    abstract void writeValue(BsonWriter writer, V value, EncoderContext encoderContext);

    @Override
    public void encode(final BsonWriter writer, final M map, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        for (final Map.Entry<K, V> entry : map.entrySet()) {
            writer.writeName(translator.encode(entry.getKey()));
            V value = entry.getValue();
            if (value == null) {
                writer.writeNull();
            } else {
                writeValue(writer, value, encoderContext);
            }
        }
        writer.writeEndDocument();
    }


    @Override
    public M decode(final BsonReader reader, final DecoderContext decoderContext) {
        M map = supplier.get();

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            K keyName = translator.decode(fieldName);
            if (reader.getCurrentBsonType() == BsonType.NULL) {
                reader.readNull();
                map.put(keyName, null);
            } else {
                map.put(keyName, readValue(reader, decoderContext));
            }
        }

        reader.readEndDocument();
        return map;
    }

    public Class<K> getKeyClass() {
        return keyClass;
    }

    @Override
    public Class<M> getEncoderClass() {
        return mapClass;
    }
}
