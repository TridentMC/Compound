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

package com.tridevmc.compound.ui.compose.scope;

import com.tridevmc.compound.ui.compose.element.IComposableElement;
import com.tridevmc.compound.ui.compose.element.IContainer;
import com.tridevmc.compound.ui.compose.element.IPrimitiveElement;
import com.tridevmc.compound.ui.compose.event.KeyEvent;
import com.tridevmc.compound.ui.compose.event.MouseClickEvent;
import com.tridevmc.compound.ui.compose.event.MouseScrollEvent;
import com.tridevmc.compound.ui.compose.state.State;

import java.util.function.Consumer;

/**
 * The scope given to composable elements for their internal composition.
 */
public interface ICompositionScope {

    /**
     * Add a primitive element to the tree.
     *
     * @param element      the element instance to add
     * @param configurator optional configuration receiving element scope
     * @param <T>          the element type
     */
    <T extends IPrimitiveElement> void e(T element, Consumer<IElementScope<T>> configurator);

    /**
     * Add a container element to the tree.
     *
     * @param element      the element instance to add
     * @param configurator optional configuration receiving container scope
     * @param <T>          the element type
     */
    <T extends IContainer> void e(T element, Consumer<IContainerScope<T>> configurator);

    /**
     * Add a composable element to the tree.
     *
     * @param element      the element instance to add
     * @param configurator optional configuration receiving composable scope
     * @param <T>          the element type
     */
    <T extends IComposableElement> void e(T element, Consumer<IComposableElementScope<T>> configurator);

    /**
     * Add an element to the tree without configuration.
     *
     * @param element the element instance to add
     * @param <T>     the element type
     */
    default <T extends IPrimitiveElement> void e(T element) {
        this.e(element, (Consumer<IElementScope<T>>) null);
    }

    /**
     * Add a container element to the tree without configuration.
     *
     * @param element the element instance to add
     * @param <T>     the element type
     */
    default <T extends IContainer> void e(T element) {
        this.e(element, (Consumer<IContainerScope<T>>) null);
    }

    /**
     * Add a composable element to the tree without configuration.
     *
     * @param element the element instance to add
     * @param <T>     the element type
     */
    default <T extends IComposableElement> void e(T element) {
        this.e(element, (Consumer<IComposableElementScope<T>>) null);
    }

    /**
     * Bind to a state, causing the current composition context to re-compose when the state changes.
     *
     * @param state the state to observe
     */
    void bind(State<?> state);

    /**
     * Register a click handler on the current node.
     *
     * @param handler the click handler
     */
    void onClick(Consumer<MouseClickEvent> handler);

    /**
     * Register a scroll handler on the current node.
     *
     * @param handler the scroll handler
     */
    void onScroll(Consumer<MouseScrollEvent> handler);

    /**
     * Register a key press handler on the current node.
     *
     * @param handler the key press handler
     */
    void onKeyPress(Consumer<KeyEvent> handler);

    /**
     * Register a key release handler on the current node.
     *
     * @param handler the key release handler
     */
    void onKeyRelease(Consumer<KeyEvent> handler);
}
