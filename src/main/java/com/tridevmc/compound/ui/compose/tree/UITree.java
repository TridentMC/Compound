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
import com.tridevmc.compound.ui.compose.event.KeyEvent;
import com.tridevmc.compound.ui.compose.event.MouseClickEvent;
import com.tridevmc.compound.ui.compose.event.MouseScrollEvent;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.Position;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.state.State;
import com.tridevmc.compound.ui.compose.state.StateObserver;

import java.util.*;
import java.util.function.Consumer;

/**
 * Manages the tree of nodes (like the DOM).
 * NOTE: This is internal to the framework and not exposed to elements or composition code.
 */
public class UITree implements StateObserver {
    private ITreeNode root;
    private final Map<IElement, ITreeNode> elementToNode = new HashMap<>();
    private final Set<State<?>> observedStates = new HashSet<>();

    // Root node management
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

    // Node lookup
    public ITreeNode getNodeForElement(IElement element) {
        return this.elementToNode.get(element);
    }

    public boolean hasNode(IElement element) {
        return this.elementToNode.containsKey(element);
    }

    // Tree operations
    public ITreeNode createNode(IElement element) {
        TreeNode node = new TreeNode(element);
        element.setTree(this);
        this.registerNode(node);
        return node;
    }

    private void registerNode(ITreeNode node) {
        this.elementToNode.put(node.getElement(), node);
        node.getElement().setTree(this);
        // Register all children recursively
        for (ITreeNode child : node.getChildren()) {
            this.registerNode(child);
        }
    }

    private void unregisterNode(ITreeNode node) {
        this.elementToNode.remove(node.getElement());
        node.getElement().setTree(null);
        // Unregister all children recursively
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

        // Collect all states used by this node and its children
        Set<State<?>> statesToCheck = new HashSet<>();
        this.walkDepthFirst(node, n -> {
            statesToCheck.addAll(n.getBoundStates());
        });

        this.unregisterNode(node);

        // Check if any states are no longer used and cleanup
        for (State<?> state : statesToCheck) {
            this.unregisterStateIfUnused(state);
        }

        // Call lifecycle
        node.getElement().onDetached();
    }

    // Re-composition (kill node and rebuild)
    public void recomposeNode(ITreeNode node) {
        if (!node.hasCompositionFunction()) {
            return;
        }

        // Properly detach all children (lifecycle, state cleanup, unregister)
        List<ITreeNode> children = new ArrayList<>(node.getChildren());
        for (ITreeNode child : children) {
            this.detachNode(child);
        }

        // Re-run composition function to rebuild children
        Runnable compositionFn = node.getCompositionFunction();
        if (compositionFn != null) {
            compositionFn.run();
        }
    }

    // Tree traversal
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

    // Find operations
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
        // Walk in reverse order (front to back) to find topmost element
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

    // Layout coordination
    public void measureTree(Constraints rootConstraints) {
        if (this.root != null) {
            this.measureNode(this.root, rootConstraints);
        }
    }

    public void placeTree(Position rootPosition) {
        if (this.root != null && this.root.getElement().getBounds() != null) {
            var size = this.root.getElement().getBounds().size();
            this.placeNode(this.root, new com.tridevmc.compound.ui.compose.layout.Bounds(rootPosition, size));
        }
    }

    /**
     * Recursively measure a node and its children (bottom-up).
     */
    private Size measureNode(ITreeNode node, Constraints constraints) {
        // Element can query children via getChildren() if it's a container
        return node.getElement().measure(constraints);
    }

    /**
     * Recursively place a node and its children (top-down).
     */
    private void placeNode(ITreeNode node, com.tridevmc.compound.ui.compose.layout.Bounds bounds) {
        // Element can query children via getChildren() if it's a container
        node.getElement().place(bounds);
    }

    // Event dispatch
    public void dispatchClick(int x, int y, MouseClickEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            // Walk up the tree firing handlers until consumed
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

    public void dispatchMouseMove(int x, int y) {
        // TODO: Track hover state and fire enter/exit events
        // This will be implemented when we need it
    }

    public void dispatchScroll(int x, int y, MouseScrollEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            // Walk up the tree firing handlers until consumed
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

    public void dispatchKeyPress(KeyEvent event) {
        // For now, dispatch to all elements
        // TODO: Implement focus system
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

    public void dispatchKeyRelease(KeyEvent event) {
        // For now, dispatch to all elements
        // TODO: Implement focus system
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

    // State change notification
    @Override
    public void onStateChanged(State<?> state) {
        // Find all nodes bound to this state and trigger re-composition
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
        // Check if any nodes are still bound to this state
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

    // Render helper
    public void renderTree(com.tridevmc.compound.ui.screen.IScreenContext context) {
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
