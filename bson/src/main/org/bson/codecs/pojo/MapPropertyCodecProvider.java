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
package org.bson.codecs.pojo;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.Translator;
import org.bson.codecs.configuration.CodecConfigurationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.bson.internal.EnumCodecHelper.getEnumMap;

final class MapPropertyCodecProvider implements PropertyCodecProvider {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T> Codec<T> get(final TypeWithTypeParameters<T> type, final PropertyCodecRegistry registry) {
        if (Map.class.isAssignableFrom(type.getType()) && type.getTypeParameters().size() == 2) {
            Class<?> keyType = type.getTypeParameters().get(0).getType();
            try {
                return new MapCodec(keyType, type.getType(), registry.get(type.getTypeParameters().get(1)));
            } catch (CodecConfigurationException e) {
                if (type.getTypeParameters().get(1).getType() == Object.class) {
                    try {
                        return (Codec<T>) registry.get(TypeData.builder(Map.class).build());
                    } catch (CodecConfigurationException e1) {
                        // Ignore and return original exception
                    }
                }
                throw e;
            }
        } else {
            return null;
        }
    }

    private static class MapCodec<K, V> implements Codec<Map<K, V>> {
        private final Class<K> keyClass;
        private final Class<Map<K, V>> encoderClass;
        private final Codec<V> codec;
        private final Translator<K, String> translator;
        private final Supplier<Map<K, V>> supplier;

        MapCodec(final Class<K> keyClass, final Class<Map<K, V>> encoderClass, final Codec<V> codec) {
            this.keyClass = keyClass;
            this.encoderClass = encoderClass;
            this.codec = codec;
            this.translator = getTranslator();
            this.supplier = getSupplier();
        }

        @Override
        public void encode(final BsonWriter writer, final Map<K, V> map, final EncoderContext encoderContext) {
            writer.writeStartDocument();
            for (final Entry<K, V> entry : map.entrySet()) {
                writer.writeName(translator.encode(entry.getKey()));
                if (entry.getValue() == null) {
                    writer.writeNull();
                } else {
                    codec.encode(writer, entry.getValue(), encoderContext);
                }
            }
            writer.writeEndDocument();
        }

        @Override
        public Map<K, V> decode(final BsonReader reader, final DecoderContext context) {
            reader.readStartDocument();
            Map<K, V> map = supplier.get();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                K key = translator.decode(reader.readName());
                if (reader.getCurrentBsonType() == BsonType.NULL) {
                    map.put(key, null);
                    reader.readNull();
                } else {
                    map.put(key, codec.decode(reader, context));
                }
            }
            reader.readEndDocument();
            return map;
        }

        @Override
        public Class<Map<K, V>> getEncoderClass() {
            return encoderClass;
        }

        private Supplier<Map<K, V>> getSupplier() {
            if (encoderClass.isInterface()) {
                return () -> new HashMap<>();
            }
            if (EnumMap.class.isAssignableFrom(encoderClass)) {
                return () -> (Map<K, V>) getEnumMap(keyClass);
            }
            try {
                Constructor<? extends Map<K, V>> constructor = encoderClass.getDeclaredConstructor();
                return () -> {
                    try {
                        return (Map<K, V>) constructor.newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new CodecConfigurationException("Can not invoke no-args constructor for Map class %s", e);
                    }
                };
            } catch (NoSuchMethodException e) {
                return  () -> {
                    throw new CodecConfigurationException(format("Map class %s has no public no-args constructor", encoderClass), e);
                };
            }
        }

        private Translator<K, String> getTranslator() {
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
            } else if (keyClass.isEnum()) {
                return new Translator<K, String>() {
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
                throw new CodecConfigurationException(format("Illegal map key class %s.", keyClass));
            }
        }
    }
}
