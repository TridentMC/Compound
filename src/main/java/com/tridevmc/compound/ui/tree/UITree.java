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
import com.tridevmc.compound.ui.animation.api.IAnimationTimeline;
import com.tridevmc.compound.ui.animation.internal.AnimationTimeline;
import com.tridevmc.compound.ui.debug.DebugOverlayConfig;
import com.tridevmc.compound.ui.debug.LayoutDebugRenderer;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.element.IElementInternal;
import com.tridevmc.compound.ui.element.IPrimitiveElement;
import com.tridevmc.compound.ui.element.internal.IWindowedElement;
import com.tridevmc.compound.ui.event.CharEvent;
import com.tridevmc.compound.ui.event.KeyInputEvent;
import com.tridevmc.compound.ui.event.MouseClickEvent;
import com.tridevmc.compound.ui.event.MouseDragEvent;
import com.tridevmc.compound.ui.event.MouseMoveEvent;
import com.tridevmc.compound.ui.event.MouseReleaseEvent;
import com.tridevmc.compound.ui.event.MouseScrollEvent;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.client.Minecraft;

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
 * Owns node lifecycle, layout, rendering, and input routing.
 * Advanced compositions may access viewport, focus, overlays, and hit testing through their scope.
 * Use tree lifecycle methods and queued invalidation to keep nodes and subscriptions consistent.
 */
public class UITree {
    public static final boolean DEBUG_EVENTS = Boolean.getBoolean("compound.ui.debugEvents");

    private final Map<IElement, ITreeNode> elementToNode = new HashMap<>();
    private final TreeLayout layout = new TreeLayout();
    private final AnimationScheduler animationScheduler = new AnimationScheduler();
    private final Map<AnimationTimeline, ITreeNode> timelines = new HashMap<>();
    private long frameNanos;
    private boolean preparingFrame;
    private final Map<ITreeNode, Set<AnimationTimeline>> composingTimelines = new HashMap<>();
    private long lifecycleGeneration;
    private boolean resetting;
    private final Queue<ITreeNode> rootsMountedDuringReset = new LinkedList<>();

    void beginTimelineComposition(ITreeNode node) {
        this.composingTimelines.put(node, new HashSet<>());
    }

    void endTimelineComposition(ITreeNode node) {
        var used = this.composingTimelines.remove(node);
        this.timelines.entrySet().removeIf(entry -> {
            if (entry.getValue() != node || used.contains(entry.getKey())) return false;
            entry.getKey().detach(node);
            return true;
        });
    }

    public void useAnimationTimeline(ITreeNode node, IAnimationTimeline timeline) {
        if (!this.isRegistered(node)) throw new IllegalStateException("Timeline needs an attached node");
        if (!(timeline instanceof AnimationTimeline clock)) {
            throw new IllegalArgumentException("Use IAnimationTimeline.create()");
        }
        clock.attach(node);
        this.timelines.put(clock, node);
        var used = this.composingTimelines.get(node);
        if (used != null) used.add(clock);
        if (this.preparingFrame) clock.advance(this.frameNanos);
    }

    private void detachTimelines(ITreeNode node) {
        this.timelines.entrySet().removeIf(entry -> {
            if (entry.getValue() != node) return false;
            entry.getKey().detach(node);
            return true;
        });
    }
    private final LayoutDebugRenderer debugRenderer = new LayoutDebugRenderer();
    private ITreeNode root;
    private Size rootSize;
    private Constraints rootConstraints;
    private final Set<ITreeNode> pendingRecompositions = new LinkedHashSet<>();
    private boolean layoutDirty = true;
    private final Set<ITreeNode> windows = new LinkedHashSet<>();
    private final Set<ITreeNode> pendingWindowSources = new LinkedHashSet<>();
    private final Set<ITreeNode> dirtyWindows = new LinkedHashSet<>();
    private final TreeInput input = new TreeInput(this);

    public void requestWindowSource(ITreeNode node) {
        if (this.isAttached(node)) this.pendingWindowSources.add(node);
    }

    void releaseWindowPin(ITreeNode node) {
        for (var parent = node; parent != null; parent = parent.getParent()) {
            if (parent.getElement() instanceof IWindowedElement) {
                this.dirtyWindows.add(parent);
                return;
            }
        }
    }

    private void flushWindowSources() {
        var pending = List.copyOf(this.pendingWindowSources);
        this.pendingWindowSources.clear();
        for (var node : pending) {
            if (this.isAttached(node)) {
                var window = (IWindowedElement) node.getElement();
                if (window.reconcileSource()) this.layoutDirty = true;
                this.dirtyWindows.add(node);
            }
        }
    }

    private Size lastWindowSize = new Size(-1, -1);

    private boolean pendingHoverUpdate = false;
    private final List<ITreeNode> overlayNodes = new ArrayList<>();

