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
import com.tridevmc.compound.ui.compose.slot.SlotContent;
import com.tridevmc.compound.ui.compose.slot.SlotKey;
import com.tridevmc.compound.ui.compose.slot.SlotMap;
import com.tridevmc.compound.ui.compose.state.State;
import com.tridevmc.compound.ui.compose.tree.ITreeNode;
import com.tridevmc.compound.ui.compose.tree.UITree;

import java.util.function.Consumer;

/**
 * Scope for configuring a composable element.
 * Provides composition methods and slot management.
 */
public class ComposableElementScope<T extends IComposableElement> extends ElementScope<T> implements IComposableElementScope<T>, ICompositionScope {
    private final UITree tree;
    private final ITreeNode parentNode;
    private final SlotMap slotMap;

    public ComposableElementScope(UITree tree, ITreeNode parentNode, T element, SlotMap slotMap) {
        super(element);
        this.tree = tree;
        this.parentNode = parentNode;
        this.slotMap = slotMap;
    }

    @Override
    public void fillSlot(SlotKey key, Consumer<ICompositionScope> content) {
        this.slotMap.put(key, new SlotContent(content));
        // Only re-compose if the element has already been composed (has children)
        if (!this.parentNode.getChildren().isEmpty()) {
            this.tree.recomposeNode(this.parentNode);
        }
    }

    @Override
    public void slot(SlotKey key, Consumer<ICompositionScope> defaultContent) {
        this.slotInto(key, defaultContent, this);
    }

    @Override
    public void slotInto(SlotKey key, Consumer<ICompositionScope> defaultContent, ICompositionScope targetScope) {
        SlotContent content = this.slotMap.get(key);
        if (content != null) {
            // Render user-provided content into target scope
            content.render(targetScope);
        } else if (defaultContent != null) {
            // Render default content into target scope
            defaultContent.accept(targetScope);
        }
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

        // Set composition function so it can re-compose when states change
        node.setCompositionFunction(() -> element.compose(scope));

        // Run composition to build internal structure
        element.compose(scope);
    }

    @Override
    public void bind(State<?> state) {
        // Bind the parent node to this state so it re-composes when state changes
        this.tree.bindNodeToState(this.parentNode, state);
    }

    @Override
    public void onClick(Consumer<MouseClickEvent> handler) {
        this.parentNode.addClickHandler(handler);
    }

    @Override
    public void onScroll(Consumer<MouseScrollEvent> handler) {
        this.parentNode.addScrollHandler(handler);
    }

    @Override
    public void onKeyPress(Consumer<KeyEvent> handler) {
        this.parentNode.addKeyPressHandler(handler);
    }

    @Override
    public void onKeyRelease(Consumer<KeyEvent> handler) {
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
    public SlotMap getSlotMap() {
        return this.slotMap;
    }
}
