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

/**
 * Root scope for bootstrapping composition.
 * The first e() call becomes the root of the tree.
 */
public class RootScope implements ICompositionScope {
    private final UITree tree;
    private ITreeNode rootNode;

    public RootScope(UITree tree) {
        this.tree = tree;
    }

    @Override
    public <T extends IPrimitiveElement> void e(T element, Consumer<IElementScope<T>> configurator) {
        ITreeNode node = this.tree.createNode(element);

        if (!this.tree.hasRoot()) {
            this.tree.setRoot(node);
            this.rootNode = node;
        } else if (this.rootNode != null) {
            this.tree.attachNode(this.rootNode, node);
        }

        element.onAttached();

        if (configurator != null) {
            ElementScope<T> scope = new ElementScope<>(element);
            configurator.accept(scope);
        }
    }

    @Override
    public <T extends IContainer> void e(T element, Consumer<IContainerScope<T>> configurator) {
        ITreeNode node = this.tree.createNode(element);

        if (!this.tree.hasRoot()) {
            this.tree.setRoot(node);
            this.rootNode = node;
        } else if (this.rootNode != null) {
            this.tree.attachNode(this.rootNode, node);
        }

        element.onAttached();

        if (configurator != null) {
            ContainerScope<T> scope = new ContainerScope<>(this.tree, node, element);
            configurator.accept(scope);
        }
    }

    @Override
    public <T extends IComposableElement> void e(T element, Consumer<IComposableElementScope<T>> configurator) {
        ITreeNode node = this.tree.createNode(element);

        if (!this.tree.hasRoot()) {
            this.tree.setRoot(node);
            this.rootNode = node;
        } else if (this.rootNode != null) {
            this.tree.attachNode(this.rootNode, node);
        }

        element.onAttached();

        SlotMap slotMap = new SlotMap();
        node.setSlotMap(slotMap);

        ComposableElementScope<T> scope = new ComposableElementScope<>(this.tree, node, element, slotMap);

        if (configurator != null) {
            configurator.accept(scope);
        }

        node.setCompositionFunction(() -> element.compose(scope));
        element.compose(scope);
    }

    @Override
    public void bind(State<?> state) {
        if (this.rootNode != null) {
            this.tree.bindNodeToState(this.rootNode, state);
        }
    }

    @Override
    public void bindLayout(State<?> state) {
        if (this.rootNode != null && this.rootNode instanceof TreeNode node) {
            node.bindLayoutState(state);
        }
    }

    @Override
    public void onClick(Consumer<MouseClickEvent> handler) {
        if (this.rootNode != null) {
            this.rootNode.addClickHandler(handler);
        }
    }

    @Override
    public void onScroll(Consumer<MouseScrollEvent> handler) {
        if (this.rootNode != null) {
            this.rootNode.addScrollHandler(handler);
        }
    }

    @Override
    public void onKeyPress(Consumer<KeyInputEvent> handler) {
        if (this.rootNode != null) {
            this.rootNode.addKeyPressHandler(handler);
        }
    }

    @Override
    public void onKeyRelease(Consumer<KeyInputEvent> handler) {
        if (this.rootNode != null) {
            this.rootNode.addKeyReleaseHandler(handler);
        }
    }

    @Override
    public void onCharTyped(Consumer<CharEvent> handler) {
        if (this.rootNode != null) {
            this.rootNode.addCharTypedHandler(handler);
        }
    }

    @Override
    public void onMouseRelease(Consumer<MouseReleaseEvent> handler) {
        if (this.rootNode != null) {
            this.rootNode.addMouseReleaseHandler(handler);
        }
    }

    @Override
    public void onMouseDrag(Consumer<MouseDragEvent> handler) {
        if (this.rootNode != null) {
            this.rootNode.addMouseDragHandler(handler);
        }
    }

    @Override
    public void onMouseMove(Consumer<MouseMoveEvent> handler) {
        if (this.rootNode != null) {
            this.rootNode.addMouseMoveHandler(handler);
        }
    }

    @Override
    public void slot(SlotKey key, Consumer<ICompositionScope> defaultContent) {
        throw new UnsupportedOperationException("Slots are not supported at the root level");
    }

    @Override
    public void slotInto(SlotKey key, Consumer<ICompositionScope> defaultContent, ICompositionScope targetScope) {
        throw new UnsupportedOperationException("Slots are not supported at the root level");
    }

    @Override
    public SlotMap getSlotMap() {
        throw new UnsupportedOperationException("Slots are not supported at the root level");
    }

    @Override
    public UITree getTree() {
        return this.tree;
    }
}