    public AnimationScheduler getAnimationScheduler() {
        return this.animationScheduler;
    }

    public ITreeNode getRoot() {
        return this.root;
    }

    public void setRoot(ITreeNode root) {
        if (this.resetting) {
            if (root != null && root != this.root && !this.rootsMountedDuringReset.contains(root)) {
                this.registerNode(root);
                this.rootsMountedDuringReset.add(root);
            }
            return;
        }
        if (this.root != null && this.root != root) this.reset();
        if (this.root != root) this.lifecycleGeneration++;
        this.layoutDirty = true;
        this.root = root;
        if (root != null) {
            this.registerNode(root);
        }
    }

    /**
     * Releases the tree and its resources. Compositions attempted from teardown callbacks are
     * also disposed; compose a replacement after this method returns.
     */
    public void reset() {
        if (this.resetting) return;
        this.resetting = true;
        this.lifecycleGeneration++;
        var previousRoot = this.root;
        try {
            this.input.reset();
            if (previousRoot != null) this.unregisterNode(previousRoot);
            while (!this.rootsMountedDuringReset.isEmpty()) {
                var discarded = this.rootsMountedDuringReset.remove();
                if (discarded.getTree() == this) this.unregisterNode(discarded);
            }
            this.root = null;
            this.elementToNode.clear();
            this.pendingRecompositions.clear();
            this.composingTimelines.clear();
            this.windows.clear();
            this.pendingWindowSources.clear();
            this.dirtyWindows.clear();
            this.animationScheduler.dispose();
            this.overlayNodes.clear();
            this.rootSize = null;
            this.rootConstraints = null;
            this.lastWindowSize = new Size(-1, -1);
            this.layoutDirty = true;
            this.pendingHoverUpdate = false;
        } finally {
            this.resetting = false;
        }
    }

    public void setViewportSize(int width, int height) {
        this.lastWindowSize = new Size(width, height);
        this.rootConstraints = Constraints.loose(width, height);
        this.layoutDirty = true;
    }

    public Size getViewportSize() {
        return this.lastWindowSize;
    }

    boolean isAttached(ITreeNode node) {
        return !this.resetting && this.isRegistered(node);
    }

    private boolean isRegistered(ITreeNode node) {
        return node != null && this.elementToNode.get(node.getElement()) == node;
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
        if (element instanceof IElementInternal internal) {
            internal.setNode(node);
        }
        this.registerNode(node);
        return node;
    }

    private void registerNode(ITreeNode node) {
        this.elementToNode.put(node.getElement(), node);
        if (node.getElement() instanceof IWindowedElement) {
            this.windows.add(node);
            this.dirtyWindows.add(node);
        }
        for (ITreeNode child : node.getChildren()) {
            this.registerNode(child);
        }
    }

    private void unregisterNode(ITreeNode node) {
        this.detachTimelines(node);
        this.input.detach(node);
        this.windows.remove(node);
        this.dirtyWindows.remove(node);
        this.pendingWindowSources.remove(node);
        this.elementToNode.remove(node.getElement(), node);
        this.pendingRecompositions.remove(node);
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
        this.requestRemeasurement(parent);
    }

