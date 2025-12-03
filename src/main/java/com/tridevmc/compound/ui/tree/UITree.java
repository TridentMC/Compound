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
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.IPrimitiveElement;
import com.tridevmc.compound.ui.event.*;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutMath;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.layout.Size;
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

    public ITreeNode getNodeForElement(IElement element) {
        return this.elementToNode.get(element);
    }

    public boolean hasNode(IElement element) {
        return this.elementToNode.containsKey(element);
    }

    public ITreeNode createNode(IElement element) {
        TreeNode node = new TreeNode(element, this);
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

        // Check if point is within the active viewport (if any)
        if (viewport != null && !viewport.contains(x, y)) {
            return null;  // Point is clipped, no children will be visible either
        }

        // Check children first (depth-first, deepest nodes have priority)
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

    /**
     * Measures a node and returns its size WITH margin included.
     *
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

        // Store for placement phase
        node.setMeasuredSize(sizeWithMargin);

        return sizeWithMargin;
    }

    /**
     * Applies layout properties to constraints to determine the element's target size.
     * This is called BEFORE measuring children so they receive the correct constraints.
     *
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

        // Apply min/max constraints
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

        // Ensure min <= max
        maxWidth = Math.max(minWidth, maxWidth);
        maxHeight = Math.max(minHeight, maxHeight);

        return new Constraints(minWidth, maxWidth, minHeight, maxHeight);
    }

    /**
     * Places a node and all its children.
     *
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

        // Step 1: Set element's bounds
        element.setBounds(bounds);

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

            // Offset by child's margin
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

        // Cursor handling
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

        // Fire exit handlers for exited nodes
        if (!exitedNodes.isEmpty()) {
            for (ITreeNode exitedNode : exitedNodes) {
                for (var handler : exitedNode.getMouseExitHandlers()) {
                    handler.run();
                }
            }
        }

        // Fire enter handlers for entered nodes
        if (!enteredNodes.isEmpty()) {
            for (ITreeNode enteredNode : enteredNodes) {
                for (var handler : enteredNode.getMouseEnterHandlers()) {
                    handler.run();
                }
            }
        }

        // Update the last hovered path
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
        // TODO: Focus system - add focus tracking to dispatch keyboard events only to the focused element.
        //       Current implementation broadcasts to all elements, which is inefficient for large UIs.
        //       Using array to allow modification in lambda
        final boolean[] consumed = {false};
        this.walkDepthFirst(this.root, node -> {
            if (!consumed[0]) {
                for (var handler : node.getKeyPressHandlers()) {
                    if (handler.apply(event)) {
                        consumed[0] = true;
                        break;
                    }
                }
            }
        });
        return consumed[0];
    }

    public boolean dispatchKeyRelease(KeyInputEvent event) {
        // TODO: Focus system - see dispatchKeyPress
        final boolean[] consumed = {false};
        this.walkDepthFirst(this.root, node -> {
            if (!consumed[0]) {
                for (var handler : node.getKeyReleaseHandlers()) {
                    if (handler.apply(event)) {
                        consumed[0] = true;
                        break;
                    }
                }
            }
        });
        return consumed[0];
    }

    public boolean dispatchCharTyped(CharEvent event) {
        // TODO: Focus system - see dispatchKeyPress
        final boolean[] consumed = {false};
        this.walkDepthFirst(this.root, node -> {
            if (!consumed[0]) {
                for (var handler : node.getCharTypedHandlers()) {
                    if (handler.apply(event)) {
                        consumed[0] = true;
                        break;
                    }
                }
            }
        });
        return consumed[0];
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

        // Render tree recursively with clipping support
        this.renderNode(this.root, context, null);
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

        // Check if this element clips its children via layout properties
        boolean shouldClip = node.getLayoutProperties().isClip();
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
