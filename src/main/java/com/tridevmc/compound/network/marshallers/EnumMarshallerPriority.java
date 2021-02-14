/*
 * Copyright 2018 - 2021 TridentMC
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
 * An enum to specify the priority for a marshaller to be used, HIGH means it will be more likely to
 * be used for a field, LOW means it is less likely to be used for a field.
 */
public enum EnumMarshallerPriority {
    HIGH(1),
    NORMAL(0),
    LOW(-1);

    int rank = 0;

    EnumMarshallerPriority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return this.rank;
    }
}
