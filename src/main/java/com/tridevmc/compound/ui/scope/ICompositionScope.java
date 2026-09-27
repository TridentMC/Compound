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
     * Register a scroll handler on the current node that only consumes the event
     * when this element currently has keyboard focus. This is the correct default
     * for text inputs and other focusable widgets embedded inside scrollable containers.
     *
     * @param handler the scroll handler that returns true if handled
     */
    default void onScrollWhenFocused(Function<MouseScrollEvent, Boolean> handler) {
        this.onScroll(event -> {
            if (!this.isFocused()) {
                return false;
            }
            return handler.apply(event);
        });
    }

    /**
     * Returns true if the current composition node has keyboard focus.
     * For the root scope this always returns false.
     *
     * @return whether the current element is focused
     */
    boolean isFocused();

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
     * Register a focus gained handler on the current node.
     * Called when this element receives keyboard focus.
     *
     * @param handler the focus gained handler
     */
    void onFocusGained(Runnable handler);

    /**
     * Register a focus lost handler on the current node.
     * Called when this element loses keyboard focus.
     *
     * @param handler the focus lost handler
     */
    void onFocusLost(Runnable handler);

    /**
     * Request keyboard focus for the current element.
     * This will cause the previously focused element (if any) to lose focus.
     */
    void requestFocus();

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
     * Register an animation for automatic lifecycle management.
     * Animations registered here are automatically disposed when the composition
     * node is detached. Elements should not manually dispose animations.
     * <p>
     * The default implementation does nothing. Scopes that have access to a tree
     * node should override this to register animations with the node for automatic cleanup.
     *
     * @param animation the animation to register
     */
    default void registerAnimation(AnimatedState<?> animation) {
        // Default: no-op. Override in scopes that have tree/node access.
    }

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
        var state = new AnimatedState<>(initialValue, durationMs, Interpolators.FLOAT, easing, getTree().getAnimationScheduler());
        this.registerAnimation(state);
        return state;
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
        var state = new AnimatedState<>(initialValue, durationMs, Interpolators.INT, easing, getTree().getAnimationScheduler());
        this.registerAnimation(state);
        return state;
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
        var state = new AnimatedState<>(initialValue, durationMs, Interpolators.COLOR, easing, getTree().getAnimationScheduler());
        this.registerAnimation(state);
        return state;
    }

    /**
     * Create a looping animated float state that alternates between two values.
     * The animation will continuously cycle between startValue and endValue with the specified easing.
     *
     * @param startValue the first value in the loop
     * @param endValue   the second value in the loop
     * @param intervalMs the duration of each transition in milliseconds
     * @param easing     the easing function (use Easing.STEP for instant toggles)
     * @return the looping animated state
     */
    default AnimatedState<Float> animateFloatLooping(float startValue, float endValue, long intervalMs, Easing easing) {
        var state = new AnimatedState<Float>(startValue, intervalMs, Interpolators.FLOAT, easing,
                getTree().getAnimationScheduler(), true, startValue, endValue);
        this.registerAnimation(state);
        return state;
    }

    /**
     * Create a looping animated float state with STEP easing (instant toggle).
     *
     * @param startValue the first value in the loop
     * @param endValue   the second value in the loop
     * @param intervalMs the duration of each transition in milliseconds
     * @return the looping animated state
     */
    default AnimatedState<Float> animateFloatLooping(float startValue, float endValue, long intervalMs) {
        return animateFloatLooping(startValue, endValue, intervalMs, Easing.STEP);
    }

    /**
     * Create a looping animated integer state that alternates between two values.
     * The animation will continuously cycle between startValue and endValue with the specified easing.
     *
     * @param startValue the first value in the loop
     * @param endValue   the second value in the loop
     * @param intervalMs the duration of each transition in milliseconds
     * @param easing     the easing function (use Easing.STEP for instant toggles)
     * @return the looping animated state
     */
    default AnimatedState<Integer> animateIntLooping(int startValue, int endValue, long intervalMs, Easing easing) {
        var state = new AnimatedState<Integer>(startValue, intervalMs, Interpolators.INT, easing,
                getTree().getAnimationScheduler(), true, startValue, endValue);
        this.registerAnimation(state);
        return state;
    }

    /**
     * Create a looping animated integer state with STEP easing (instant toggle).
     *
     * @param startValue the first value in the loop
     * @param endValue   the second value in the loop
     * @param intervalMs the duration of each transition in milliseconds
     * @return the looping animated state
     */
    default AnimatedState<Integer> animateIntLooping(int startValue, int endValue, long intervalMs) {
        return animateIntLooping(startValue, endValue, intervalMs, Easing.STEP);
    }

    /**
     * Create a looping animated color state (ARGB) that alternates between two colors.
     * The animation will continuously cycle between startColor and endColor with the specified easing.
     *
     * @param startColor the first color value (ARGB) in the loop
     * @param endColor   the second color value (ARGB) in the loop
     * @param intervalMs the duration of each transition in milliseconds
     * @param easing     the easing function
     * @return the looping animated state
     */
    default AnimatedState<Integer> animateColorLooping(int startColor, int endColor, long intervalMs, Easing easing) {
        var state = new AnimatedState<Integer>(startColor, intervalMs, Interpolators.COLOR, easing,
                getTree().getAnimationScheduler(), true, startColor, endColor);
        this.registerAnimation(state);
        return state;
    }

    /**
     * Create a looping animated color state with default EASE_IN_OUT easing.
     *
     * @param startColor the first color value (ARGB) in the loop
     * @param endColor   the second color value (ARGB) in the loop
     * @param intervalMs the duration of each transition in milliseconds
     * @return the looping animated state
     */
    default AnimatedState<Integer> animateColorLooping(int startColor, int endColor, long intervalMs) {
        return animateColorLooping(startColor, endColor, intervalMs, Easing.EASE_IN_OUT);
    }
}
