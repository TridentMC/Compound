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

public interface IConfigFieldSerializer<T> {

    /**
     * Convert the given value to a string to be de-serialized later.
     *
     * @param field the field the value is stored in.
     * @param value the value to convert to a string.
     * @return the value converted to a string.
     */
    String toString(ConfigField<T> field, T value);

    /**
     * Convert the given string back into an object.
     *
     * @param field the field the value is stored in.
     * @param value the previously serialized object to deserialize.
     * @return the de-serialized object.
     */
    T fromString(ConfigField<T> field, String value);

    /**
     * Determines if the given field can be serialized by this serializer.
     *
     * @param field the field to check against.
     * @return true if the field can be serialized by this serializer, false otherwise.
     */
    boolean accepts(ConfigField<T> field);

}