    public void detachNode(ITreeNode node) {
        ITreeNode parent = node.getParent();
        if (parent != null) {
            parent.removeChild(node);
        }

        this.unregisterNode(node);
        this.requestRemeasurement(parent);
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
            node.runComposition();
            this.requestRemeasurement(node);
        }
    }

    public void walkDepthFirst(ITreeNode start, Consumer<ITreeNode> visitor) {
        this.walkDepthFirstWithDepth(start, 0, (node, depth) -> visitor.accept(node));
    }

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
        var inputRoot = this.input.inputRoot();
        if (inputRoot == null) return null;

        // Overlays (layer > 0) are rendered on top and must be hit-tested first.
        List<ITreeNode> overlays = new ArrayList<>();
        TreeLayers.collect(this.root, overlays);
        var inputGroup = TreeLayers.group(inputRoot);
        if (!overlays.isEmpty()) {
            for (int i = overlays.size() - 1; i >= 0; i--) {
                ITreeNode overlay = overlays.get(i);
                var candidate = inputGroup.filter(group -> group == overlay).isPresent()
                        ? inputRoot : inputRoot.isAncestorOf(overlay) ? overlay : null;
                if (candidate == null) continue;
                ITreeNode result = this.findNodeAtWithViewport(candidate, x, y, null);
                if (result != null) {
                    return result;
                }
            }
        }

        return inputGroup.isPresent() ? null
                : this.findNodeAtWithViewport(inputRoot, x, y, null);
    }

    private Bounds screenClip(Bounds clip) {
        if (this.lastWindowSize.width() < 0 || this.lastWindowSize.height() < 0) return clip;
        var screen = new Bounds(0, 0, this.lastWindowSize.width(), this.lastWindowSize.height());
        return clip == null ? screen : clip.intersection(screen);
    }

    private ITreeNode findNodeAtWithViewport(ITreeNode node, int x, int y, Bounds activeViewport) {
        if (!node.getElement().isVisible()) return null;
        IElement element = node.getElement();
        Bounds elementBounds = element.getBounds();

        if (elementBounds == null) {
            return null;
        }

        var geometry = node.getFrameGeometry();
        Bounds viewport = this.screenClip(geometry == null ? activeViewport : geometry.clip());
        if (viewport != null && !viewport.contains(x, y)) return null;

        // Iterate in REVERSE order to hit-test top-most elements first
        List<ITreeNode> children = node.getChildren();
        ITreeNode result = null;
        for (int i = children.size() - 1; i >= 0; i--) {
            ITreeNode child = children.get(i);
            if (TreeLayers.isOverlay(child)) continue;
            result = this.findNodeAtWithViewport(child, x, y, viewport);
            if (result != null) {
                break;  // Found a child node at this position
            }
        }

        if (result == null && element.isHitTestable()
                && (geometry == null ? elementBounds.contains(x, y) : geometry.contains(x, y))) {
            return node;
        }

        return result;
    }

    public void measureTree(Constraints rootConstraints) {
        this.rootConstraints = rootConstraints;
        if (this.root != null) {
            this.rootSize = this.layout.measureNode(this.root, rootConstraints);
        }
    }

    public void placeTree(Position rootPosition) {
        if (this.root != null && this.rootSize != null) {
            var props = this.root.getLayoutProperties();
            var allocated = new Bounds(rootPosition, this.rootSize);
            this.root.setAllocatedBounds(allocated);
            var rootBounds = new Bounds(rootPosition.x() + props.getMarginLeft(),
                    rootPosition.y() + props.getMarginTop(),
                    Constraints.subtractInset(this.rootSize.width(),
                            (long) props.getMarginLeft() + props.getMarginRight()),
                    Constraints.subtractInset(this.rootSize.height(),
                            (long) props.getMarginTop() + props.getMarginBottom()));
            this.layout.placeNode(this.root, rootBounds);
        }
    }

    public void placeTree(Position rootPosition, Constraints rootConstraints) {
        this.placeTree(rootPosition);
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

    public void requestRecompose(ITreeNode node) {
        if (this.isAttached(node)) this.pendingRecompositions.add(node);
    }

    public void requestRemeasure(ITreeNode node) {
        this.requestRemeasurement(node);
    }

    @Deprecated
    public void bindNodeToState(ITreeNode node, State<?> state) {
        node.bindCompositionState(state);
    }

    @Deprecated
    public void unbindNodeFromState(ITreeNode node, State<?> state) {
        node.unbindCompositionState(state);
        node.unbindLayoutState(state);
    }

    public void layoutAndRender(int width, int height, IScreenContext context) {
        this.prepareFrame(width, height, context);
        this.renderTree(context);
    }

    public void prepareFrame(int width, int height, IScreenContext context) {
        if (this.root == null) {
            return;
        }

        // Callbacks may close or replace the tree; a replacement starts on its next frame.
        var generation = this.lifecycleGeneration;
        this.frameNanos = context.frameNanos();
        this.preparingFrame = true;
        try {
            for (var timeline : this.timelines.keySet()) timeline.advance(this.frameNanos);
            this.animationScheduler.updateAnimations(context.getTicks());
            if (!this.isCurrentFrame(generation)) return;
            this.flushRecompositions();
        } finally {
            this.preparingFrame = false;
        }
        if (!this.isCurrentFrame(generation)) return;

        var sizeChanged = this.lastWindowSize.width() != width || this.lastWindowSize.height() != height;

        if (sizeChanged || this.rootConstraints == null) {
            this.lastWindowSize = new Size(width, height);
            this.rootConstraints = Constraints.loose(width, height);
            this.layoutDirty = true;
        }
        this.flushWindowSources();
        if (!this.isCurrentFrame(generation)) return;
        for (int pass = 0; this.layoutDirty || !this.dirtyWindows.isEmpty(); pass++) {
            if (pass == 8) throw new IllegalStateException("Lazy viewport layout did not settle in eight passes");
            if (this.layoutDirty) {
                this.layoutDirty = false;
                this.measureTree(this.rootConstraints);
                if (!this.isCurrentFrame(generation)) return;
                this.placeTree(Position.ORIGIN, this.rootConstraints);
                if (!this.isCurrentFrame(generation)) return;
                this.dirtyWindows.addAll(this.windows);
                this.pendingHoverUpdate = true;
            }
            var pending = List.copyOf(this.dirtyWindows);
            this.dirtyWindows.clear();
            for (var node : pending) {
                if (this.isAttached(node)) {
                    this.layoutDirty |= TreeWindows.reconcile(node, new Bounds(0, 0, width, height));
                    if (!this.isCurrentFrame(generation)) return;
                }
            }
            this.flushWindowSources();
            if (!this.isCurrentFrame(generation)) return;
        }
        TreeGeometry.prepare(this.root);
        if (!this.isCurrentFrame(generation)) return;
        TreeGeometry.sample(this.root, null, null);
        if (!this.isCurrentFrame(generation)) return;
        this.pendingHoverUpdate = false;
        this.input.updateHoverState();
    }

    private boolean isCurrentFrame(long generation) {
        return this.root != null && this.lifecycleGeneration == generation;
    }

    public void renderTree(IScreenContext context) {
        if (this.root == null || this.root.getBounds() == null) {
            return;
        }

        TreeLayers.collect(this.root, this.overlayNodes);
        if (!TreeLayers.isOverlay(this.root)) this.renderNode(this.root, context, null);

        if (!this.overlayNodes.isEmpty()) {
            for (var overlay : this.overlayNodes) {
                this.renderNode(overlay, context, null);
            }
        }

        if (DebugOverlayConfig.get().isEnabled()) {
            this.debugRenderer.render(context, this.input.hoveredNode());
        }
    }

    private void renderNode(ITreeNode node, IScreenContext context, Bounds activeViewport) {
        if (!node.getElement().isVisible()) return;
        IElement element = node.getElement();

        var geometry = node.getFrameGeometry();
        Bounds viewport = this.screenClip(geometry == null ? activeViewport : geometry.clip());
        if (geometry == null && node.getLayoutProperties().isClip()) {
            viewport = viewport == null ? node.getBounds() : viewport.intersection(node.getBounds());
        }
        boolean intersects = viewport == null || (viewport.width() > 0 && viewport.height() > 0
                && (geometry == null ? viewport.intersects(node.getBounds())
                : geometry.left() < viewport.right() && geometry.right() > viewport.left()
                && geometry.top() < viewport.bottom() && geometry.bottom() > viewport.top()));
        if (intersects && element instanceof IPrimitiveElement primitive) {
            var pose = geometry == null ? null : context.getActiveStack();
            if (viewport != null) context.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
            if (pose != null) pose.pushMatrix();
            try {
                if (pose != null && geometry != null) pose.mul(geometry.matrix());
                primitive.draw(context);
            } finally {
                if (pose != null) pose.popMatrix();
                if (viewport != null) context.disableScissor();
            }
        }
        for (ITreeNode child : node.getChildren()) {
            if (TreeLayers.isOverlay(child)) continue;
            this.renderNode(child, context, viewport);
        }
    }

    /** Last pointer position delivered through this tree's input routing. */
    public Position getMousePosition() {
        return this.input.mousePosition();
    }

    void requestHoverUpdate() {
        this.pendingHoverUpdate = true;
    }

    public void setInputRoot(ITreeNode node) {
        this.input.setInputRoot(node);
    }

    public void clearInputRoot(ITreeNode node) {
        this.input.clearInputRoot(node);
    }

    public ITreeNode getFocusedNode() {
        return this.input.getFocusedNode();
    }

    public ITreeNode getCapturedNode() {
        return this.input.getCapturedNode();
    }

    public void requestFocus(ITreeNode node) {
        this.input.requestFocus(node);
    }

    public void clearFocus() {
        this.input.clearFocus();
    }

    public boolean hasFocus(ITreeNode node) {
        return this.input.hasFocus(node);
    }

    public CursorType getRequestedCursor() {
        return this.input.getRequestedCursor();
    }

    public boolean dispatchClick(int x, int y, MouseClickEvent event) {
        return this.input.dispatchClick(x, y, event);
    }

    public void dispatchMouseMove(int x, int y, MouseMoveEvent event) {
        this.input.dispatchMouseMove(x, y, event);
    }

    public boolean dispatchScroll(int x, int y, MouseScrollEvent event) {
        return this.input.dispatchScroll(x, y, event);
    }

    public boolean dispatchKeyPress(KeyInputEvent event) {
        return this.input.dispatchKeyPress(event);
    }

    public boolean dispatchKeyRelease(KeyInputEvent event) {
        return this.input.dispatchKeyRelease(event);
    }

    public boolean dispatchCharTyped(CharEvent event) {
        return this.input.dispatchCharTyped(event);
    }

    public boolean dispatchMouseRelease(int x, int y, MouseReleaseEvent event) {
        return this.input.dispatchMouseRelease(x, y, event);
    }

    public boolean dispatchMouseDrag(int x, int y, MouseDragEvent event) {
        return this.input.dispatchMouseDrag(x, y, event);
    }

}
