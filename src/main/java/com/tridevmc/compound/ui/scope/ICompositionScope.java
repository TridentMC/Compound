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

package com.tridevmc.compound.ui.scope;

import com.tridevmc.compound.ui.animation.AnimatedState;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.animation.Interpolators;
import com.tridevmc.compound.ui.element.IComposableElement;
import com.tridevmc.compound.ui.element.IContainer;
import com.tridevmc.compound.ui.element.IPrimitiveElement;
import com.tridevmc.compound.ui.event.*;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.slot.SlotMap;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.tree.UITree;

import java.util.function.Consumer;
import java.util.function.Function;

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
        this.e(element, null);
    }

    /**
     * Add a container element to the tree without configuration.
     *
     * @param element the element instance to add
     * @param <T>     the element type
     */
    default <T extends IContainer> void e(T element) {
        this.e(element, null);
    }

    /**
     * Add a composable element to the tree without configuration.
     *
     * @param element the element instance to add
     * @param <T>     the element type
     */
    default <T extends IComposableElement> void e(T element) {
        this.e(element, null);
    }

    /**
     * Bind to a state, causing the current composition context to re-compose when the state changes.
     *
     * @param state the state to observe
     */
    void bind(State<?> state);

    /**
     * Bind to a state for composition-only updates (triggers full recomposition).
     * Use this when state changes affect the structure of the UI (conditional rendering, lists, etc.).
     *
     * @param state the state to observe for composition changes
     */
    default void bindComposition(State<?> state) {
        this.bind(state);
    }

    /**
     * Bind to a state for layout-only updates (triggers remeasure, not recomposition).
     * Use this when state changes only affect layout properties (size, position, spacing, etc.).
     *
     * @param state the state to observe for layout changes
     */
    void bindLayout(State<?> state);

    /**
     * Register a click handler on the current node.
     *
     * @param handler the click handler that returns true if handled
     */
    void onClick(Function<MouseClickEvent, Boolean> handler);

    /**
     * Register a scroll handler on the current node.
     * Handler should return true if it handled the event (stops bubbling),
     * false otherwise (continues bubbling to parent).
     *
     * @param handler the scroll handler that returns true if handled
     */
    void onScroll(Function<MouseScrollEvent, Boolean> handler);

    /**
     * Register a key press handler on the current node.
     *
     * @param handler the key press handler that returns true if handled
     */
    void onKeyPress(Function<KeyInputEvent, Boolean> handler);

    /**
     * Register a key release handler on the current node.
     *
     * @param handler the key release handler that returns true if handled
     */
    void onKeyRelease(Function<KeyInputEvent, Boolean> handler);

    /**
     * Register a character typed handler on the current node.
     *
     * @param handler the character typed handler that returns true if handled
     */
    void onCharTyped(Function<CharEvent, Boolean> handler);

    /**
     * Register a mouse release handler on the current node.
     *
     * @param handler the mouse release handler that returns true if handled
     */
    void onMouseRelease(Function<MouseReleaseEvent, Boolean> handler);

    /**
     * Register a mouse drag handler on the current node.
     *
     * @param handler the mouse drag handler that returns true if handled
     */
    void onMouseDrag(Function<MouseDragEvent, Boolean> handler);

    /**
     * Register a mouse move handler on the current node.
     *
     * @param handler the mouse move handler that returns true if handled
     */
    void onMouseMove(Function<MouseMoveEvent, Boolean> handler);

    /**
     * Register a mouse enter handler on the current node.
     * Called when the mouse enters the element's bounds.
     *
     * @param handler the mouse enter handler
     */
    void onMouseEnter(Runnable handler);

    /**
     * Register a mouse exit handler on the current node.
     * Called when the mouse leaves the element's bounds.
     *
     * @param handler the mouse exit handler
     */
    void onMouseExit(Runnable handler);

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

    /**
     * Get the UITree for accessing the animation scheduler.
     *
     * @return the UITree
     */
    UITree getTree();

    /**
     * Create an animated float state.
     *
     * @param initialValue the initial value
     * @param durationMs   the animation duration in milliseconds
     * @return the animated state
     */
    default AnimatedState<Float> animateFloat(float initialValue, long durationMs) {
        return animateFloat(initialValue, durationMs, Easing.EASE_IN_OUT);
    }

    /**
     * Create an animated float state with custom easing.
     *
     * @param initialValue the initial value
     * @param durationMs   the animation duration in milliseconds
     * @param easing       the easing function
     * @return the animated state
     */
    default AnimatedState<Float> animateFloat(float initialValue, long durationMs, Easing easing) {
        return new AnimatedState<>(initialValue, durationMs, Interpolators.FLOAT, easing, getTree().getAnimationScheduler());
    }

    /**
     * Create an animated integer state.
     *
     * @param initialValue the initial value
     * @param durationMs   the animation duration in milliseconds
     * @return the animated state
     */
    default AnimatedState<Integer> animateInt(int initialValue, long durationMs) {
        return animateInt(initialValue, durationMs, Easing.EASE_IN_OUT);
    }

    /**
     * Create an animated integer state with custom easing.
     *
     * @param initialValue the initial value
     * @param durationMs   the animation duration in milliseconds
     * @param easing       the easing function
     * @return the animated state
     */
    default AnimatedState<Integer> animateInt(int initialValue, long durationMs, Easing easing) {
        return new AnimatedState<>(initialValue, durationMs, Interpolators.INT, easing, getTree().getAnimationScheduler());
    }

    /**
     * Create an animated color state (ARGB).
     *
     * @param initialValue the initial color value (ARGB)
     * @param durationMs   the animation duration in milliseconds
     * @return the animated state
     */
    default AnimatedState<Integer> animateColor(int initialValue, long durationMs) {
        return animateColor(initialValue, durationMs, Easing.EASE_IN_OUT);
    }

    /**
     * Create an animated color state with custom easing.
     *
     * @param initialValue the initial color value (ARGB)
     * @param durationMs   the animation duration in milliseconds
     * @param easing       the easing function
     * @return the animated state
     */
    default AnimatedState<Integer> animateColor(int initialValue, long durationMs, Easing easing) {
        return new AnimatedState<>(initialValue, durationMs, Interpolators.COLOR, easing, getTree().getAnimationScheduler());
    }
}
