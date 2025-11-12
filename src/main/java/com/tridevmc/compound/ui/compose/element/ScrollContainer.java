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

package com.tridevmc.compound.ui.compose.element;

import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.Position;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.compose.scope.IComposableElementScope;
import com.tridevmc.compound.ui.compose.scope.ICompositionScope;
import com.tridevmc.compound.ui.compose.slot.SlotKey;
import com.tridevmc.compound.ui.compose.state.State;
import com.tridevmc.compound.ui.compose.state.StateImpl;
import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * A scrollable container that clips content using scissor and offsets children based on scroll position.
 *
 * Usage:
 * <pre>
 * scope.e(new ScrollContainer(), container -> {
 *     container.fillSlot(ScrollContainer.CONTENT, content -> {
 *         // Add scrollable content here
 *         content.e(new Column(), column -> {
 *             // ... many items that will scroll
 *         });
 *     });
 *     container.onScroll(event -> {
 *         // Optional: custom scroll handling
 *     });
 * });
 * </pre>
 */
public class ScrollContainer extends BaseElement implements IComposableElement {

    public static final SlotKey CONTENT = new SlotKey("content");

    private final State<Double> scrollOffset;
    private final double scrollSpeed;
    private double maxScroll;
    private Size contentSize;

    public ScrollContainer() {
        this(1.0);
    }

    public ScrollContainer(double scrollSpeed) {
        this.scrollOffset = new StateImpl<>(0.0);
        this.scrollSpeed = scrollSpeed;
        this.maxScroll = 0.0;
        this.contentSize = Size.ZERO;
    }

    @Override
    public void compose(ICompositionScope scope) {
        IComposableElementScope<?> elementScope = (IComposableElementScope<?>) scope;

        // Bind to scroll offset state
        scope.bind(this.scrollOffset);

        // Register scroll handler
        scope.onScroll(event -> {
            double newOffset = this.scrollOffset.get() + (event.scrollDelta() * this.scrollSpeed * 10.0);

            // Clamp scroll offset
            newOffset = Math.max(0.0, Math.min(this.maxScroll, newOffset));

            if (newOffset != this.scrollOffset.get()) {
                this.scrollOffset.set(newOffset);
                event.consume();
            }
        });

        // Create a stack to layer the scissor operations and content
        scope.e(new Stack(), stack -> {
            // Enable scissor clipping
            stack.e(new ElementScissorEnable());

            // Offset container for scrolled content
            stack.e(new ElementScrollOffset(this.scrollOffset), offsetContainer -> {
                // Render content slot
                elementScope.slot(CONTENT, null);
            });

            // Disable scissor clipping
            stack.e(new ElementScissorDisable());
        });
    }

    @Override
    public Size measure(Constraints constraints) {
        var tree = this.getTree();
        if (tree == null) {
            return Size.ZERO;
        }
        var node = tree.getNodeForElement(this);
        if (node == null) {
            return Size.ZERO;
        }
        var children = node.getChildren();
        if (children.isEmpty()) {
            return Size.ZERO;
        }

        // The scissor wrapper is the first child
        var scissorNode = children.get(0);
        if (scissorNode.getChildren().isEmpty()) {
            return Size.ZERO;
        }

        // Measure content with unconstrained height to get true size
        var contentNode = scissorNode.getChildren().get(0);
        this.contentSize = contentNode.getElement().measure(Constraints.loose(constraints.maxWidth(), Integer.MAX_VALUE));

        // Calculate max scroll based on content vs container height
        int containerHeight = constraints.maxHeight();
        this.maxScroll = Math.max(0.0, this.contentSize.height() - containerHeight);

        // Container takes up the constrained size
        return new Size(this.contentSize.width(), Math.min(this.contentSize.height(), containerHeight));
    }

    @Override
    public void place(Bounds bounds) {
        this.setBounds(bounds);

        var tree = this.getTree();
        if (tree == null) {
            return;
        }
        var node = tree.getNodeForElement(this);
        if (node == null) {
            return;
        }
        var children = node.getChildren();
        if (children.isEmpty()) {
            return;
        }

        // Place the scissor wrapper
        var scissorNode = children.get(0);
        scissorNode.getElement().place(bounds);
    }

    public State<Double> getScrollOffsetState() {
        return this.scrollOffset;
    }

    public double getScrollOffset() {
        return this.scrollOffset.get();
    }

    public void setScrollOffset(double offset) {
        this.scrollOffset.set(Math.max(0.0, Math.min(this.maxScroll, offset)));
    }

    public double getMaxScroll() {
        return this.maxScroll;
    }

    /**
     * Primitive element that enables scissor clipping based on parent bounds.
     */
    private static class ElementScissorEnable extends BaseElement implements IPrimitiveElement {
        @Override
        public Size measure(Constraints constraints) {
            return Size.ZERO;
        }

        @Override
        public void place(Bounds bounds) {
            this.setBounds(bounds);
        }

        @Override
        public void draw(IScreenContext context) {
            var bounds = this.getParent().getBounds();
            if (bounds != null) {
                context.enableScissor(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height());
            }
        }

        private IElement getParent() {
            var tree = this.getTree();
            if (tree == null) {
                return null;
            }
            var node = tree.getNodeForElement(this);
            if (node == null || node.getParent() == null) {
                return null;
            }
            return node.getParent().getElement();
        }
    }

    /**
     * Primitive element that disables scissor clipping.
     */
    private static class ElementScissorDisable extends BaseElement implements IPrimitiveElement {
        @Override
        public Size measure(Constraints constraints) {
            return Size.ZERO;
        }

        @Override
        public void place(Bounds bounds) {
            this.setBounds(bounds);
        }

        @Override
        public void draw(IScreenContext context) {
            context.disableScissor();
        }
    }

    /**
     * Container that offsets its children based on scroll position.
     */
    private static class ElementScrollOffset extends BaseElement implements IContainer {
        private final State<Double> scrollOffset;

        public ElementScrollOffset(State<Double> scrollOffset) {
            this.scrollOffset = scrollOffset;
        }

        @Override
        public Size measure(Constraints constraints) {
            var tree = this.getTree();
            if (tree == null) {
                return Size.ZERO;
            }
            var node = tree.getNodeForElement(this);
            if (node == null) {
                return Size.ZERO;
            }
            var children = node.getChildren();
            if (children.isEmpty()) {
                return Size.ZERO;
            }

            // Measure children with loose height constraint to get full content size
            Size maxSize = Size.ZERO;
            for (var child : children) {
                Size childSize = child.getElement().measure(Constraints.loose(constraints.maxWidth(), Integer.MAX_VALUE));
                maxSize = new Size(Math.max(maxSize.width(), childSize.width()), Math.max(maxSize.height(), childSize.height()));
            }
            return maxSize;
        }

        @Override
        public void place(Bounds bounds) {
            this.setBounds(bounds);

            var tree = this.getTree();
            if (tree == null) {
                return;
            }
            var node = tree.getNodeForElement(this);
            if (node == null) {
                return;
            }
            var children = node.getChildren();

            // Place children with scroll offset applied
            int offsetY = (int) -this.scrollOffset.get();
            Bounds offsetBounds = new Bounds(
                    new Position(bounds.x(), bounds.y() + offsetY),
                    bounds.size()
            );

            for (var child : children) {
                child.getElement().place(offsetBounds);
            }
        }
    }
}
