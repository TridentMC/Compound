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

package com.tridevmc.compound.network.marshallers;

/**
 * Stores information about a marshaller, used for registration of default marshallers.
 * <p>
 * For internal use only.
 */
public class MarshallerMetadata {

    public String[] ids;
    public Marshaller marshaller;
    public Class[] acceptedTypes;

    public MarshallerMetadata(String[] ids, Marshaller marshaller, Class[] acceptedTypes) {
        this.ids = ids;
        this.marshaller = marshaller;
        this.acceptedTypes = acceptedTypes;
    }

}
