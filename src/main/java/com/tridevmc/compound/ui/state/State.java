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

package com.tridevmc.compound.ui.state;

import java.util.function.Function;

/**
 * Observable value used by composition and layout bindings, or read directly while drawing.
 * Implementations may update immediately or over time; animated state reports intermediate values.
 *
 * @param <T> the stored value type
 */
public interface State<T> {

    /**
     * Creates an immediately updated observable value. Null values are allowed.
     * Observers are notified synchronously only when {@link java.util.Objects#equals}
     * considers the new value different; mutating an object in place does not notify observers.
     *
     * @param initialValue the initial value, which may be null
     * @param <T>          the type of the value
     * @return a new independently observable state
     */
    static <T> State<T> of(T initialValue) {
        return new StateImpl<>(initialValue);
    }

    /**
     * Gets the current value. Animated implementations may return an intermediate value.
     *
     * @return the current value
     */
    T get();

    /**
     * Requests a new value. Immediate states apply it synchronously; animated states
     * may treat it as a target and notify observers as the current value changes.
     *
     * @param value the new value
     */
    void set(T value);

    /**
     * Computes a requested value from the current value and passes it to {@link #set}.
     *
     * @param updater the function computing the requested value; it receives null when the current value is null
     */
    void update(Function<T, T> updater);

    /**
     * Registers an observer for value changes. Repeated registration of the same observer
     * has no additional effect in the built-in implementations.
     *
     * @param observer the observer to register
     */
    void addObserver(StateObserver observer);

    /**
     * Removes an observer. Removing an unregistered observer has no effect.
     *
     * @param observer the observer to remove
     */
    void removeObserver(StateObserver observer);

    /**
     * Removes observers and releases implementation-owned update resources.
     * For immediate states this clears subscriptions; animated states also leave their scheduler.
     */
    void dispose();
}
