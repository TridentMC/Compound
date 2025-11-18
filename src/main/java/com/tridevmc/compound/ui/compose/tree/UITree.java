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

package com.tridevmc.compound.ui.compose.tree;

import com.tridevmc.compound.ui.compose.element.IElement;
import com.tridevmc.compound.ui.compose.element.IPrimitiveElement;
import com.tridevmc.compound.ui.compose.event.CharEvent;
import com.tridevmc.compound.ui.compose.event.KeyInputEvent;
import com.tridevmc.compound.ui.compose.event.MouseClickEvent;
import com.tridevmc.compound.ui.compose.event.MouseDragEvent;
import com.tridevmc.compound.ui.compose.event.MouseMoveEvent;
import com.tridevmc.compound.ui.compose.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.compose.event.MouseScrollEvent;
import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.Position;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.state.State;
import com.tridevmc.compound.ui.compose.state.StateObserver;
import com.tridevmc.compound.ui.screen.IScreenContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Manages the tree of nodes (like the DOM).
 * NOTE: This is internal to the framework and not exposed to elements or composition code.
 */
public class UITree implements StateObserver {
    private ITreeNode root;
    private final Map<IElement, ITreeNode> elementToNode = new HashMap<>();
    private final Set<State<?>> observedStates = new HashSet<>();
    private Size rootSize;
    private Size lastWindowSize = new Size(-1, -1);

    public void setRoot(ITreeNode root) {
        this.root = root;
        if (root != null) {
            this.registerNode(root);
        }
    }

    public ITreeNode getRoot() {
        return this.root;
    }

    public boolean hasRoot() {
        return this.root != null;
    }

    public ITreeNode getNodeForElement(IElement element) {
        return this.elementToNode.get(element);
    }

    public boolean hasNode(IElement element) {
        return this.elementToNode.containsKey(element);
    }

    public ITreeNode createNode(IElement element) {
        TreeNode node = new TreeNode(element);
        element.setTree(this);
        this.registerNode(node);
        return node;
    }

    private void registerNode(ITreeNode node) {
        this.elementToNode.put(node.getElement(), node);
        node.getElement().setTree(this);
        for (ITreeNode child : node.getChildren()) {
            this.registerNode(child);
        }
    }

    private void unregisterNode(ITreeNode node) {
        this.elementToNode.remove(node.getElement());
        node.getElement().setTree(null);
        for (ITreeNode child : node.getChildren()) {
            this.unregisterNode(child);
        }
    }

    public void attachNode(ITreeNode parent, ITreeNode child) {
        parent.addChild(child);
        this.registerNode(child);
    }

    public void detachNode(ITreeNode node) {
        ITreeNode parent = node.getParent();
        if (parent != null) {
            parent.removeChild(node);
        }

        Set<State<?>> statesToCheck = new HashSet<>();
        this.walkDepthFirst(node, n -> statesToCheck.addAll(n.getBoundStates()));

        this.unregisterNode(node);

        for (State<?> state : statesToCheck) {
            this.unregisterStateIfUnused(state);
        }

        node.getElement().onDetached();
    }

    public void recomposeNode(ITreeNode node) {
        if (!node.hasCompositionFunction()) {
            return;
        }

        List<ITreeNode> children = new ArrayList<>(node.getChildren());
        for (ITreeNode child : children) {
            this.detachNode(child);
        }

        Runnable compositionFn = node.getCompositionFunction();
        if (compositionFn != null) {
            compositionFn.run();
            this.requestRemeasurement(node);
        }
    }

    public void walkDepthFirst(ITreeNode start, Consumer<ITreeNode> visitor) {
        if (start == null) {
            return;
        }
        visitor.accept(start);
        for (ITreeNode child : start.getChildren()) {
            this.walkDepthFirst(child, visitor);
        }
    }

