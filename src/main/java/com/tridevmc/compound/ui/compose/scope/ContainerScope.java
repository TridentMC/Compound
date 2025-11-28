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
import com.tridevmc.compound.ui.compose.event.KeyInputEvent;
import com.tridevmc.compound.ui.compose.event.MouseClickEvent;
import com.tridevmc.compound.ui.compose.event.MouseDragEvent;
import com.tridevmc.compound.ui.compose.event.MouseMoveEvent;
import com.tridevmc.compound.ui.compose.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.compose.event.MouseScrollEvent;
import com.tridevmc.compound.ui.compose.slot.SlotKey;
import com.tridevmc.compound.ui.compose.slot.SlotMap;
import com.tridevmc.compound.ui.compose.state.State;
import com.tridevmc.compound.ui.compose.tree.ITreeNode;
import com.tridevmc.compound.ui.compose.tree.TreeNode;
import com.tridevmc.compound.ui.compose.tree.UITree;

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
        super(element);
        this.tree = tree;
        this.parentNode = parentNode;
    }

    @Override
    public <E extends IPrimitiveElement> void e(E element, Consumer<IElementScope<E>> configurator) {
        ITreeNode node = this.tree.createNode(element);
        this.tree.attachNode(this.parentNode, node);
        element.onAttached();

        if (configurator != null) {
            ElementScope<E> scope = new ElementScope<>(element);
            configurator.accept(scope);
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
        }

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
    public void onClick(Consumer<MouseClickEvent> handler) {
        this.parentNode.addClickHandler(handler);
    }

    @Override
    public void onScroll(Function<MouseScrollEvent, Boolean> handler) {
        this.parentNode.addScrollHandler(handler);
    }

    @Override
    public void onKeyPress(Consumer<KeyInputEvent> handler) {
        this.parentNode.addKeyPressHandler(handler);
    }

    @Override
    public void onKeyRelease(Consumer<KeyInputEvent> handler) {
        this.parentNode.addKeyReleaseHandler(handler);
    }

    @Override
    public void onCharTyped(Consumer<CharEvent> handler) {
        this.parentNode.addCharTypedHandler(handler);
    }

    @Override
    public void onMouseRelease(Consumer<MouseReleaseEvent> handler) {
        this.parentNode.addMouseReleaseHandler(handler);
    }

    @Override
    public void onMouseDrag(Consumer<MouseDragEvent> handler) {
        this.parentNode.addMouseDragHandler(handler);
    }

    @Override
    public void onMouseMove(Consumer<MouseMoveEvent> handler) {
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
}
