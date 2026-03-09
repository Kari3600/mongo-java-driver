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
import org.bson.BsonWriter;

import java.util.Map;

/**
 * A Codec for Map instances.
 *
 * @since 3.5
 */
class ParameterizedMapCodec<K, V, M extends Map<K, V>> extends AbstractMapCodec<K, V, M> {
    private final Codec<V> codec;

    ParameterizedMapCodec(final Codec<V> codec, final Class<K> keyClass, final Class<M> mapClass) {
        super(keyClass, mapClass);
        this.codec = codec;
    }

    @Override
    V readValue(final BsonReader reader, final DecoderContext decoderContext) {
        return decoderContext.decodeWithChildContext(codec, reader);
    }

    @Override
    void writeValue(final BsonWriter writer, final V value, final EncoderContext encoderContext) {
        encoderContext.encodeWithChildContext(codec, writer, value);
    }
}
