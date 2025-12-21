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
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.IElementInternal;
import com.tridevmc.compound.ui.element.IPrimitiveElement;
import com.tridevmc.compound.ui.event.*;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.state.State;

import java.util.*;
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
     * and its ancestors. This is rendered after normal rendering so it appears on top.
     * Individual elements have zero awareness of debug mode.
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
        var ancestorChain = new java.util.ArrayList<ITreeNode>();
        ITreeNode current = hoveredNode;
        while (current != null) {
            ancestorChain.add(0, current); // Add at beginning to get root-to-node order
            current = current.getParent();
        }

        // Render debug for each node in the chain (ancestors get outline, hovered gets full detail)
        for (int i = 0; i < ancestorChain.size(); i++) {
            ITreeNode node = ancestorChain.get(i);
            boolean isHoveredNode = (node == hoveredNode);

            var bounds = node.getBounds();
            if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) {
                continue;
            }

            float x = bounds.x();
            float y = bounds.y();
            float w = bounds.width();
            float h = bounds.height();

            if (isHoveredNode) {
                // Full detail for hovered element
                var props = node.getLayoutProperties();
                if (props == null) {
                    props = LayoutProperties.create();
                }

                int marginL = props.getMarginLeft();
                int marginT = props.getMarginTop();
                int marginR = props.getMarginRight();
                int marginB = props.getMarginBottom();

                int paddingL = props.getPaddingLeft();
                int paddingT = props.getPaddingTop();
                int paddingR = props.getPaddingRight();
                int paddingB = props.getPaddingBottom();

                // Draw margin areas (orange)
                if (marginL > 0) {
                    context.drawRect(x - marginL, y, marginL, h, config.getMarginColor());
                    drawArrowLabel(context, x - marginL / 2f, y + h / 2f, marginL, true, config.getMarginColor());
                }
                if (marginR > 0) {
                    context.drawRect(x + w, y, marginR, h, config.getMarginColor());
                    drawArrowLabel(context, x + w + marginR / 2f, y + h / 2f, marginR, true, config.getMarginColor());
                }
                if (marginT > 0) {
                    context.drawRect(x, y - marginT, w, marginT, config.getMarginColor());
                    drawArrowLabel(context, x + w / 2f, y - marginT / 2f, marginT, false, config.getMarginColor());
                }
                if (marginB > 0) {
                    context.drawRect(x, y + h, w, marginB, config.getMarginColor());
                    drawArrowLabel(context, x + w / 2f, y + h + marginB / 2f, marginB, false, config.getMarginColor());
                }

                // Draw padding areas (green)
                if (paddingL > 0) {
                    context.drawRect(x, y, paddingL, h, config.getPaddingColor());
                    drawArrowLabel(context, x + paddingL / 2f, y + h / 2f, paddingL, true, config.getPaddingColor());
                }
                if (paddingR > 0) {
                    context.drawRect(x + w - paddingR, y, paddingR, h, config.getPaddingColor());
                    drawArrowLabel(context, x + w - paddingR / 2f, y + h / 2f, paddingR, true, config.getPaddingColor());
                }
                if (paddingT > 0) {
                    context.drawRect(x + paddingL, y, w - paddingL - paddingR, paddingT, config.getPaddingColor());
                    drawArrowLabel(context, x + w / 2f, y + paddingT / 2f, paddingT, false, config.getPaddingColor());
                }
                if (paddingB > 0) {
                    context.drawRect(x + paddingL, y + h - paddingB, w - paddingL - paddingR, paddingB, config.getPaddingColor());
                    drawArrowLabel(context, x + w / 2f, y + h - paddingB / 2f, paddingB, false, config.getPaddingColor());
                }

                // Draw bounds outline (thicker for hovered)
                context.drawRectOutline(x, y, w, h, config.getBoundsOutlineColor(), 2);

                // Draw dimension label
                if (config.shouldShowDimensions()) {
                    String dimensionText = bounds.width() + " × " + bounds.height();
                    int textWidth = context.getFont().width(dimensionText);
                    int textHeight = 9;
                    int pad = 2;

                    float labelX = x;
                    float labelY = y - textHeight - pad * 2 - 2;
                    if (labelY < 0) {
                        labelY = y + h + 2;
                    }

                    context.drawRect(labelX, labelY, textWidth + pad * 2, textHeight + pad * 2, 0xDD000000);
                    context.drawStringWithShadow(dimensionText, labelX + pad, labelY + pad, 0xFFFFFF);
                }
            } else {
                // Just thin outline for ancestors
                context.drawRectOutline(x, y, w, h, 0x80888888, 1); // Gray outline for parents
            }
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

