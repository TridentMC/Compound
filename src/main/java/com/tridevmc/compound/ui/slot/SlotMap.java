/*
 * Copyright 2018 - 2026 TridentMC
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

package com.tridevmc.compound.ui.slot;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

/**
 * Storage for slot content (internal to framework).
 */
public class SlotMap {
    private final Map<SlotKey, SlotContent> slots = Maps.newHashMap();

    /**
     * Store content for a slot.
     *
     * @param key     the slot key
     * @param content the slot content
     */
    public void put(SlotKey key, SlotContent content) {
        this.slots.put(key, content);
    }

    /**
     * Get content for a slot.
     *
     * @param key the slot key
     * @return the slot content, or null if not present
     */
    public @Nullable SlotContent get(SlotKey key) {
        return this.slots.get(key);
    }

    /**
     * Check if slot is filled.
     *
     * @param key the slot key
     * @return true if the slot has content
     */
    public boolean has(SlotKey key) {
        return this.slots.containsKey(key);
    }

    /**
     * Clear all slots.
     */
    public void clear() {
        this.slots.clear();
    }
}
