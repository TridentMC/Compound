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

import com.tridevmc.compound.ui.compose.animation.AnimationScheduler;
import com.tridevmc.compound.ui.compose.state.GuiRenderStateAdapter;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Manages the tree of nodes (like the DOM).
 * NOTE: This is internal to the framework and not exposed to elements or composition code.
 */
public class UITree {
    private ITreeNode root;
    private final Map<IElement, ITreeNode> elementToNode = new HashMap<>();
    private final AnimationScheduler animationScheduler = new AnimationScheduler();
    private Size rootSize;
    private Size lastWindowSize = new Size(-1, -1);

    public AnimationScheduler getAnimationScheduler() {
        return this.animationScheduler;
    }

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
        TreeNode node = new TreeNode(element, this);
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

        // Clean up state observers
        node.dispose();

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

        this.unregisterNode(node);
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
        this.walkDepthFirstWithDepth(start, 0, (node, depth) -> visitor.accept(node));
    }

    /**
     * Walk tree depth-first, providing depth to visitor.
     * Depth-first composition order: parent before children.
     */
    public void walkDepthFirstWithDepth(ITreeNode start, int depth, BiConsumer<ITreeNode, Integer> visitor) {
        if (start == null) {
            return;
        }
        visitor.accept(start, depth);
        for (ITreeNode child : start.getChildren()) {
            this.walkDepthFirstWithDepth(child, depth + 1, visitor);
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
        return this.findNodeAtWithViewport(this.root, x, y, null);
    }

    /**
     * Recursively find the deepest node at a position, respecting clipping viewports.
     *
     * @param node The current node being checked
     * @param x The x coordinate to check
     * @param y The y coordinate to check
     * @param activeViewport The current clipping viewport, or null if unrestricted
     * @return The deepest node at the position, or null if none found
     */
    private ITreeNode findNodeAtWithViewport(ITreeNode node, int x, int y, Bounds activeViewport) {
        IElement element = node.getElement();
        Bounds elementBounds = element.getBounds();

        if (elementBounds == null) {
            return null;
        }

        // Update viewport if this element clips
        Bounds viewport = activeViewport;
        if (element.getLayoutProperties().isClip()) {
            if (activeViewport != null) {
                viewport = activeViewport.intersection(elementBounds);
            } else {
                viewport = elementBounds;
            }
        }

        // Check if point is within the active viewport (if any)
        if (viewport != null && !viewport.contains(x, y)) {
            return null;  // Point is clipped, no children will be visible either
        }

        // Check children first (depth-first, deepest nodes have priority)
        ITreeNode result = null;
        for (ITreeNode child : node.getChildren()) {
            result = this.findNodeAtWithViewport(child, x, y, viewport);
            if (result != null) {
                break;  // Found a child node at this position
            }
        }

        // If no child was found, check if this node itself is at the position
        if (result == null && elementBounds.contains(x, y)) {
            return node;
        }

        return result;
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

        // Preserve existing bounds instead of forcing to Position.ORIGIN
        Bounds existingBounds = measureTarget.getElement().getBounds();
        Bounds bounds = existingBounds != null ? existingBounds : new Bounds(Position.ORIGIN, this.rootSize);
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

    public boolean dispatchScroll(int x, int y, MouseScrollEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;

            while (current != null) {
                for (var handler : current.getScrollHandlers()) {
                    boolean handled = handler.apply(event);
                    if (handled) {
                        return true;  // Event was handled
                    }
                }
                current = current.getParent();
            }
        }
        return false;  // Event was not handled
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

    /**
     * Request recomposition of a specific node.
     * Called directly by TreeNode composition observers.
     *
     * @param node the node to recompose
     */
    public void requestRecompose(ITreeNode node) {
        this.recomposeNode(node);
    }

    /**
     * Request remeasurement of a specific node without recomposition.
     * Called directly by TreeNode layout observers for layout-only state changes.
     *
     * @param node the node that needs remeasurement
     */
    public void requestRemeasure(ITreeNode node) {
        this.requestRemeasurement(node);
    }

    /**
     * Bind a node to a state.
     * Deprecated: Use node.bindCompositionState() or node.bindLayoutState() directly.
     *
     * @param node  the node to bind
     * @param state the state to bind to
     */
    @Deprecated
    public void bindNodeToState(ITreeNode node, State<?> state) {
        // Default to composition binding for backward compatibility
        node.bindCompositionState(state);
    }

    /**
     * Unbind a node from a state.
     * Deprecated: Use node.unbindCompositionState() or node.unbindLayoutState() directly.
     *
     * @param node  the node to unbind
     * @param state the state to unbind from
     */
    @Deprecated
    public void unbindNodeFromState(ITreeNode node, State<?> state) {
        node.unbindCompositionState(state);
        node.unbindLayoutState(state);
    }

    /**
     * Handle layout and rendering with window dimensions.
     * UITree tracks when window size changes and triggers measurement only when needed.
     */
    public void layoutAndRender(int width, int height, IScreenContext context) {
        if (this.root == null) {
            return;
        }

        // Update animations FIRST using Minecraft's tick system
        // This updates animation state values for the current tick
        this.animationScheduler.updateAnimations(context.getTicks());

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

        // Render tree recursively with clipping support
        // Note: GuiRenderStateAdapter was removed as it wasn't actually being used (see ADAPTER-ISSUE.md)
        this.renderNode(this.root, context, null);
    }

    /**
     * Recursively render a node and its children.
     * Handles clipping and viewport culling automatically.
     *
     * @param node The node to render
     * @param context The screen context for drawing
     * @param activeViewport The current clipping viewport, or null if unrestricted
     */
    private void renderNode(ITreeNode node, IScreenContext context, Bounds activeViewport) {
        IElement element = node.getElement();

        // Check if this element clips its children via layout properties
        boolean shouldClip = element.getLayoutProperties().isClip();
        boolean didEnableScissor = false;
        Bounds viewport = activeViewport;

        if (shouldClip) {
            Bounds clipBounds = element.getBounds();
            if (clipBounds != null) {
                // Intersect with parent viewport if any
                if (activeViewport != null) {
                    clipBounds = activeViewport.intersection(clipBounds);
                }

                viewport = clipBounds;
                context.enableScissor(
                    clipBounds.left(),
                    clipBounds.top(),
                    clipBounds.right(),
                    clipBounds.bottom()
                );
                didEnableScissor = true;
            }
        }

        // Render this element if it's a primitive and visible
        if (element.isVisible() && element instanceof IPrimitiveElement primitive) {
            // Optimization: only draw if element intersects viewport
            if (viewport == null || viewport.intersects(element.getBounds())) {
                primitive.draw(context);
            }
            // Huge performance win: skip draw() entirely for off-screen elements!
        }

        // Recursively render all children with current viewport
        for (ITreeNode child : node.getChildren()) {
            this.renderNode(child, context, viewport);
        }

        // Restore previous scissor state (only if we actually enabled it)
        if (didEnableScissor) {
            context.disableScissor();
        }
    }
}
