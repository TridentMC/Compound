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

import net.minecraft.resources.ResourceLocation;

public class ForgeRegistryEntrySerializer<T> implements IConfigFieldSerializer<T> {

    @Override
    public String toString(ConfigField<T> field, T value) {
        if (field.getRegistry() == null) {
            throw new NullPointerException("Unable to find valid registry with name " + field.getRegistryName().toString());
        } else {
            var key = field.getRegistry().getKey(value);
            if (key == null) {
                throw new NullPointerException("Unable to find valid key for value " + value.toString());
            } else {
                return key.toString();
            }
        }
    }

    @Override
    public T fromString(ConfigField<T> field, String value) {
        if (field.getRegistry() == null) {
            throw new NullPointerException("Unable to find valid registry with name " + field.getRegistryName().toString());
        } else {
            T registeredValue = field.getRegistry().get(new ResourceLocation(value));
            if (registeredValue == null) {
                throw new NullPointerException("Unable to find valid value for key " + value);
            } else {
                return registeredValue;
            }
        }
    }

    @Override
    public boolean accepts(ConfigField<T> field) {
        return field.getRegistryName() != null;
    }

}