    public void walkBreadthFirst(ITreeNode start, Consumer<ITreeNode> visitor) {
        if (start == null) {
            return;
        }
        Queue<ITreeNode> queue = new LinkedList<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            ITreeNode node = queue.poll();
            visitor.accept(node);
            queue.addAll(node.getChildren());
        }
    }

    public <T extends IElement> List<ITreeNode> findNodesByElementType(Class<T> type) {
        List<ITreeNode> result = new ArrayList<>();
        this.walkDepthFirst(this.root, node -> {
            if (type.isInstance(node.getElement())) {
                result.add(node);
            }
        });
        return result;
    }

    public ITreeNode findNodeAt(int x, int y) {
        if (this.root == null) {
            return null;
        }
        List<ITreeNode> nodes = new ArrayList<>();
        this.walkDepthFirst(this.root, nodes::add);
        Collections.reverse(nodes);

        for (ITreeNode node : nodes) {
            if (node.getElement().getBounds() != null &&
                    node.getElement().getBounds().contains(x, y)) {
                return node;
            }
        }
        return null;
    }

    public void measureTree(Constraints rootConstraints) {
        if (this.root != null) {
            this.rootSize = this.measureNode(this.root, rootConstraints);
        }
    }

    public void placeTree(Position rootPosition) {
        if (this.root != null && this.rootSize != null) {
            Bounds rootBounds = new Bounds(rootPosition, this.rootSize);
            this.placeNode(this.root, rootBounds);
        }
    }

    public void placeTree(Position rootPosition, Constraints rootConstraints) {
        if (this.root != null && this.rootSize != null) {
            Size constraintSize = new Size(
                rootConstraints.hasBoundedWidth() ? rootConstraints.maxWidth() : this.rootSize.width(),
                rootConstraints.hasBoundedHeight() ? rootConstraints.maxHeight() : this.rootSize.height()
            );
            Bounds rootBounds = new Bounds(rootPosition, constraintSize);
            this.placeNode(this.root, rootBounds);
        }
    }

    private Size measureNode(ITreeNode node, Constraints constraints) {
        List<IElement> children = node.getChildren().stream()
                .map(ITreeNode::getElement)
                .toList();
        Size size = node.getElement().measure(constraints, children);
        node.setMeasuredSize(size);
        return size;
    }

    private void placeNode(ITreeNode node, Bounds bounds) {
        List<IElement> children = node.getChildren().stream()
                .map(ITreeNode::getElement)
                .toList();

        Bounds placementBounds = bounds;
        if ((bounds.width() == 0 || bounds.height() == 0) && node.getMeasuredSize() != null) {
            Size measuredSize = node.getMeasuredSize();
            placementBounds = new Bounds(bounds.position(), measuredSize);
        }

        node.getElement().place(placementBounds, children);
    }

    private void requestRemeasurement(ITreeNode changedNode) {
        if (this.root == null || this.rootSize == null) {
            return;
        }

        ITreeNode target = changedNode.getParent();
        while (target != null && target.getParent() != null) {
            target = target.getParent();
        }

        Constraints constraints = Constraints.loose(this.rootSize.width(), this.rootSize.height());
        ITreeNode measureTarget = target != null ? target : changedNode;

        this.measureNode(measureTarget, constraints);
        Bounds bounds = new Bounds(Position.ORIGIN, this.rootSize);
        this.placeNode(measureTarget, bounds);
    }

    public void dispatchClick(int x, int y, MouseClickEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            while (current != null && !event.isConsumed()) {
                for (var handler : current.getClickHandlers()) {
                    handler.accept(event);
                    if (event.isConsumed()) {
                        break;
                    }
                }
                current = current.getParent();
            }
        }
    }

    private ITreeNode lastHoveredNode = null;

    public void dispatchMouseMove(int x, int y, MouseMoveEvent event) {
        ITreeNode node = this.findNodeAt(x, y);

        if (node != lastHoveredNode) {
            if (lastHoveredNode != null) {
                for (var handler : lastHoveredNode.getMouseExitHandlers()) {
                    handler.run();
                }
            }

            if (node != null) {
                for (var handler : node.getMouseEnterHandlers()) {
                    handler.run();
                }
            }

            lastHoveredNode = node;
        }

        if (node != null && !event.isConsumed()) {
            ITreeNode current = node;
            while (current != null && !event.isConsumed()) {
                for (var handler : current.getMouseMoveHandlers()) {
                    handler.accept(event);
                    if (event.isConsumed()) {
                        break;
                    }
                }
                current = current.getParent();
            }
        }
    }

    public void dispatchScroll(int x, int y, MouseScrollEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            while (current != null && !event.isConsumed()) {
                for (var handler : current.getScrollHandlers()) {
                    handler.accept(event);
                    if (event.isConsumed()) {
                        break;
                    }
                }
                current = current.getParent();
            }
        }
    }

    public void dispatchKeyPress(KeyInputEvent event) {
        // TODO: Focus system - add focus tracking to dispatch keyboard events only to the focused element.
        //       Current implementation broadcasts to all elements, which is inefficient for large UIs.
        this.walkDepthFirst(this.root, node -> {
            if (!event.isConsumed()) {
                for (var handler : node.getKeyPressHandlers()) {
                    handler.accept(event);
                    if (event.isConsumed()) {
                        break;
                    }
                }
            }
        });
    }

    public void dispatchKeyRelease(KeyInputEvent event) {
        // TODO: Focus system - see dispatchKeyPress
        this.walkDepthFirst(this.root, node -> {
            if (!event.isConsumed()) {
                for (var handler : node.getKeyReleaseHandlers()) {
                    handler.accept(event);
                    if (event.isConsumed()) {
                        break;
                    }
                }
            }
        });
    }

    public void dispatchCharTyped(CharEvent event) {
        // TODO: Focus system - see dispatchKeyPress
        this.walkDepthFirst(this.root, node -> {
            if (!event.isConsumed()) {
                for (var handler : node.getCharTypedHandlers()) {
                    handler.accept(event);
                    if (event.isConsumed()) {
                        break;
                    }
                }
            }
        });
    }

    public void dispatchMouseRelease(int x, int y, MouseReleaseEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            while (current != null && !event.isConsumed()) {
                for (var handler : current.getMouseReleaseHandlers()) {
                    handler.accept(event);
                    if (event.isConsumed()) {
                        break;
                    }
                }
                current = current.getParent();
            }
        }
    }

    public void dispatchMouseDrag(int x, int y, MouseDragEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            while (current != null && !event.isConsumed()) {
                for (var handler : current.getMouseDragHandlers()) {
                    handler.accept(event);
                    if (event.isConsumed()) {
                        break;
                    }
                }
                current = current.getParent();
            }
        }
    }

    @Override
    public void onStateChanged(State<?> state) {
        List<ITreeNode> nodesToRecompose = new ArrayList<>();
        this.walkDepthFirst(this.root, node -> {
            if (node.isBoundToState(state)) {
                nodesToRecompose.add(node);
            }
        });

        for (ITreeNode node : nodesToRecompose) {
            this.recomposeNode(node);
        }
    }

    /**
     * Register a state for observation. The UITree will observe this state
     * and trigger re-composition when it changes.
     *
     * @param state the state to observe
     */
    public void registerState(State<?> state) {
        if (!this.observedStates.contains(state)) {
            this.observedStates.add(state);
            state.addObserver(this);
        }
    }

    /**
     * Unregister a state from observation if it's no longer used by any nodes.
     *
     * @param state the state to check and potentially unregister
     */
    public void unregisterStateIfUnused(State<?> state) {
        boolean[] isUsed = {false};
        this.walkDepthFirst(this.root, node -> {
            if (node.isBoundToState(state)) {
                isUsed[0] = true;
            }
        });

        if (!isUsed[0] && this.observedStates.contains(state)) {
            this.observedStates.remove(state);
            state.removeObserver(this);
        }
    }

    /**
     * Bind a node to a state and ensure the state is observed.
     *
     * @param node  the node to bind
     * @param state the state to bind to
     */
    public void bindNodeToState(ITreeNode node, State<?> state) {
        node.bindState(state);
        this.registerState(state);
    }

    /**
     * Unbind a node from a state and cleanup if no longer needed.
     *
     * @param node  the node to unbind
     * @param state the state to unbind from
     */
    public void unbindNodeFromState(ITreeNode node, State<?> state) {
        node.unbindState(state);
        this.unregisterStateIfUnused(state);
    }

    /**
     * Handle layout and rendering with window dimensions.
     * UITree tracks when window size changes and triggers measurement only when needed.
     */
    public void layoutAndRender(int width, int height, IScreenContext context) {
        if (this.root == null) {
            return;
        }

        var sizeChanged = this.lastWindowSize.width() != width || this.lastWindowSize.height() != height;

        if (sizeChanged) {
            this.lastWindowSize = new Size(width, height);
            var constraints = Constraints.loose(width, height);
            this.measureTree(constraints);
            this.placeTree(Position.ORIGIN, constraints);
        }

        this.renderTree(context);
    }

    public void renderTree(IScreenContext context) {
        if (this.root == null) {
            return;
        }
        this.walkDepthFirst(this.root, node -> {
            IElement element = node.getElement();
            if (element.isVisible() && element instanceof IPrimitiveElement primitive) {
                primitive.draw(context);
            }
        });
    }
}
