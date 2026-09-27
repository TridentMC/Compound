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

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.tridevmc.compound.ui.animation.AnimationScheduler;
import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.debug.DebugOverlayConfig;
import com.tridevmc.compound.ui.debug.LayoutDebugRenderer;
import com.tridevmc.compound.ui.element.*;
import com.tridevmc.compound.ui.event.*;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.state.State;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.Comparator;
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
    public static final boolean DEBUG_EVENTS = Boolean.getBoolean("compound.ui.debugEvents");

    private final Map<IElement, ITreeNode> elementToNode = new HashMap<>();
    private final AnimationScheduler animationScheduler = new AnimationScheduler();
    private final LayoutDebugRenderer debugRenderer = new LayoutDebugRenderer();
    private ITreeNode root;
    private Size rootSize;
    private Constraints rootConstraints;
    private final Set<ITreeNode> pendingRecompositions = new LinkedHashSet<>();
    private boolean layoutDirty = true;
    private ITreeNode capturedNode;
    private int capturedButton = -1;
    private final List<InputScope> inputScopes = new ArrayList<>();

    private record InputScope(ITreeNode node, ITreeNode previousFocus) {
    }

    private Size lastWindowSize = new Size(-1, -1);
    private int lastMouseX, lastMouseY;
    private static final CursorType HAND_CURSOR = CursorType.createStandardCursor(GLFW.GLFW_HAND_CURSOR, "hand", CursorType.DEFAULT);
    private static final CursorType IBEAM_CURSOR = CursorType.createStandardCursor(GLFW.GLFW_IBEAM_CURSOR, "ibeam", CursorType.DEFAULT);
    private static final CursorType CROSSHAIR_CURSOR = CursorType.createStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR, "crosshair", CursorType.DEFAULT);
    private static final CursorType HRESIZE_CURSOR = CursorType.createStandardCursor(GLFW.GLFW_HRESIZE_CURSOR, "hresize", CursorType.DEFAULT);
    private static final CursorType VRESIZE_CURSOR = CursorType.createStandardCursor(GLFW.GLFW_VRESIZE_CURSOR, "vresize", CursorType.DEFAULT);

    private CursorType requestedCursor = CursorType.DEFAULT;
    private Set<ITreeNode> lastHoveredPath = new HashSet<>();
    private boolean pendingHoverUpdate = false;
    private ITreeNode focusedNode = null; // Global focus tracking
    private final List<ITreeNode> overlayNodes = new ArrayList<>();

    public AnimationScheduler getAnimationScheduler() {
        return this.animationScheduler;
    }

    public ITreeNode getRoot() {
        return this.root;
    }

    public void setRoot(ITreeNode root) {
        if (this.root != null && this.root != root) this.reset();
        this.layoutDirty = true;
        this.root = root;
        if (root != null) {
            this.registerNode(root);
        }
    }

    /** Disposes the current composition and prepares this tree for reuse. */
    public void reset() {
        this.clearFocus();
        this.inputScopes.clear();
        this.capturedNode = null;
        this.capturedButton = -1;
        if (this.root != null) this.unregisterNode(this.root);
        this.root = null;
        this.elementToNode.clear();
        this.pendingRecompositions.clear();
        this.animationScheduler.dispose();
        this.lastHoveredPath.clear();
        this.overlayNodes.clear();
        this.rootSize = null;
        this.rootConstraints = null;
        this.lastWindowSize = new Size(-1, -1);
        this.layoutDirty = true;
        this.pendingHoverUpdate = false;
        this.requestedCursor = CursorType.DEFAULT;
    }

    /** Sets the viewport before composition so responsive overlays can use its dimensions. */
    public void setViewportSize(int width, int height) {
        this.lastWindowSize = new Size(width, height);
        this.rootConstraints = Constraints.loose(width, height);
        this.layoutDirty = true;
    }

    /** Returns the current viewport size for positioning overlays. */
    public Size getViewportSize() {
        return this.lastWindowSize;
    }

    /** Restricts input to a popup or modal until its input scope is cleared. */
    public void setInputRoot(ITreeNode node) {
        if (node == null || this.inputRoot() == node) return;
        this.inputScopes.add(new InputScope(node, this.focusedNode));
        if (this.focusedNode != node && (this.focusedNode == null || !node.isAncestorOf(this.focusedNode))) {
            this.clearFocus();
        }
        this.capturedNode = null;
        this.pendingHoverUpdate = true;
    }

    /** Restores the input scope and focus that preceded the supplied popup. */
    public void clearInputRoot(ITreeNode node) {
        for (int i = this.inputScopes.size() - 1; i >= 0; i--) {
            var entry = this.inputScopes.get(i);
            if (entry.node() == node) {
                var wasActive = i == this.inputScopes.size() - 1;
                this.inputScopes.remove(i);
                if (wasActive) {
                    var previous = entry.previousFocus();
                    this.requestFocus(this.isAttached(previous) ? previous : null);
                }
                this.pendingHoverUpdate = true;
                return;
            }
        }
    }

    private ITreeNode inputRoot() {
        return this.inputScopes.isEmpty() ? this.root : this.inputScopes.getLast().node();
    }

    private boolean isAttached(ITreeNode node) {
        return node != null && this.elementToNode.get(node.getElement()) == node;
    }

    private boolean acceptsInput(ITreeNode node) {
        var inputRoot = this.inputRoot();
        return node != null && (node == inputRoot || (inputRoot != null && inputRoot.isAncestorOf(node)));
    }

    public boolean hasRoot() {
        return this.root != null;
    }

    // Focus management
    public ITreeNode getFocusedNode() {
        return this.focusedNode;
    }

    public void requestFocus(ITreeNode node) {
        if (node != null && (!this.isAttached(node) || !this.acceptsInput(node))) return;
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
        if (DEBUG_EVENTS) {
            System.out.println("[UITree] focus=" + (node == null ? "none" : node.getElement().getClass().getSimpleName()));
        }

        // Notify the new focused element that it gained focus (via handlers)
        if (node != null) {
            for (var handler : node.getFocusGainedHandlers()) {
                handler.run();
            }
            var minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getNarrator() != null) {
                minecraft.getNarrator().saySystemNow(this.narrationMessage(node));
            }
        }
    }

    private Component narrationMessage(ITreeNode node) {
        if (!node.getElement().isVisible()) return Component.empty();
        var message = node.getElement().getNarrationMessage();
        if (!message.getString().isBlank()) return message;
        var combined = Component.empty();
        for (var child : node.getChildren()) {
            var childMessage = this.narrationMessage(child);
            if (!childMessage.getString().isBlank()) {
                if (!combined.getString().isEmpty()) combined.append(", ");
                combined.append(childMessage);
            }
        }
        return combined;
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
        this.clearInputRoot(node);
        this.elementToNode.remove(node.getElement(), node);
        this.pendingRecompositions.remove(node);
        this.lastHoveredPath.remove(node);
        if (this.capturedNode == node) {
            this.capturedNode = null;
            this.capturedButton = -1;
        }
        if (this.focusedNode == node) this.clearFocus();
        for (var child : node.getChildren()) this.unregisterNode(child);
        node.dispose();
        node.getElement().onDetached();
        if (node.getElement() instanceof IElementInternal internal && internal.getNode() == node) {
            internal.setNode(null);
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
    }

    public void recomposeNode(ITreeNode node) {
        if (!this.isAttached(node) || !node.hasCompositionFunction()) {
            return;
        }

        List<ITreeNode> children = new ArrayList<>(node.getChildren());
        for (ITreeNode child : children) {
            this.detachNode(child);
        }

        node.clearHandlers();

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
        var inputRoot = this.inputRoot();
        if (inputRoot == null) return null;

        // Overlays (layer > 0) are rendered on top and must be hit-tested first.
        List<ITreeNode> overlays = new ArrayList<>();
        this.collectOverlayNodes(inputRoot, overlays);
        if (!overlays.isEmpty()) {
            overlays.sort((a, b) -> Integer.compare(a.getLayoutProperties().getLayer(), b.getLayoutProperties().getLayer()));
            for (int i = overlays.size() - 1; i >= 0; i--) {
                ITreeNode overlay = overlays.get(i);
                ITreeNode result = this.findNodeAtWithViewport(overlay, x, y, null);
                if (result != null) {
                    return result;
                }
            }
        }

        return this.findNodeAtWithViewport(inputRoot, x, y, null);
    }

    private void collectOverlayNodes(ITreeNode node, List<ITreeNode> result) {
        if (!node.getElement().isVisible()) return;
        for (ITreeNode child : node.getChildren()) {
            if (child.getLayoutProperties().getLayer() > 0) {
                result.add(child);
            } else {
                this.collectOverlayNodes(child, result);
            }
        }
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
        if (!node.getElement().isVisible()) return null;
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
        this.rootConstraints = rootConstraints;
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
        List<Size> measuredChildren = this.measureChildren(node, contentConstraints);

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

    private List<Size> measureChildren(ITreeNode node, Constraints constraints) {
        var children = node.getChildren();
        var measured = new ArrayList<Size>(children.size());
        var horizontal = node.getElement() instanceof Row;
        var weightedLayout = horizontal ? constraints.hasBoundedWidth()
                : node.getElement() instanceof Column && constraints.hasBoundedHeight();
        var available = horizontal ? constraints.maxWidth() : constraints.maxHeight();
        long occupied = LayoutMath.calculateTotalSpacing(children.size(), node.getLayoutProperties().getSpacing());
        double remainingWeight = 0;
        for (var child : children) {
            var weight = child.getLayoutProperties().getWeight();
            if (weightedLayout && weight != null && weight > 0) {
                remainingWeight += weight;
                measured.add(new Size(0, 0));
            } else {
                var size = this.measureNode(child, constraints);
                occupied += horizontal ? size.width() : size.height();
                measured.add(size);
            }
        }
        var remaining = (int) Math.max(0L, available - occupied);
        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            var weight = child.getLayoutProperties().getWeight();
            if (weightedLayout && weight != null && weight > 0) {
                var allocation = (int) Math.round(remaining * weight / remainingWeight);
                remaining -= allocation;
                remainingWeight -= weight;
                var childConstraints = horizontal ? constraints.withFixedWidth(allocation)
                        : constraints.withFixedHeight(allocation);
                measured.set(i, this.measureNode(child, childConstraints));
            }
        }
        return measured;
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
        if (this.isAttached(changedNode)) this.layoutDirty = true;
    }

    private void flushRecompositions() {
        var pending = new ArrayList<>(this.pendingRecompositions);
        this.pendingRecompositions.clear();
        pending.sort(Comparator.comparingInt(ITreeNode::getDepth));
        for (var node : pending) {
            if (this.isAttached(node)) this.recomposeNode(node);
        }
    }

    public boolean dispatchClick(int x, int y, MouseClickEvent event) {
        ITreeNode node = this.findNodeAt(x, y);
        if (DEBUG_EVENTS) {
            System.out.println("[UITree] dispatchClick at (" + x + "," + y + ") node=" +
                    (node != null ? node.getElement().getClass().getSimpleName() : "null") +
                    " handlers=" + (node != null ? node.getClickHandlers().size() : 0));
        }

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            var focusTarget = node;
            while (focusTarget != null && this.acceptsInput(focusTarget)
                    && !focusTarget.getElement().isFocusable()) {
                focusTarget = focusTarget.getParent();
            }
            this.requestFocus(this.acceptsInput(focusTarget) ? focusTarget : null);
        }

        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && this.acceptsInput(current) && !consumed) {
                for (var handler : current.getClickHandlers()) {
                    if (handler.apply(event)) {
                        if (DEBUG_EVENTS) {
                            System.out.println("[UITree] click consumed by " +
                                    current.getElement().getClass().getSimpleName());
                        }
                        this.capturedNode = current;
                        this.capturedButton = event.button();
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed || !this.inputScopes.isEmpty();
        }
        return !this.inputScopes.isEmpty();
    }

    public CursorType getRequestedCursor() {
        return this.requestedCursor;
    }

    private static CursorType toPlatformCursor(UICursor cursor) {
        if (cursor == null) return CursorType.DEFAULT;
        return switch (cursor) {
            case HAND -> HAND_CURSOR;
            case IBEAM -> IBEAM_CURSOR;
            case CROSSHAIR -> CROSSHAIR_CURSOR;
            case HRESIZE -> HRESIZE_CURSOR;
            case VRESIZE -> VRESIZE_CURSOR;
            default -> CursorType.DEFAULT;
        };
    }

    private void updateHoverState() {
        int x = this.lastMouseX;
        int y = this.lastMouseY;
        ITreeNode node = this.findNodeAt(x, y);

        UICursor cursor = null;
        if (node != null) {
            ITreeNode current = node;
            while (current != null && this.acceptsInput(current)) {
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
        this.requestedCursor = toPlatformCursor(cursor);

        // Build the current hovered path (node and all ancestors)
        Set<ITreeNode> currentHoveredPath = new HashSet<>();
        if (node != null) {
            ITreeNode current = node;
            while (current != null && this.acceptsInput(current)) {
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
            while (current != null && this.acceptsInput(current) && !consumed) {
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
        if (DEBUG_EVENTS) {
            System.out.println("[UITree] dispatchScroll at (" + x + "," + y + ") node=" +
                    (node != null ? node.getElement().getClass().getSimpleName() : "null"));
        }
        if (node != null) {
            ITreeNode current = node;
            while (current != null && this.acceptsInput(current)) {
                for (var handler : current.getScrollHandlers()) {
                    boolean handled = handler.apply(event);
                    if (handled) {
                        if (DEBUG_EVENTS) {
                            System.out.println("[UITree] scroll consumed by " +
                                    current.getElement().getClass().getSimpleName());
                        }
                        return true;
                    }
                }
                current = current.getParent();
            }
        }
        return !this.inputScopes.isEmpty();
    }

    public boolean dispatchKeyPress(KeyInputEvent event) {
        if (DEBUG_EVENTS) {
            System.out.println("[UITree] key=" + event.keyCode() + " focus="
                    + (this.focusedNode == null ? "none" : this.focusedNode.getElement().getClass().getSimpleName()));
        }
        if (event.keyCode() == GLFW.GLFW_KEY_TAB && !event.ctrlDown() && !event.altDown()) {
            return this.moveFocus(event.shiftDown());
        }
        var current = this.focusedNode != null ? this.focusedNode : this.inputRoot();
        while (current != null && this.acceptsInput(current)) {
            for (var handler : current.getKeyPressHandlers()) {
                if (handler.apply(event)) return true;
            }
            current = current.getParent();
        }
        return !this.inputScopes.isEmpty();
    }

    private boolean moveFocus(boolean backwards) {
        var candidates = new ArrayList<ITreeNode>();
        this.walkDepthFirst(this.inputRoot(), node -> {
            var bounds = node.getBounds();
            if (!node.getElement().isVisible() || !node.getElement().isFocusable() || bounds == null || bounds.width() <= 0 || bounds.height() <= 0) return;
            for (var parent = node.getParent(); parent != null; parent = parent.getParent()) {
                if (!parent.getElement().isVisible()) return;
                if (parent.getLayoutProperties().isClip() && parent.getBounds() != null
                        && !parent.getBounds().intersects(bounds)) return;
                if (parent == this.inputRoot() || parent.getLayoutProperties().getLayer() > 0) break;
            }
            candidates.add(node);
        });
        if (candidates.isEmpty()) return !this.inputScopes.isEmpty();
        var index = candidates.indexOf(this.focusedNode);
        var next = index < 0 ? (backwards ? candidates.size() - 1 : 0)
                : Math.floorMod(index + (backwards ? -1 : 1), candidates.size());
        this.requestFocus(candidates.get(next));
        return true;
    }

    public boolean dispatchKeyRelease(KeyInputEvent event) {
        var current = this.focusedNode;
        while (current != null && this.acceptsInput(current)) {
            for (var handler : current.getKeyReleaseHandlers()) {
                if (handler.apply(event)) return true;
            }
            current = current.getParent();
        }
        return !this.inputScopes.isEmpty();
    }

    public boolean dispatchCharTyped(CharEvent event) {
        if (this.focusedNode != null && this.acceptsInput(this.focusedNode)) {
            for (var handler : this.focusedNode.getCharTypedHandlers()) {
                if (handler.apply(event)) return true;
            }
        }
        return !this.inputScopes.isEmpty();
    }

    public boolean dispatchMouseRelease(int x, int y, MouseReleaseEvent event) {
        ITreeNode node = this.capturedNode != null && this.capturedButton == event.button()
                ? this.capturedNode : this.findNodeAt(x, y);
        if (this.capturedButton == event.button()) {
            this.capturedNode = null;
            this.capturedButton = -1;
        }
        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && this.acceptsInput(current) && !consumed) {
                for (var handler : current.getMouseReleaseHandlers()) {
                    if (handler.apply(event)) {
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed || !this.inputScopes.isEmpty();
        }
        return !this.inputScopes.isEmpty();
    }

    public boolean dispatchMouseDrag(int x, int y, MouseDragEvent event) {
        ITreeNode node = this.capturedNode != null && this.capturedButton == event.button()
                ? this.capturedNode : this.findNodeAt(x, y);
        if (node != null) {
            ITreeNode current = node;
            boolean consumed = false;
            while (current != null && this.acceptsInput(current) && !consumed) {
                for (var handler : current.getMouseDragHandlers()) {
                    if (handler.apply(event)) {
                        consumed = true;
                        break;
                    }
                }
                current = current.getParent();
            }
            return consumed || !this.inputScopes.isEmpty();
        }
        return !this.inputScopes.isEmpty();
    }

    /**
     * Request recomposition of a specific node.
     * Called directly by TreeNode composition observers.
     *
     * @param node the node to recompose
     */
    public void requestRecompose(ITreeNode node) {
        if (this.isAttached(node)) this.pendingRecompositions.add(node);
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

        this.flushRecompositions();

        var sizeChanged = this.lastWindowSize.width() != width || this.lastWindowSize.height() != height;

        if (sizeChanged || this.rootConstraints == null) {
            this.lastWindowSize = new Size(width, height);
            this.rootConstraints = Constraints.loose(width, height);
            this.layoutDirty = true;
        }
        if (this.layoutDirty) {
            this.layoutDirty = false;
            this.measureTree(this.rootConstraints);
            this.placeTree(Position.ORIGIN, this.rootConstraints);
            this.pendingHoverUpdate = true;
        }
        if (this.pendingHoverUpdate) {
            this.pendingHoverUpdate = false;
            this.updateHoverState();
        }

        this.renderTree(context);
    }

    public void renderTree(IScreenContext context) {
        if (this.root == null) {
            return;
        }

        this.overlayNodes.clear();
        this.renderNode(this.root, context, null, true);

        if (!this.overlayNodes.isEmpty()) {
            // Sort by layer ascending; collection order within a layer is pre-order traversal.
            this.overlayNodes.sort(Comparator.comparingInt(node -> node.getLayoutProperties().getLayer()));
            for (var overlay : this.overlayNodes) {
                this.renderNode(overlay, context, null, false);
            }
        }

        // Debug overlay rendered AFTER normal rendering (on top)
        // Individual elements have zero awareness of debug mode
        if (DebugOverlayConfig.get().isEnabled()) {
            this.debugRenderer.render(context, this.findNodeAt(this.lastMouseX, this.lastMouseY));
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
    private void renderNode(ITreeNode node, IScreenContext context, Bounds activeViewport, boolean collectOverlays) {
        if (!node.getElement().isVisible()) return;
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
            if (collectOverlays && child.getLayoutProperties().getLayer() > 0) {
                this.overlayNodes.add(child);
                continue;
            }
            this.renderNode(child, context, viewport, collectOverlays);
        }

        if (didEnableScissor) {
            context.disableScissor();
        }
    }

}
