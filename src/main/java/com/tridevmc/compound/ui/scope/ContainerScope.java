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
import com.tridevmc.compound.ui.element.IPrimitiveElement;
import com.tridevmc.compound.ui.event.*;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.slot.SlotMap;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.tree.ITreeNode;
import com.tridevmc.compound.ui.tree.TreeNode;
import com.tridevmc.compound.ui.tree.UITree;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Scope for configuring a container element.
 * Provides access to the element and composition methods for adding children.
 */
public class ContainerScope<T extends IContainer> extends ElementScope<T> implements IContainerScope<T> {
    protected final UITree tree;
    protected final ITreeNode parentNode;

    public ContainerScope(UITree tree, ITreeNode parentNode, T element) {
        super(element, parentNode);
        this.tree = tree;
        this.parentNode = parentNode;
    }

    @Override
    public <E extends IPrimitiveElement> void e(E element, Consumer<IElementScope<E>> configurator) {
        ITreeNode node = this.tree.createNode(element);
        this.tree.attachNode(this.parentNode, node);
        element.onAttached();

        if (configurator != null) {
            ElementScope<E> scope = new ElementScope<>(element, node);
            configurator.accept(scope);
            node.setLayoutProperties(scope.getLayoutProperties());
        }
    }

    @Override
    public <E extends IContainer> void e(E element, Consumer<IContainerScope<E>> configurator) {
        ITreeNode node = this.tree.createNode(element);
        this.tree.attachNode(this.parentNode, node);
        element.onAttached();

        if (configurator != null) {
            ContainerScope<E> scope = new ContainerScope<>(this.tree, node, element);
            configurator.accept(scope);
            node.setLayoutProperties(scope.getLayoutProperties());
        }
    }

    @Override
    public <E extends IComposableElement> void e(E element, Consumer<IComposableElementScope<E>> configurator) {
        ITreeNode node = this.tree.createNode(element);
        this.tree.attachNode(this.parentNode, node);
        element.onAttached();

        SlotMap slotMap = new SlotMap();
        node.setSlotMap(slotMap);

        ComposableElementScope<E> scope = new ComposableElementScope<>(this.tree, node, element, slotMap);

        if (configurator != null) {
            configurator.accept(scope);
            node.setLayoutProperties(scope.getLayoutProperties());
        }

        node.preserveHandlers();
        node.setCompositionFunction(() -> element.compose(scope));
        element.compose(scope);
    }

    @Override
    public void bind(State<?> state) {
        this.tree.bindNodeToState(this.parentNode, state);
    }

    @Override
    public void bindLayout(State<?> state) {
        if (this.parentNode instanceof TreeNode node) {
            node.bindLayoutState(state);
        }
    }

    @Override
    public void onClick(Function<MouseClickEvent, Boolean> handler) {
        this.parentNode.addClickHandler(handler);
    }

    @Override
    public void onScroll(Function<MouseScrollEvent, Boolean> handler) {
        this.parentNode.addScrollHandler(handler);
    }

    @Override
    public void onKeyPress(Function<KeyInputEvent, Boolean> handler) {
        this.parentNode.addKeyPressHandler(handler);
    }

    @Override
    public void onKeyRelease(Function<KeyInputEvent, Boolean> handler) {
        this.parentNode.addKeyReleaseHandler(handler);
    }

    @Override
    public void onCharTyped(Function<CharEvent, Boolean> handler) {
        this.parentNode.addCharTypedHandler(handler);
    }

    @Override
    public void onMouseRelease(Function<MouseReleaseEvent, Boolean> handler) {
        this.parentNode.addMouseReleaseHandler(handler);
    }

    @Override
    public void onMouseDrag(Function<MouseDragEvent, Boolean> handler) {
        this.parentNode.addMouseDragHandler(handler);
    }

    @Override
    public void onMouseMove(Function<MouseMoveEvent, Boolean> handler) {
        this.parentNode.addMouseMoveHandler(handler);
    }

    @Override
    public void onMouseEnter(Runnable handler) {
        this.parentNode.addMouseEnterHandler(handler);
    }

    @Override
    public void onMouseExit(Runnable handler) {
        this.parentNode.addMouseExitHandler(handler);
    }

    @Override
    public void onFocusGained(Runnable handler) {
        this.parentNode.addFocusGainedHandler(handler);
    }

    @Override
    public void onFocusLost(Runnable handler) {
        this.parentNode.addFocusLostHandler(handler);
    }

    @Override
    public void requestFocus() {
        this.parentNode.getTree().requestFocus(this.parentNode);
    }

    @Override
    public boolean isFocused() {
        return this.tree.hasFocus(this.parentNode);
    }

    @Override
    public void slot(SlotKey key, Consumer<ICompositionScope> defaultContent) {
        throw new UnsupportedOperationException("Slots are not supported in container scopes");
    }

    @Override
    public void slotInto(SlotKey key, Consumer<ICompositionScope> defaultContent, ICompositionScope targetScope) {
        throw new UnsupportedOperationException("Slots are not supported in container scopes");
    }

    @Override
    public SlotMap getSlotMap() {
        throw new UnsupportedOperationException("Slots are not supported in container scopes");
    }

    @Override
    public UITree getTree() {
        return this.tree;
    }

    @Override
    public void registerAnimation(AnimatedState<?> animation) {
        if (this.parentNode != null) {
            this.parentNode.registerAnimation(animation);
        }
    }
}
