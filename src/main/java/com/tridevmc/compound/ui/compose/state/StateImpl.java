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

package com.tridevmc.compound.ui.compose.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Implementation of State that manages a single value and notifies observers on change.
 */
public class StateImpl<T> implements State<T> {
    private T value;
    private final List<StateObserver> observers = new ArrayList<>();

    public StateImpl(T initialValue) {
        this.value = initialValue;
    }

    @Override
    public T get() {
        return this.value;
    }

    @Override
    public void set(T value) {
        if (!Objects.equals(this.value, value)) {
            this.value = value;
            this.notifyObservers();
        }
    }

    @Override
    public void update(Function<T, T> updater) {
        this.set(updater.apply(this.value));
    }

    @Override
    public void addObserver(StateObserver observer) {
        if (!this.observers.contains(observer)) {
            this.observers.add(observer);
        }
    }

    @Override
    public void removeObserver(StateObserver observer) {
        this.observers.remove(observer);
    }

    @Override
    public void dispose() {
        this.observers.clear();
    }

    /**
     * Notify all observers that this state has changed.
     */
    private void notifyObservers() {
        // Create a copy to avoid concurrent modification if an observer modifies the list
        List<StateObserver> observersCopy = new ArrayList<>(this.observers);
        for (StateObserver observer : observersCopy) {
            observer.onStateChanged(this);
        }
    }

    /**
     * Create a new State with the given initial value.
     *
     * @param initialValue the initial value
     * @param <T>          the type of the value
     * @return a new State instance
     */
    public static <T> State<T> of(T initialValue) {
        return new StateImpl<>(initialValue);
    }
}
