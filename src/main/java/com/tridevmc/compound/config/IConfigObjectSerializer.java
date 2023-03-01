/*
 * Copyright 2018 - 2022 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.config;

import org.jetbrains.annotations.NotNull;

/**
 * Used to define custom serialization steps for objects used in configs.
 * Register with {@link RegisteredConfigObjectSerializer}
 *
 * @param <T> the type of object to serialize.
 */
public interface IConfigObjectSerializer<T> extends IConfigFieldSerializer<T> {

    /**
     * Convert the given value to a string to be de-serialized later.
     *
     * @param fieldType the type of field the value is stored in.
     * @param value     the value to convert to a string.
     * @return the value converted to a string.
     */
    String toString(Class<T> fieldType, T value);

    /**
     * Convert the given string back into an object.
     *
     * @param fieldType the type of field the value is stored in.
     * @param value     the previously serialized object to deserialize.
     * @return the de-serialized object.
     */
    T fromString(Class<T> fieldType, String value);

    /**
     * Determines if the given class can be serialized by this serializer.
     *
     * @param clazz the class to check against.
     * @return true if the class can be serialized by this serializer, false otherwise.
     */
    boolean accepts(Class<?> clazz);

    @Override
    default String toString(@NotNull ConfigField<T> field, T value) {
        return toString(field.getFieldType(), value);
    }

    @Override
    default T fromString(ConfigField<T> field, String value) {
        return fromString(field.getFieldType(), value);
    }

    @Override
    default boolean accepts(ConfigField<T> field) {
        return accepts(field.getFieldType());
    }

}
