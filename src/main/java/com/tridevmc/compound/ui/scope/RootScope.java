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
            node.setLayoutProperties(scope.getLayoutProperties());
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
            node.setLayoutProperties(scope.getLayoutProperties());
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
            node.setLayoutProperties(scope.getLayoutProperties());
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
    public void onClick(Function<MouseClickEvent, Boolean> handler) {
        if (this.rootNode != null) {
            this.rootNode.addClickHandler(handler);
        }
    }

    @Override
    public void onScroll(Function<MouseScrollEvent, Boolean> handler) {
        if (this.rootNode != null) {
            this.rootNode.addScrollHandler(handler);
        }
    }

    @Override
    public void onKeyPress(Function<KeyInputEvent, Boolean> handler) {
        if (this.rootNode != null) {
            this.rootNode.addKeyPressHandler(handler);
        }
    }

    @Override
    public void onKeyRelease(Function<KeyInputEvent, Boolean> handler) {
        if (this.rootNode != null) {
            this.rootNode.addKeyReleaseHandler(handler);
        }
    }

    @Override
    public void onCharTyped(Function<CharEvent, Boolean> handler) {
        if (this.rootNode != null) {
            this.rootNode.addCharTypedHandler(handler);
        }
    }

    @Override
    public void onMouseRelease(Function<MouseReleaseEvent, Boolean> handler) {
        if (this.rootNode != null) {
            this.rootNode.addMouseReleaseHandler(handler);
        }
    }

    @Override
    public void onMouseDrag(Function<MouseDragEvent, Boolean> handler) {
        if (this.rootNode != null) {
            this.rootNode.addMouseDragHandler(handler);
        }
    }

    @Override
    public void onMouseMove(Function<MouseMoveEvent, Boolean> handler) {
        if (this.rootNode != null) {
            this.rootNode.addMouseMoveHandler(handler);
        }
    }

    @Override
    public void onMouseEnter(Runnable handler) {
        if (this.rootNode != null) {
            this.rootNode.addMouseEnterHandler(handler);
        }
    }

    @Override
    public void onMouseExit(Runnable handler) {
        if (this.rootNode != null) {
            this.rootNode.addMouseExitHandler(handler);
        }
    }

    @Override
    public void onFocusGained(Runnable handler) {
        if (this.rootNode != null) {
            this.rootNode.addFocusGainedHandler(handler);
        }
    }

    @Override
    public void onFocusLost(Runnable handler) {
        if (this.rootNode != null) {
            this.rootNode.addFocusLostHandler(handler);
        }
    }

    @Override
    public void requestFocus() {
        if (this.rootNode != null) {
            this.rootNode.getTree().requestFocus(this.rootNode);
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
