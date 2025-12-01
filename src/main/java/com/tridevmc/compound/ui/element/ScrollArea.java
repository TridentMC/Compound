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

package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;

import java.util.List;

/**
 * A scrollable container that clips its content to a viewport.
 * Supports vertical, horizontal, and 2D scrolling.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * // Vertical scrolling
 * scope.e(new ScrollArea(), scroll -> {
 *     scroll.layoutProperties().fixedSize(200, 300);
 *     scroll.e(new Column(), col -> {
 *         for (int i = 0; i < 100; i++) {
 *             col.e(new ElementLabel("Item " + i));
 *         }
 *     });
 * });
 *
 * // Horizontal scrolling
 * scope.e(new ScrollArea(ScrollArea.Direction.HORIZONTAL), scroll -> {
 *     scroll.e(new Row(), row -> {
 *         // Wide content...
 *     });
 * });
 * </pre>
 */
public class ScrollArea extends BaseElement implements IComposableElement {

    public static final SlotKey CONTENT_SLOT = new SlotKey("content");
    private final State<Integer> scrollX = new StateImpl<>(0);
    private final State<Integer> scrollY = new StateImpl<>(0);
    private Direction direction = Direction.VERTICAL;
    private int scrollSpeed = 20;

    public ScrollArea() {
        this(Direction.VERTICAL);
    }

    public ScrollArea(Direction direction) {
        this.direction = direction;
        // Clipping is automatically enabled in onAttached() for viewport culling optimization
    }

    public ScrollArea direction(Direction direction) {
        this.direction = direction;
        return this;
    }

    public ScrollArea scrollSpeed(int speed) {
        this.scrollSpeed = speed;
        return this;
    }

    /**
     * Programmatically set scroll position.
     */
    public void scrollTo(int x, int y) {
        this.scrollX.set(x);
        this.scrollY.set(y);
    }

    public State<Integer> getScrollXState() {
        return scrollX;
    }

    public State<Integer> getScrollYState() {
        return scrollY;
    }

    @Override
    public void compose(ICompositionScope scope) {
        // ScrollArea MUST clip to its viewport for proper rendering and viewport culling
        // Enable clipping on our own node's layout properties during composition
        var tree = this.getTree();
        if (tree != null) {
            var node = tree.getNodeForElement(this);
            if (node != null) {
                node.getLayoutProperties().clip();
            }
        }

        // Bind scroll states so changes trigger layout
        scope.bindLayout(scrollX);
        scope.bindLayout(scrollY);

        // Create internal box to hold scrollable content
        // Don't use fillMax - let it size based on content, otherwise it fills Integer.MAX_VALUE
        scope.e(new Box(), box -> {
            scope.slotInto(CONTENT_SLOT, box);
        });

        // Register scroll event handler
        scope.onScroll(event -> {
            var bounds = this.getBounds();
            if (bounds == null) return false;

            var node = scope.getTree().getNodeForElement(this);
            if (node == null || node.getChildren().isEmpty()) return false;

            var childNode = node.getChildren().get(0);
            var childSize = childNode.getMeasuredSize();
            if (childSize == null) return false;

            if (direction == Direction.VERTICAL) {
                int maxScrollY = Math.max(0, childSize.height() - bounds.height());
                if (maxScrollY > 0) {
                    int delta = (int) Math.round(event.scrollDelta() * scrollSpeed);
                    int newY = Math.clamp(scrollY.get() - delta, 0, maxScrollY);
                    scrollY.set(newY);
                    return true;
                }
            } else if (direction == Direction.HORIZONTAL) {
                int maxScrollX = Math.max(0, childSize.width() - bounds.width());
                if (maxScrollX > 0) {
                    int delta = (int) Math.round(event.scrollDelta() * scrollSpeed);
                    int newX = Math.clamp(scrollX.get() - delta, 0, maxScrollX);
                    scrollX.set(newX);
                    return true;
                }
            }

            return false;
        });
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        if (children.isEmpty()) {
            return constraints.constrain(new Size(0, 0));
        }

        if (children.size() > 1) {
            System.err.println("WARNING: ScrollArea should have exactly one child, but has " +
                    children.size() + ". Only the first child will be scrolled.");
        }

        var child = children.get(0);

        // Allow child to be larger than viewport based on direction
        var childConstraints = switch (direction) {
            case VERTICAL -> Constraints.loose(
                    constraints.maxWidth(),
                    Integer.MAX_VALUE  // Unlimited height
            );
            case HORIZONTAL -> Constraints.loose(
                    Integer.MAX_VALUE,  // Unlimited width
                    constraints.maxHeight()
            );
        };

        LayoutHelper.measureChild(child, childConstraints);

        // ScrollArea fills available space (viewport size)
        return constraints.constrain(new Size(
                constraints.maxWidth(),
                constraints.maxHeight()
        ));
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);

        if (children.isEmpty()) return;

        var child = children.get(0);
        var childSize = LayoutHelper.getMeasuredSize(child);
        if (childSize == null) return;

        // Clamp scroll to valid range (only update if changed to avoid layout loop)
        int maxScrollX = Math.max(0, childSize.width() - bounds.width());
        int maxScrollY = Math.max(0, childSize.height() - bounds.height());

        int clampedX = Math.clamp(scrollX.get(), 0, maxScrollX);
        int clampedY = Math.clamp(scrollY.get(), 0, maxScrollY);

        if (scrollX.get() != clampedX) {
            scrollX.set(clampedX);
        }
        if (scrollY.get() != clampedY) {
            scrollY.set(clampedY);
        }

        // Place child with scroll offset
        var childBounds = new Bounds(
                new Position(
                        bounds.x() - scrollX.get(),
                        bounds.y() - scrollY.get()
                ),
                childSize
        );

        LayoutHelper.placeChild(child, childBounds);
    }

    public enum Direction {
        /**
         * Scroll vertically only
         */
        VERTICAL,
        /**
         * Scroll horizontally only
         */
        HORIZONTAL
    }
}
