/*
 * Copyright 2018 - 2024 TridentMC
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

package com.tridevmc.compound.ui.state;

import java.util.function.Function;

/**
 * Observable value used by composition, layout, and draw bindings.
 */
public interface State<T> {

    /**
     * Create a new State with the given initial value.
     *
     * @param initialValue the initial value
     * @param <T>          the type of the value
     * @return a new State instance
     */
    static <T> State<T> of(T initialValue) {
        return new StateImpl<>(initialValue);
    }

    /**
     * Get current value.
     *
     * @return the current value
     */
    T get();

    /**
     * Set new value (triggers observers).
     *
     * @param value the new value
     */
    void set(T value);

    /**
     * Update value using current value.
     *
     * @param updater function to compute new value from current value
     */
    void update(Function<T, T> updater);

    /**
     * Internal: Register observer (used by framework).
     *
     * @param observer the observer to register
     */
    void addObserver(StateObserver observer);

    /**
     * Internal: Remove observer (used by framework).
     *
     * @param observer the observer to remove
     */
    void removeObserver(StateObserver observer);

    /**
     * Cleanup.
     */
    void dispose();
}
