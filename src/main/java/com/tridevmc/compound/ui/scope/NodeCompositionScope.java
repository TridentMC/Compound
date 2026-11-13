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
import com.tridevmc.compound.ui.element.IComposableElement;
import com.tridevmc.compound.ui.element.IContainer;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.IPrimitiveElement;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.event.MouseMoveEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.slot.SlotMap;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.tree.ITreeNode;
import java.util.function.Consumer;
import java.util.function.Function;

interface NodeCompositionScope extends ICompositionScope {
    ITreeNode scopeNode();

    void attach(ITreeNode node);

    default ITreeNode mount(IElement element) {
        var node = this.getTree().createNode(element);
        this.attach(node);
        element.onAttached();
        return node;
    }

    @Override
    default <T extends IPrimitiveElement> void e(T element, Consumer<IElementScope<T>> configurator) {
        var node = this.mount(element);
        if (configurator != null) {
            var scope = new ElementScope<>(element, node);
            configurator.accept(scope);
            node.setLayoutProperties(scope.getLayoutProperties());
        }
    }

    @Override
    default <T extends IContainer> void e(T element, Consumer<IContainerScope<T>> configurator) {
        var node = this.mount(element);
        var scope = new ContainerScope<>(this.getTree(), node, element);
        node.setLayoutProperties(scope.getLayoutProperties());
        node.setCompositionFunction(() -> {
            scope.resetLayoutProperties();
            node.setLayoutProperties(scope.getLayoutProperties());
            if (configurator != null) configurator.accept(scope);
        });
        node.runComposition();
    }

    @Override
    default <T extends IComposableElement> void e(T element, Consumer<IComposableElementScope<T>> configurator) {
        var node = this.mount(element);
        var slots = new SlotMap();
        node.setSlotMap(slots);
        var scope = new ComposableElementScope<>(this.getTree(), node, element, slots);
        node.setLayoutProperties(scope.getLayoutProperties());
        if (configurator != null) configurator.accept(scope);
        node.preserveHandlers();
        node.setCompositionFunction(() -> element.compose(scope));
        node.runComposition();
    }

    @Override
    default void bind(State<?> state) {
        if (this.scopeNode() != null) this.scopeNode().bindCompositionState(state);
    }

    @Override
    default void bindLayout(State<?> state) {
        if (this.scopeNode() != null) this.scopeNode().bindLayoutState(state);
    }

    @Override
    default void onClick(Function<MouseClickEvent, Boolean> handler) {
        if (this.scopeNode() != null) this.scopeNode().addClickHandler(handler);
    }

    @Override
    default void onScroll(Function<MouseScrollEvent, Boolean> handler) {
        if (this.scopeNode() != null) this.scopeNode().addScrollHandler(handler);
    }

    @Override
    default void onKeyPress(Function<KeyInputEvent, Boolean> handler) {
        if (this.scopeNode() != null) this.scopeNode().addKeyPressHandler(handler);
    }

    @Override
    default void onKeyRelease(Function<KeyInputEvent, Boolean> handler) {
        if (this.scopeNode() != null) this.scopeNode().addKeyReleaseHandler(handler);
    }

    @Override
    default void onCharTyped(Function<CharEvent, Boolean> handler) {
        if (this.scopeNode() != null) this.scopeNode().addCharTypedHandler(handler);
    }

    @Override
    default void onMouseRelease(Function<MouseReleaseEvent, Boolean> handler) {
        if (this.scopeNode() != null) this.scopeNode().addMouseReleaseHandler(handler);
    }

    @Override
    default void onMouseDrag(Function<MouseDragEvent, Boolean> handler) {
        if (this.scopeNode() != null) this.scopeNode().addMouseDragHandler(handler);
    }

    @Override
    default void onMouseMove(Function<MouseMoveEvent, Boolean> handler) {
        if (this.scopeNode() != null) this.scopeNode().addMouseMoveHandler(handler);
    }

    @Override
    default void onMouseEnter(Runnable handler) {
        if (this.scopeNode() != null) this.scopeNode().addMouseEnterHandler(handler);
    }

    @Override
    default void onMouseExit(Runnable handler) {
        if (this.scopeNode() != null) this.scopeNode().addMouseExitHandler(handler);
    }

    @Override
    default void onFocusGained(Runnable handler) {
        if (this.scopeNode() != null) this.scopeNode().addFocusGainedHandler(handler);
    }

    @Override
    default void onFocusLost(Runnable handler) {
        if (this.scopeNode() != null) this.scopeNode().addFocusLostHandler(handler);
    }

    @Override
    default void requestFocus() {
        if (this.scopeNode() != null) this.getTree().requestFocus(this.scopeNode());
    }

    @Override
    default boolean isFocused() {
        return this.scopeNode() != null && this.getTree().hasFocus(this.scopeNode());
    }

    @Override
    default void useAnimationTimeline(com.tridevmc.compound.ui.animation.api.IAnimationTimeline timeline) {
        if (this.scopeNode() == null) throw new IllegalStateException("Timeline needs a mounted node");
        this.getTree().useAnimationTimeline(this.scopeNode(), timeline);
    }

    @Override
    default void registerAnimation(AnimatedState<?> animation) {
        if (this.scopeNode() != null) this.scopeNode().registerAnimation(animation);
    }

    @Override
    default void retainAnimation(AnimatedState<?> animation) {
        if (this.scopeNode() != null) this.scopeNode().retainAnimation(animation);
    }

    @Override
    default void slot(SlotKey key, Consumer<ICompositionScope> defaultContent) {
        this.slotInto(key, defaultContent, this);
    }

    @Override
    default void slotInto(SlotKey key, Consumer<ICompositionScope> defaultContent, ICompositionScope target) {
        var content = this.getSlotMap().get(key);
        if (content != null) content.render(target);
        else if (defaultContent != null) defaultContent.accept(target);
    }

    @Override
    default SlotMap getSlotMap() {
        throw new UnsupportedOperationException("Slots require a composable element scope");
    }
}
