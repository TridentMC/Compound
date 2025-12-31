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

package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.ui.animation.AnimationScheduler;
import com.tridevmc.compound.ui.debug.DebugOverlayConfig;
import com.tridevmc.compound.ui.element.*;
import com.tridevmc.compound.ui.event.*;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.state.State;

import java.util.ArrayList;
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
    private final Map<IElement, ITreeNode> elementToNode = new HashMap<>();
    private final AnimationScheduler animationScheduler = new AnimationScheduler();
    private ITreeNode root;
    private Size rootSize;
    private Size lastWindowSize = new Size(-1, -1);
    private int lastMouseX, lastMouseY;
    private com.mojang.blaze3d.platform.cursor.CursorType requestedCursor = com.mojang.blaze3d.platform.cursor.CursorType.DEFAULT;
    private Set<ITreeNode> lastHoveredPath = new HashSet<>();
    private boolean pendingHoverUpdate = false;
    private ITreeNode focusedNode = null; // Global focus tracking

    public AnimationScheduler getAnimationScheduler() {
        return this.animationScheduler;
    }

    public ITreeNode getRoot() {
        return this.root;
    }

    public void setRoot(ITreeNode root) {
        this.root = root;
        if (root != null) {
            this.registerNode(root);
        }
    }

    public boolean hasRoot() {
        return this.root != null;
    }

    // Focus management
    public ITreeNode getFocusedNode() {
        return this.focusedNode;
    }

    public void requestFocus(ITreeNode node) {
        if (this.focusedNode == node) return;

        // Clear focus from previous node
        var previousFocused = this.focusedNode;
        this.focusedNode = null;

        // Notify the previous focused element that it lost focus (via handlers)
        if (previousFocused != null) {
            for (var handler : previousFocused.getFocusLostHandlers()) {
                handler.run();
            }
        }

        // Set new focused node
        this.focusedNode = node;

        // Notify the new focused element that it gained focus (via handlers)
        if (node != null) {
            for (var handler : node.getFocusGainedHandlers()) {
                handler.run();
            }
        }
    }

    public void clearFocus() {
        if (this.focusedNode != null) {
            var previousFocused = this.focusedNode;
            this.focusedNode = null;

            // Notify the element that it lost focus (via handlers)
            for (var handler : previousFocused.getFocusLostHandlers()) {
                handler.run();
            }
        }
    }

    public boolean hasFocus(ITreeNode node) {
        return this.focusedNode == node;
    }

    public ITreeNode getNodeForElement(IElement element) {
        return this.elementToNode.get(element);
    }

    public boolean hasNode(IElement element) {
        return this.elementToNode.containsKey(element);
    }

    public ITreeNode createNode(IElement element) {
        TreeNode node = new TreeNode(element, this);
        if (element instanceof IElementInternal internal) {
            internal.setNode(node);
        }
        this.registerNode(node);
        return node;
    }

    private void registerNode(ITreeNode node) {
        this.elementToNode.put(node.getElement(), node);
        for (ITreeNode child : node.getChildren()) {
            this.registerNode(child);
        }
    }

    private void unregisterNode(ITreeNode node) {
        this.elementToNode.remove(node.getElement());

        // Clear focus if this node is focused
        if (this.focusedNode == node) {
            clearFocus();
        }

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
     * @param node           The current node being checked
     * @param x              The x coordinate to check
     * @param y              The y coordinate to check
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
        if (node.getLayoutProperties().isClip()) {
            if (activeViewport != null) {
                viewport = activeViewport.intersection(elementBounds);
            } else {
                viewport = elementBounds;
            }
        }

        if (viewport != null && !viewport.contains(x, y)) {
            return null;
        }

        // Iterate in REVERSE order to hit-test top-most elements first
        List<ITreeNode> children = node.getChildren();
        ITreeNode result = null;
        for (int i = children.size() - 1; i >= 0; i--) {
            ITreeNode child = children.get(i);
            result = this.findNodeAtWithViewport(child, x, y, viewport);
            if (result != null) {
                break;  // Found a child node at this position
            }
        }

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

    /**
     * Measures a node and returns its size WITH margin included.
     * <p>
     * Steps:
     * 1. Account for margin in available constraints
     * 2. Apply layout properties (fixed/min/max/fillMax) to get element constraints
     * 3. Calculate content constraints (accounting for padding)
     * 4. Recursively measure children with content constraints
     * 5. Call element.measure() with pre-measured child sizes
     * 6. Add padding and constrain to element constraints
     * 7. Add margin to get final size
     * 8. Store and return
     */
    private Size measureNode(ITreeNode node, Constraints constraints) {
        LayoutProperties props = node.getLayoutProperties();
        if (props == null) {
            props = LayoutProperties.create();
        }

        // Step 1: Account for margin in available space
        int marginHorizontal = props.getMarginLeft() + props.getMarginRight();
        int marginVertical = props.getMarginTop() + props.getMarginBottom();

        Constraints constraintsWithoutMargin = new Constraints(
                Math.max(0, constraints.minWidth() - marginHorizontal),
                Math.max(0, constraints.maxWidth() - marginHorizontal),
                Math.max(0, constraints.minHeight() - marginVertical),
                Math.max(0, constraints.maxHeight() - marginVertical)
        );

        // Step 2: Apply layout properties to constraints BEFORE measuring children
        Constraints elementConstraints = this.applyLayoutPropertiesToConstraints(constraintsWithoutMargin, props);

        // Step 2.5: Relax min constraints for children
        // Children should not be forced to be as large as the parent's minimum size
        // They are constrained by the parent's maximum size
        Constraints childConstraints = new Constraints(
                0,
                elementConstraints.maxWidth(),
                0,
                elementConstraints.maxHeight()
        );

        // Step 3: Calculate content constraints (accounting for padding)
        Constraints contentConstraints = LayoutMath.calculateContentConstraints(childConstraints, props);

        // Step 4: Recursively measure children with content constraints
        List<Size> measuredChildren = new ArrayList<>();
        for (ITreeNode child : node.getChildren()) {
            Size childSize = this.measureNode(child, contentConstraints);
            measuredChildren.add(childSize);
        }

        // Step 5: Element calculates intrinsic size
        Size intrinsicSize = node.getElement().measure(contentConstraints, props, measuredChildren);

        // Step 6: Add padding to get element's total size and constrain to element constraints
        int withPaddingWidth = intrinsicSize.width() + props.getHorizontalPadding();
        int withPaddingHeight = intrinsicSize.height() + props.getVerticalPadding();
        Size elementSize = new Size(withPaddingWidth, withPaddingHeight);

        // Apply fillMax after measurement (not before, so children get loose constraints)
        int finalWidth = elementSize.width();
        int finalHeight = elementSize.height();
        if (props.isFillMaxWidth()) {
            finalWidth = constraintsWithoutMargin.maxWidth();
        }
        if (props.isFillMaxHeight()) {
            finalHeight = constraintsWithoutMargin.maxHeight();
        }

        // Constrain to element constraints (respects fixedSize, min/max)
        // However, allow exceeding maxWidth/maxHeight if explicitly set to Integer.MAX_VALUE (for scrolling content)
        int constrainedWidth = finalWidth;
        int constrainedHeight = finalHeight;

        if (props.getMaxWidth() != null && props.getMaxWidth() == Integer.MAX_VALUE) {
            // Allow exceeding parent maxWidth - only apply minWidth constraint
            constrainedWidth = Math.max(elementConstraints.minWidth(), finalWidth);
        } else {
            constrainedWidth = elementConstraints.constrainWidth(finalWidth);
        }

        if (props.getMaxHeight() != null && props.getMaxHeight() == Integer.MAX_VALUE) {
            // Allow exceeding parent maxHeight - only apply minHeight constraint
            constrainedHeight = Math.max(elementConstraints.minHeight(), finalHeight);
        } else {
            constrainedHeight = elementConstraints.constrainHeight(finalHeight);
        }

        Size finalSize = new Size(constrainedWidth, constrainedHeight);

        // Step 7: Add margin to get total size (what parent sees)
        int totalWidth = finalSize.width() + marginHorizontal;
        int totalHeight = finalSize.height() + marginVertical;
        Size sizeWithMargin = new Size(totalWidth, totalHeight);

        node.setMeasuredSize(sizeWithMargin);

        return sizeWithMargin;
    }

    /**
     * Applies layout properties to constraints to determine the element's target size.
     * This is called BEFORE measuring children so they receive the correct constraints.
     * <p>
     * Note: fillMax is NOT applied here - it's applied after measurement. This allows
     * children to be measured with loose constraints while the element itself fills max.
     */
    private Constraints applyLayoutPropertiesToConstraints(Constraints constraints, LayoutProperties props) {
        int minWidth = constraints.minWidth();
        int maxWidth = constraints.maxWidth();
        int minHeight = constraints.minHeight();
        int maxHeight = constraints.maxHeight();

        // Apply fixed size (tightest constraints)
        if (props.getFixedWidth() != null) {
            minWidth = props.getFixedWidth();
            maxWidth = props.getFixedWidth();
        }
        if (props.getFixedHeight() != null) {
            minHeight = props.getFixedHeight();
            maxHeight = props.getFixedHeight();
        }

        if (props.getMinWidth() != null) {
            minWidth = Math.max(minWidth, props.getMinWidth());
        }
        if (props.getMaxWidth() != null) {
            // If maxWidth is Integer.MAX_VALUE, use it to unbind the constraint (for scrolling content)
            if (props.getMaxWidth() == Integer.MAX_VALUE) {
                maxWidth = Integer.MAX_VALUE;
            } else {
                maxWidth = Math.min(maxWidth, props.getMaxWidth());
            }
        }
        if (props.getMinHeight() != null) {
            minHeight = Math.max(minHeight, props.getMinHeight());
        }
        if (props.getMaxHeight() != null) {
            // If maxHeight is Integer.MAX_VALUE, use it to unbind the constraint (for scrolling content)
            if (props.getMaxHeight() == Integer.MAX_VALUE) {
                maxHeight = Integer.MAX_VALUE;
            } else {
                maxHeight = Math.min(maxHeight, props.getMaxHeight());
            }
        }

        maxWidth = Math.max(minWidth, maxWidth);
        maxHeight = Math.max(minHeight, maxHeight);

        return new Constraints(minWidth, maxWidth, minHeight, maxHeight);
    }

    /**
     * Places a node and all its children.
     * <p>
     * Steps:
     * 1. Set element's bounds
     * 2. Get measured child sizes
     * 3. Call element.place() to get child bound allocations
     * 4. Apply child margins and recursively place children
     */
    private void placeNode(ITreeNode node, Bounds bounds) {
        IElement element = node.getElement();
        LayoutProperties props = node.getLayoutProperties();
        if (props == null) {
            props = LayoutProperties.create();
        }

        // Step 1: Set node's bounds (element's getBounds() delegates to node)
        node.setBounds(bounds);

        List<ITreeNode> children = node.getChildren();
        if (children.isEmpty()) {
            return;
        }

        // Step 2: Get measured child sizes
        List<Size> measuredChildren = children.stream()
                .map(ITreeNode::getMeasuredSize)
                .toList();

        // Step 3: Element calculates child bounds
        List<Bounds> childBounds = element.place(bounds, props, measuredChildren);

        if (childBounds.size() != children.size()) {
            throw new IllegalStateException(
                    "Element " + element.getClass().getSimpleName() + " returned " +
                            childBounds.size() + " bounds but has " + children.size() + " children"
            );
        }

        // Step 4: Apply margins and recursively place
        for (int i = 0; i < children.size(); i++) {
            ITreeNode child = children.get(i);
            Bounds allocatedBounds = childBounds.get(i);
            LayoutProperties childProps = child.getLayoutProperties();
            if (childProps == null) {
                childProps = LayoutProperties.create();
            }

            // Store what parent allocated (for debug overlay)
            child.setAllocatedBounds(allocatedBounds);

            int contentX = allocatedBounds.x() + childProps.getMarginLeft();
            int contentY = allocatedBounds.y() + childProps.getMarginTop();
            int contentWidth = allocatedBounds.width() - childProps.getMarginLeft() - childProps.getMarginRight();
            int contentHeight = allocatedBounds.height() - childProps.getMarginTop() - childProps.getMarginBottom();

            Bounds contentBounds = new Bounds(
                    new Position(contentX, contentY),
                    new Size(contentWidth, contentHeight)
            );

            this.placeNode(child, contentBounds);
        }
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
        this.pendingHoverUpdate = true;
    }

    public boolean dispatchClick(int x, int y, MouseClickEvent event) {
        ITreeNode node = this.findNodeAt(x, y);

        // Check if we clicked on an element that can receive focus (has focus handlers registered)
        boolean clickedOnFocusable = false;
        if (node != null) {
            ITreeNode current = node;
            while (current != null) {
                if (!current.getFocusGainedHandlers().isEmpty()) {
                    clickedOnFocusable = true;
                    break;
                }
                current = current.getParent();
            }
        }

        // Clear focus if clicking outside focusable elements (focusable elements will call scope.requestFocus() in their handlers)
        if (!clickedOnFocusable && this.focusedNode != null) {
            clearFocus();
        }

        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && !consumed) {
                for (var handler : current.getClickHandlers()) {
                    if (handler.apply(event)) {
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed;
        }
        return false;
    }

    public com.mojang.blaze3d.platform.cursor.CursorType getRequestedCursor() {
        return this.requestedCursor;
    }

    private void updateHoverState() {
        int x = this.lastMouseX;
        int y = this.lastMouseY;
        ITreeNode node = this.findNodeAt(x, y);

        com.mojang.blaze3d.platform.cursor.CursorType cursor = com.mojang.blaze3d.platform.cursor.CursorType.DEFAULT;
        if (node != null) {
            ITreeNode current = node;
            while (current != null) {
                Bounds bounds = current.getElement().getBounds();
                if (bounds != null) {
                    int localX = x - bounds.x();
                    int localY = y - bounds.y();
                    var nodeCursor = current.getElement().getCursor(localX, localY);
                    if (nodeCursor != null) {
                        cursor = nodeCursor;
                        break;
                    }
                }
                current = current.getParent();
            }
        }
        this.requestedCursor = cursor;

        // Build the current hovered path (node and all ancestors)
        Set<ITreeNode> currentHoveredPath = new HashSet<>();
        if (node != null) {
            ITreeNode current = node;
            while (current != null) {
                currentHoveredPath.add(current);
                current = current.getParent();
            }
        }

        // Find nodes that were exited (in last path but not in current path)
        Set<ITreeNode> exitedNodes = new HashSet<>(lastHoveredPath);
        exitedNodes.removeAll(currentHoveredPath);

        // Find nodes that were entered (in current path but not in last path)
        Set<ITreeNode> enteredNodes = new HashSet<>(currentHoveredPath);
        enteredNodes.removeAll(lastHoveredPath);

        if (!exitedNodes.isEmpty()) {
            for (ITreeNode exitedNode : exitedNodes) {
                for (var handler : exitedNode.getMouseExitHandlers()) {
                    handler.run();
                }
            }
        }

        if (!enteredNodes.isEmpty()) {
            for (ITreeNode enteredNode : enteredNodes) {
                for (var handler : enteredNode.getMouseEnterHandlers()) {
                    handler.run();
                }
            }
        }

        lastHoveredPath = currentHoveredPath;
    }

    public void dispatchMouseMove(int x, int y, MouseMoveEvent event) {
        this.lastMouseX = x;
        this.lastMouseY = y;
        this.updateHoverState();

        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && !consumed) {
                for (var handler : current.getMouseMoveHandlers()) {
                    if (handler.apply(event)) {
                        consumed = true;
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

    public boolean dispatchKeyPress(KeyInputEvent event) {
        // Dispatch keyboard events only to the focused element
        if (this.focusedNode != null) {
            for (var handler : this.focusedNode.getKeyPressHandlers()) {
                if (handler.apply(event)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean dispatchKeyRelease(KeyInputEvent event) {
        // Dispatch keyboard events only to the focused element
        if (this.focusedNode != null) {
            for (var handler : this.focusedNode.getKeyReleaseHandlers()) {
                if (handler.apply(event)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean dispatchCharTyped(CharEvent event) {
        // Dispatch character events only to the focused element
        if (this.focusedNode != null) {
            for (var handler : this.focusedNode.getCharTypedHandlers()) {
                if (handler.apply(event)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean dispatchMouseRelease(int x, int y, MouseReleaseEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && !consumed) {
                for (var handler : current.getMouseReleaseHandlers()) {
                    if (handler.apply(event)) {
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed;
        }
        return false;
    }

    public boolean dispatchMouseDrag(int x, int y, MouseDragEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && !consumed) {
                for (var handler : current.getMouseDragHandlers()) {
                    if (handler.apply(event)) {
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed;
        }
        return false;
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
            this.updateHoverState(); // Re-evaluate hover state after layout changes
        } else if (this.pendingHoverUpdate) {
            this.updateHoverState();
            this.pendingHoverUpdate = false;
        }

        this.renderTree(context);
    }

    public void renderTree(IScreenContext context) {
        if (this.root == null) {
            return;
        }

        this.renderNode(this.root, context, null);

        // Debug overlay rendered AFTER normal rendering (on top)
        // Individual elements have zero awareness of debug mode
        if (DebugOverlayConfig.get().isEnabled()) {
            this.renderDebugOverlay(context);
        }
    }

    /**
     * Recursively render a node and its children.
     * Handles clipping and viewport culling automatically.
     *
     * @param node           The node to render
     * @param context        The screen context for drawing
     * @param activeViewport The current clipping viewport, or null if unrestricted
     */
    private void renderNode(ITreeNode node, IScreenContext context, Bounds activeViewport) {
        IElement element = node.getElement();

        boolean shouldClip = node.getLayoutProperties().isClip();
        boolean didEnableScissor = false;
        Bounds viewport = activeViewport;

        if (shouldClip) {
            Bounds clipBounds = element.getBounds();
            if (clipBounds != null) {
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

        if (element instanceof IPrimitiveElement primitive) {
            // Optimization: only draw if element intersects viewport
            if (viewport == null || viewport.intersects(element.getBounds())) {
                primitive.draw(context);
            }
        }

        for (ITreeNode child : node.getChildren()) {
            this.renderNode(child, context, viewport);
        }

        if (didEnableScissor) {
            context.disableScissor();
        }
    }

    /**
     * Renders debug overlay showing bounds, margins, and padding for the hovered element
     * and its ancestors. Includes layout driver visualization (alignment springs, spacing bars).
     *
     * @param context the screen context for drawing
     */
    private void renderDebugOverlay(IScreenContext context) {
        var config = DebugOverlayConfig.get();

        // Find the currently hovered node
        ITreeNode hoveredNode = this.findNodeAt(this.lastMouseX, this.lastMouseY);
        if (hoveredNode == null) {
            return;
        }

        // Build the ancestor chain from root to hovered node
        var ancestorChain = new ArrayList<ITreeNode>();
        ITreeNode current = hoveredNode;
        while (current != null) {
            ancestorChain.add(0, current); // Add at beginning to get root-to-node order
            current = current.getParent();
        }

        // 1. Render Ancestors (Dimmed Box Model)
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode node = ancestorChain.get(i);
            drawBoxModel(context, node, false);
        }

        // 2. Render Allocated Bounds (what parent gave us) vs Actual Bounds
        drawAllocatedVsActual(context, hoveredNode);

        // 3. Render Hovered Element (Full Box Model)
        drawBoxModel(context, hoveredNode, true);

        // 4. Render Layout Drivers for ALL Column/Row ancestors in the chain
        // This is critical: For composed elements like Label->Text, the Column
        // might be 2+ levels up, not the immediate parent!
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode ancestor = ancestorChain.get(i);
            ITreeNode childInAncestor = ancestorChain.get(i + 1);
            
            // Visualize spacing for Column/Row ancestors
            if (ancestor.getElement() instanceof Column || ancestor.getElement() instanceof Row) {
                drawLayoutDrivers(context, childInAncestor, ancestor, childInAncestor == hoveredNode);
            }
            
            // Visualize alignment springs for ancestors with contentAlignment (Stack, Box, etc.)
            // This is key for showing WHY text is centered in buttons!
            var ancestorProps = ancestor.getLayoutProperties();
            if (ancestorProps != null && ancestorProps.getContentAlignment() != null) {
                drawAlignmentDrivers(context, childInAncestor, ancestor);
            }
        }

        // 5. Render Enhanced Info Panel
        drawDebugInfoPanel(context, hoveredNode, ancestorChain);
    }

    /**
     * Visualizes the difference between allocated bounds (what parent gave) and actual bounds.
     * This shows WHY there are gaps around elements due to centering/alignment.
     */
    private void drawAllocatedVsActual(IScreenContext context, ITreeNode node) {
        var allocatedBounds = node.getAllocatedBounds();
        var actualBounds = node.getBounds();
        
        if (allocatedBounds == null || actualBounds == null) {
            return;
        }
        
        // Only draw if there's actually a difference
        if (allocatedBounds.equals(actualBounds)) {
            return;
        }
        
        int unusedColor = DebugOverlayConfig.COLOR_UNUSED_SPACE;
        
        // Draw unused space as yellow fill
        // Top gap
        if (actualBounds.y() > allocatedBounds.y()) {
            float gapHeight = actualBounds.y() - allocatedBounds.y();
            context.drawRect(allocatedBounds.x(), allocatedBounds.y(), 
                    allocatedBounds.width(), gapHeight, unusedColor);
        }
        
        // Bottom gap
        float actualBottom = actualBounds.y() + actualBounds.height();
        float allocBottom = allocatedBounds.y() + allocatedBounds.height();
        if (actualBottom < allocBottom) {
            float gapHeight = allocBottom - actualBottom;
            context.drawRect(allocatedBounds.x(), actualBottom, 
                    allocatedBounds.width(), gapHeight, unusedColor);
        }
        
        // Left gap
        if (actualBounds.x() > allocatedBounds.x()) {
            float gapWidth = actualBounds.x() - allocatedBounds.x();
            context.drawRect(allocatedBounds.x(), actualBounds.y(), 
                    gapWidth, actualBounds.height(), unusedColor);
        }
        
        // Right gap
        float actualRight = actualBounds.x() + actualBounds.width();
        float allocRight = allocatedBounds.x() + allocatedBounds.width();
        if (actualRight < allocRight) {
            float gapWidth = allocRight - actualRight;
            context.drawRect(actualRight, actualBounds.y(), 
                    gapWidth, actualBounds.height(), unusedColor);
        }
        
        // Draw dashed outline for allocated bounds
        int allocOutlineColor = DebugOverlayConfig.COLOR_ALLOCATED_OUTLINE;
        drawDashedRect(context, allocatedBounds.x(), allocatedBounds.y(), 
                allocatedBounds.width(), allocatedBounds.height(), allocOutlineColor, 4);
    }
    
    /**
     * Draws a dashed rectangle outline.
     */
    private void drawDashedRect(IScreenContext context, float x, float y, float w, float h, int color, int dashLen) {
        // Top edge
        for (float dx = 0; dx < w; dx += dashLen * 2) {
            float len = Math.min(dashLen, w - dx);
            context.drawRect(x + dx, y, len, 1, color);
        }
        // Bottom edge
        for (float dx = 0; dx < w; dx += dashLen * 2) {
            float len = Math.min(dashLen, w - dx);
            context.drawRect(x + dx, y + h - 1, len, 1, color);
        }
        // Left edge
        for (float dy = 0; dy < h; dy += dashLen * 2) {
            float len = Math.min(dashLen, h - dy);
            context.drawRect(x, y + dy, 1, len, color);
        }
        // Right edge
        for (float dy = 0; dy < h; dy += dashLen * 2) {
            float len = Math.min(dashLen, h - dy);
            context.drawRect(x + w - 1, y + dy, 1, len, color);
        }
    }

    /**
     * Draws the CSS Box Model (Margin, Padding, Content, Outline) for a node.
     */
    private void drawBoxModel(IScreenContext context, ITreeNode node, boolean isHovered) {
        var bounds = node.getBounds();
        if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }

        var props = node.getLayoutProperties();
        if (props == null) props = LayoutProperties.create();

        float x = bounds.x();
        float y = bounds.y();
        float w = bounds.width();
        float h = bounds.height();

        int marginColor = isHovered ? DebugOverlayConfig.COLOR_MARGIN : DebugOverlayConfig.COLOR_ANCESTOR_MARGIN;
        int paddingColor = isHovered ? DebugOverlayConfig.COLOR_PADDING : DebugOverlayConfig.COLOR_ANCESTOR_PADDING;
        int outlineColor = isHovered ? DebugOverlayConfig.COLOR_BOUNDS_OUTLINE : DebugOverlayConfig.COLOR_ANCESTOR_OUTLINE;

        // Draw Margin (Orange)
        int mL = props.getMarginLeft();
        int mT = props.getMarginTop();
        int mR = props.getMarginRight();
        int mB = props.getMarginBottom();

        if (mL > 0) context.drawRect(x - mL, y, mL, h, marginColor);
        if (mT > 0) context.drawRect(x, y - mT, w, mT, marginColor);
        if (mR > 0) context.drawRect(x + w, y, mR, h, marginColor);
        if (mB > 0) context.drawRect(x, y + h, w, mB, marginColor);

        // Draw Padding (Green)
        int pL = props.getPaddingLeft();
        int pT = props.getPaddingTop();
        int pR = props.getPaddingRight();
        int pB = props.getPaddingBottom();

        if (pL > 0) context.drawRect(x, y, pL, h, paddingColor);
        if (pT > 0) context.drawRect(x + pL, y, w - pL - pR, pT, paddingColor);
        if (pR > 0) context.drawRect(x + w - pR, y, pR, h, paddingColor);
        if (pB > 0) context.drawRect(x + pL, y + h - pB, w - pL - pR, pB, paddingColor);

        // Draw Outline
        context.drawRectOutline(x, y, w, h, outlineColor, 1);

        // If hovered, draw detailed margin/padding labels
        if (isHovered) {
             if (mL > 0) drawArrowLabel(context, x - mL/2f, y + h/2f, mL, true, marginColor);
             if (mR > 0) drawArrowLabel(context, x + w + mR/2f, y + h/2f, mR, true, marginColor);
             if (mT > 0) drawArrowLabel(context, x + w/2f, y - mT/2f, mT, false, marginColor);
             if (mB > 0) drawArrowLabel(context, x + w/2f, y + h + mB/2f, mB, false, marginColor);

             if (pL > 0) drawArrowLabel(context, x + pL/2f, y + h/2f, pL, true, paddingColor);
             if (pR > 0) drawArrowLabel(context, x + w - pR/2f, y + h/2f, pR, true, paddingColor);
             if (pT > 0) drawArrowLabel(context, x + w/2f, y + pT/2f, pT, false, paddingColor);
             if (pB > 0) drawArrowLabel(context, x + w/2f, y + h - pB/2f, pB, false, paddingColor);
        }
    }

    /**
     * Visualizes spacing bars for Column/Row parents.
     * @param isDirectChild if true, the child is the directly hovered element (shows bright labels)
     */
    private void drawLayoutDrivers(IScreenContext context, ITreeNode child, ITreeNode parent, boolean isDirectChild) {
        var parentProps = parent.getLayoutProperties();
        var parentElement = parent.getElement();
        if (parentProps == null) return;
        
        var parentBounds = parent.getBounds();
        var childBounds = child.getBounds();
        if (parentBounds == null || childBounds == null) return;

        // --- Spacing Bars for Column/Row ---
        if (parentElement instanceof Column || parentElement instanceof Row) {
            boolean isColumn = parentElement instanceof Column;
            var siblings = parent.getChildren();
            int spacingColor = DebugOverlayConfig.COLOR_SPACING;
            int dimmedSpacingColor = 0x40E91E63; // Dimmed pink for non-adjacent
            
            // Find index of target child
            int childIndex = -1;
            for (int i = 0; i < siblings.size(); i++) {
                if (siblings.get(i) == child) {
                    childIndex = i;
                    break;
                }
            }

            // Draw spacing gaps
            for (int i = 0; i < siblings.size() - 1; i++) {
                var node1 = siblings.get(i);
                var node2 = siblings.get(i + 1);
                var b1 = node1.getBounds();
                var b2 = node2.getBounds();
                
                if (b1 != null && b2 != null) {
                    // Is this gap adjacent to the target element?
                    boolean isAdjacentGap = (i == childIndex - 1) || (i == childIndex);
                    // Use bright color for adjacent, dimmed for others
                    int color = isAdjacentGap ? spacingColor : dimmedSpacingColor;
                    
                    if (isColumn) {
                        float gapY = b1.y() + b1.height();
                        float gapH = b2.y() - gapY;
                        if (gapH > 0 && gapH < 100) {
                            float gapX = Math.max(b1.x(), b2.x());
                            float gapW = Math.min(b1.width(), b2.width());
                            context.drawRect(gapX, gapY, gapW, gapH, color);
                            
                            // Draw pixel label on adjacent gaps
                            if (isAdjacentGap && gapH >= 2) {
                                drawArrowLabel(context, gapX + gapW/2f, gapY + gapH/2f, (int)gapH, false, color);
                            }
                        }
                    } else {
                        float gapX = b1.x() + b1.width();
                        float gapW = b2.x() - gapX;
                        if (gapW > 0 && gapW < 100) {
                            float gapY = Math.max(b1.y(), b2.y());
                            float gapH = Math.min(b1.height(), b2.height());
                            context.drawRect(gapX, gapY, gapW, gapH, color);
                            
                            // Draw pixel label on adjacent gaps
                            if (isAdjacentGap && gapW >= 2) {
                                drawArrowLabel(context, gapX + gapW/2f, gapY + gapH/2f, (int)gapW, true, color);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Visualizes alignment springs for non-Column/Row parents (Stack, Box, etc.)
     */
    private void drawAlignmentDrivers(IScreenContext context, ITreeNode child, ITreeNode parent) {
        var parentProps = parent.getLayoutProperties();
        if (parentProps == null) return;
        
        var parentBounds = parent.getBounds();
        var childBounds = child.getBounds();
        if (parentBounds == null || childBounds == null) return;

        // Alignment Springs (Yellow)
        Alignment align = parentProps.getContentAlignment();

        if (align != null) {
            int springColor = DebugOverlayConfig.COLOR_ALIGNMENT;
            float px = parentBounds.x() + parentProps.getPaddingLeft();
            float py = parentBounds.y() + parentProps.getPaddingTop();
            float pw = parentBounds.width() - parentProps.getPaddingLeft() - parentProps.getPaddingRight();
            float ph = parentBounds.height() - parentProps.getPaddingTop() - parentProps.getPaddingBottom();
            
            float cx = childBounds.x();
            float cy = childBounds.y();
            float cw = childBounds.width();
            float ch = childBounds.height();

            // Horizontal Springs
            if (align == Alignment.CENTER || align == Alignment.CENTER_LEFT || align == Alignment.CENTER_RIGHT || 
                align == Alignment.TOP_CENTER || align == Alignment.BOTTOM_CENTER) {
                
                // Left Spring
                if (cx > px) {
                    float w = cx - px;
                    context.drawRect(px, cy, w, ch, springColor);
                    context.drawRect(px, cy + ch/2f, w, 1, 0xFFFFFF00);
                }
                
                // Right Spring
                if (cx + cw < px + pw) {
                    float w = (px + pw) - (cx + cw);
                    context.drawRect(cx + cw, cy, w, ch, springColor);
                    context.drawRect(cx + cw, cy + ch/2f, w, 1, 0xFFFFFF00);
                }
            }
            
            // Vertical Springs (for vertical centering)
            if (align == Alignment.CENTER || align == Alignment.TOP_CENTER || align == Alignment.BOTTOM_CENTER ||
                align == Alignment.CENTER_LEFT || align == Alignment.CENTER_RIGHT) {
                
                // Top Spring
                if (cy > py) {
                    float h = cy - py;
                    context.drawRect(cx, py, cw, h, springColor);
                    context.drawRect(cx + cw/2f, py, 1, h, 0xFFFFFF00);
                }
                
                // Bottom Spring
                if (cy + ch < py + ph) {
                    float h = (py + ph) - (cy + ch);
                    context.drawRect(cx, cy + ch, cw, h, springColor);
                    context.drawRect(cx + cw/2f, cy + ch, 1, h, 0xFFFFFF00);
                }
            }
        }
    }

    /**
     * Draws the enhanced layout info panel with hierarchy, bounds analysis, and gap explanations.
     */
    private void drawDebugInfoPanel(IScreenContext context, ITreeNode node, List<ITreeNode> ancestorChain) {
        var element = node.getElement();
        var bounds = node.getBounds();
        var allocatedBounds = node.getAllocatedBounds();
        var measuredSize = node.getMeasuredSize();
        var parent = node.getParent();
        var props = node.getLayoutProperties();
        if (props == null) props = LayoutProperties.create();

        var lines = new ArrayList<String>();
        var colors = new ArrayList<Integer>();

        // Title with element type and size
        String title = element.getClass().getSimpleName() + " (" + (int)bounds.width() + "×" + (int)bounds.height() + ")";
        lines.add(title);
        colors.add(0xFFFFFFFF);

        // Hierarchy breadcrumb (last 4 ancestors max)
        if (ancestorChain.size() > 1) {
            var breadcrumb = new StringBuilder();
            int start = Math.max(0, ancestorChain.size() - 4);
            for (int i = start; i < ancestorChain.size(); i++) {
                if (i > start) breadcrumb.append(" > ");
                breadcrumb.append(ancestorChain.get(i).getElement().getClass().getSimpleName());
            }
            lines.add(breadcrumb.toString());
            colors.add(0xFF888888);
        }

        lines.add("");
        colors.add(0xFF888888);

        // Bounds Analysis Section
        lines.add("BOUNDS ANALYSIS");
        colors.add(0xFF4FC3F7); // Light blue header

        lines.add("  Position: (" + (int)bounds.x() + ", " + (int)bounds.y() + ")");
        colors.add(0xFFCCCCCC);

        if (measuredSize != null) {
            lines.add("  Measured: " + measuredSize.width() + "×" + measuredSize.height());
            colors.add(0xFFCCCCCC);
        }

        // Show allocated vs actual bounds if different
        if (allocatedBounds != null && !allocatedBounds.equals(bounds)) {
            lines.add("  Allocated: " + (int)allocatedBounds.width() + "×" + (int)allocatedBounds.height());
            colors.add(0xFFFFFF00); // Yellow - this is the key info!
            
            // Calculate and show gaps
            int gapTop = bounds.y() - allocatedBounds.y();
            int gapBottom = (allocatedBounds.y() + allocatedBounds.height()) - (bounds.y() + bounds.height());
            int gapLeft = bounds.x() - allocatedBounds.x();
            int gapRight = (allocatedBounds.x() + allocatedBounds.width()) - (bounds.x() + bounds.width());
            
            if (gapTop > 0 || gapBottom > 0) {
                lines.add("  Gap V: ↑" + gapTop + "px ↓" + gapBottom + "px");
                colors.add(0xFFFFFF00);
            }
            if (gapLeft > 0 || gapRight > 0) {
                lines.add("  Gap H: ←" + gapLeft + "px →" + gapRight + "px");
                colors.add(0xFFFFFF00);
            }
        }

        // Parent layout info
        if (parent != null) {
            lines.add("");
            colors.add(0xFF888888);
            
            lines.add("PARENT LAYOUT (" + parent.getElement().getClass().getSimpleName() + ")");
            colors.add(0xFF81C784); // Light green header
            
            var pProps = parent.getLayoutProperties();
            if (pProps != null) {
                // Show alignment that caused centering
                Alignment align = pProps.getContentAlignment();
                if (align == null && parent.getElement() instanceof Column) align = pProps.getHorizontalAlignment();
                if (align == null && parent.getElement() instanceof Row) align = pProps.getVerticalAlignment();
                
                if (align != null) {
                    lines.add("  align: " + align.name());
                    colors.add(0xFFFFEB3B); // Yellow
                }
                
                if (pProps.getSpacing() > 0) {
                    lines.add("  spacing: " + pProps.getSpacing() + "px");
                    colors.add(0xFFFF69B4); // Pink
                }
                
                int pPad = pProps.getPaddingLeft() + pProps.getPaddingRight() + 
                           pProps.getPaddingTop() + pProps.getPaddingBottom();
                if (pPad > 0) {
                    lines.add("  padding: " + pProps.getPaddingTop() + " " + pProps.getPaddingRight() + 
                             " " + pProps.getPaddingBottom() + " " + pProps.getPaddingLeft());
                    colors.add(0xFF4CAF50); // Green
                }
            }
        }
        
        // Sibling context - search ancestor chain for Column/Row containers
        // This is crucial for composed elements like Label->Text where Column is 2 levels up
        for (int i = 0; i < ancestorChain.size() - 1; i++) {
            ITreeNode ancestor = ancestorChain.get(i);
            ITreeNode childInAncestor = ancestorChain.get(i + 1);
            
            if (ancestor.getElement() instanceof Column || ancestor.getElement() instanceof Row) {
                boolean isColumn = ancestor.getElement() instanceof Column;
                var siblings = ancestor.getChildren();
                int childIndex = -1;
                
                for (int j = 0; j < siblings.size(); j++) {
                    if (siblings.get(j) == childInAncestor) {
                        childIndex = j;
                        break;
                    }
                }
                
                if (childIndex != -1 && siblings.size() > 1) {
                    lines.add("");
                    colors.add(0xFF888888);
                    
                    String containerName = ancestor.getElement().getClass().getSimpleName();
                    var ancestorProps = ancestor.getLayoutProperties();
                    int spacing = ancestorProps != null ? ancestorProps.getSpacing() : 0;
                    
                    lines.add("LAYOUT IN " + containerName + " (spacing=" + spacing + ")");
                    colors.add(0xFFFF69B4); // Pink header
                    
                    // Previous sibling
                    if (childIndex > 0) {
                        var prevNode = siblings.get(childIndex - 1);
                        var prevBounds = prevNode.getBounds();
                        var prevElement = prevNode.getElement();
                        var childBounds = childInAncestor.getBounds();
                        
                        String prevName = prevElement.getClass().getSimpleName();
                        if (prevBounds != null) {
                            prevName += " (" + (int)prevBounds.width() + "×" + (int)prevBounds.height() + ")";
                        }
                        lines.add("  prev: " + prevName);
                        colors.add(0xFFCCCCCC);
                        
                        // Calculate gap between prev and child
                        if (prevBounds != null && childBounds != null) {
                            int gap = isColumn ? 
                                childBounds.y() - (prevBounds.y() + prevBounds.height()) :
                                childBounds.x() - (prevBounds.x() + prevBounds.width());
                            if (gap > 0) {
                                lines.add("  ↕ gap above: " + gap + "px");
                                colors.add(0xFFFF69B4); // Pink
                            }
                        }
                    }
                    
                    // Current element indicator
                    var childBounds = childInAncestor.getBounds();
                    String childName = childInAncestor.getElement().getClass().getSimpleName();
                    if (childBounds != null) {
                        childName += " (" + (int)childBounds.width() + "×" + (int)childBounds.height() + ")";
                    }
                    lines.add("  → this: " + childName);
                    colors.add(0xFFFFFFFF);
                    
                    // Next sibling
                    if (childIndex < siblings.size() - 1) {
                        var nextNode = siblings.get(childIndex + 1);
                        var nextBounds = nextNode.getBounds();
                        var nextElement = nextNode.getElement();
                        
                        // Calculate gap between child and next
                        if (nextBounds != null && childBounds != null) {
                            int gap = isColumn ?
                                nextBounds.y() - (childBounds.y() + childBounds.height()) :
                                nextBounds.x() - (childBounds.x() + childBounds.width());
                            if (gap > 0) {
                                lines.add("  ↕ gap below: " + gap + "px");
                                colors.add(0xFFFF69B4);
                            }
                        }
                        
                        String nextName = nextElement.getClass().getSimpleName();
                        if (nextBounds != null) {
                            nextName += " (" + (int)nextBounds.width() + "×" + (int)nextBounds.height() + ")";
                        }
                        lines.add("  next: " + nextName);
                        colors.add(0xFFCCCCCC);
                    }
                    
                    // Only show one Column/Row context (the innermost one)
                    break;
                }
            }
        }
        
        // Centering context - search ancestor chain for contentAlignment
        // Iterate in REVERSE so we find the INNERMOST ancestor (closest to hovered element)
        // This shows WHY text is centered in buttons (Stack with contentAlignment=CENTER)
        for (int i = ancestorChain.size() - 2; i >= 0; i--) {
            ITreeNode ancestor = ancestorChain.get(i);
            ITreeNode childInAncestor = ancestorChain.get(i + 1);
            
            var ancestorProps = ancestor.getLayoutProperties();
            if (ancestorProps != null && ancestorProps.getContentAlignment() != null) {
                Alignment align = ancestorProps.getContentAlignment();
                var ancestorBounds = ancestor.getBounds();
                var childBounds = childInAncestor.getBounds();
                
                lines.add("");
                colors.add(0xFF888888);
                
                String ancestorName = ancestor.getElement().getClass().getSimpleName();
                lines.add("CENTERING (" + ancestorName + ")");
                colors.add(0xFFFFEB3B); // Yellow header
                
                lines.add("  contentAlignment: " + align.name());
                colors.add(0xFFFFEB3B);
                
                if (ancestorBounds != null && childBounds != null) {
                    // Calculate centering gaps (accounting for padding)
                    float px = ancestorBounds.x() + ancestorProps.getPaddingLeft();
                    float py = ancestorBounds.y() + ancestorProps.getPaddingTop();
                    float pw = ancestorBounds.width() - ancestorProps.getPaddingLeft() - ancestorProps.getPaddingRight();
                    float ph = ancestorBounds.height() - ancestorProps.getPaddingTop() - ancestorProps.getPaddingBottom();
                    
                    int leftGap = (int)(childBounds.x() - px);
                    int rightGap = (int)((px + pw) - (childBounds.x() + childBounds.width()));
                    int topGap = (int)(childBounds.y() - py);
                    int bottomGap = (int)((py + ph) - (childBounds.y() + childBounds.height()));
                    
                    if (leftGap > 0 || rightGap > 0) {
                        lines.add("  H gaps: ←" + leftGap + "px  →" + rightGap + "px");
                        colors.add(0xFFFFEB3B);
                    }
                    if (topGap > 0 || bottomGap > 0) {
                        lines.add("  V gaps: ↑" + topGap + "px  ↓" + bottomGap + "px");
                        colors.add(0xFFFFEB3B);
                    }
                }
                
                // Only show innermost centering ancestor
                break;
            }
        }

        // This element's properties
        int mTot = props.getMarginLeft() + props.getMarginRight() + props.getMarginTop() + props.getMarginBottom();
        int pTot = props.getPaddingLeft() + props.getPaddingRight() + props.getPaddingTop() + props.getPaddingBottom();
        
        if (mTot > 0 || pTot > 0 || props.isFillMaxWidth() || props.isFillMaxHeight()) {
            lines.add("");
            colors.add(0xFF888888);
            
            lines.add("THIS ELEMENT");
            colors.add(0xFFFFAB40); // Orange header
            
            if (mTot > 0) {
                lines.add("  margin: " + props.getMarginTop() + " " + props.getMarginRight() + 
                         " " + props.getMarginBottom() + " " + props.getMarginLeft());
                colors.add(0xFFFFA500); // Orange
            }
            
            if (pTot > 0) {
                lines.add("  padding: " + props.getPaddingTop() + " " + props.getPaddingRight() + 
                         " " + props.getPaddingBottom() + " " + props.getPaddingLeft());
                colors.add(0xFF4CAF50); // Green
            }
            
            if (props.isFillMaxWidth() || props.isFillMaxHeight()) {
                String fill = "";
                if (props.isFillMaxWidth() && props.isFillMaxHeight()) fill = "fillMax";
                else if (props.isFillMaxWidth()) fill = "fillMaxWidth";
                else fill = "fillMaxHeight";
                lines.add("  " + fill);
                colors.add(0xFFCE93D8); // Purple
            }
        }

        // Calculate panel dimensions
        int lineHeight = 10;
        int panelPadding = 6;
        float panelW = 220;
        float panelH = lines.size() * lineHeight + panelPadding * 2;
        
        // Position panel in top-right, but ensure it's visible
        float px = context.getWidth() - panelW - 5;
        float py = 5;

        // Draw panel background
        context.drawRect(px, py, panelW, panelH, 0xE8000000);
        context.drawRectOutline(px, py, panelW, panelH, 0xFF333333, 1);

        // Draw lines
        for (int i = 0; i < lines.size(); i++) {
            context.drawString(lines.get(i), px + panelPadding, py + panelPadding + i * lineHeight, colors.get(i));
        }
    }

    /**
     * Draws a measurement arrow label at the specified position.
     */
    private void drawArrowLabel(IScreenContext context, float centerX, float centerY,
                                 int value, boolean horizontal, int color) {
        String text = String.valueOf(value);
        int textWidth = context.getFont().width(text);
        int textHeight = 7;

        float textX = centerX - textWidth / 2f;
        float textY = centerY - textHeight / 2f;

        // Draw background for readability
        context.drawRect(textX - 1, textY - 1, textWidth + 2, textHeight + 2, 0xAA000000);

        // Draw arrow lines
        int arrowColor = 0xFFFFFFFF;
        if (horizontal) {
            // Horizontal arrows
            float arrowLen = Math.max(4, (value - textWidth) / 2f - 4);
            if (arrowLen > 2) {
                context.drawRect(centerX - textWidth / 2f - arrowLen, centerY - 0.5f, arrowLen - 2, 1, arrowColor);
                context.drawRect(centerX + textWidth / 2f + 2, centerY - 0.5f, arrowLen - 2, 1, arrowColor);
            }
        } else {
            // Vertical arrows
            float arrowLen = Math.max(4, (value - textHeight) / 2f - 4);
            if (arrowLen > 2) {
                context.drawRect(centerX - 0.5f, centerY - textHeight / 2f - arrowLen, 1, arrowLen - 2, arrowColor);
                context.drawRect(centerX - 0.5f, centerY + textHeight / 2f + 2, 1, arrowLen - 2, arrowColor);
            }
        }

        // Draw value text
        context.drawStringWithShadow(text, textX, textY, 0xFFFFFF);
    }
}

