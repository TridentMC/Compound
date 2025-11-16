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
import com.tridevmc.compound.ui.compose.event.CharEvent;
import com.tridevmc.compound.ui.compose.event.KeyEvent;
import com.tridevmc.compound.ui.compose.event.MouseClickEvent;
import com.tridevmc.compound.ui.compose.event.MouseDragEvent;
import com.tridevmc.compound.ui.compose.event.MouseMoveEvent;
import com.tridevmc.compound.ui.compose.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.compose.event.MouseScrollEvent;
import com.tridevmc.compound.ui.compose.slot.SlotKey;
import com.tridevmc.compound.ui.compose.slot.SlotMap;
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

    /**
     * Register a character typed handler on the current node.
     *
     * @param handler the character typed handler
     */
    void onCharTyped(Consumer<CharEvent> handler);

    /**
     * Register a mouse release handler on the current node.
     *
     * @param handler the mouse release handler
     */
    void onMouseRelease(Consumer<MouseReleaseEvent> handler);

    /**
     * Register a mouse drag handler on the current node.
     *
     * @param handler the mouse drag handler
     */
    void onMouseDrag(Consumer<MouseDragEvent> handler);

    /**
     * Register a mouse move handler on the current node.
     *
     * @param handler the mouse move handler
     */
    void onMouseMove(Consumer<MouseMoveEvent> handler);

    /**
     * Render a slot (used inside compose() method).
     * This renders user-provided slot content, or falls back to default content if no content was provided.
     *
     * @param key            the slot key
     * @param defaultContent default content if slot not filled
     */
    void slot(SlotKey key, Consumer<ICompositionScope> defaultContent);

    /**
     * Render a slot without default content.
     *
     * @param key the slot key
     */
    default void slot(SlotKey key) {
        this.slot(key, null);
    }

    /**
     * Render a slot into a specific target scope.
     * This renders user-provided slot content into the target scope, or falls back to default content.
     * Useful when the content needs to be rendered as a child of a nested container.
     *
     * @param key            the slot key
     * @param defaultContent default content if slot not filled
     * @param targetScope    the scope to render the content into
     */
    void slotInto(SlotKey key, Consumer<ICompositionScope> defaultContent, ICompositionScope targetScope);

    /**
     * Render a slot into a specific target scope without default content.
     *
     * @param key         the slot key
     * @param targetScope the scope to render the content into
     */
    default void slotInto(SlotKey key, ICompositionScope targetScope) {
        this.slotInto(key, null, targetScope);
    }

    /**
     * Get the slot map for this composition scope.
     *
     * @return the slot map
     */
    SlotMap getSlotMap();
}
