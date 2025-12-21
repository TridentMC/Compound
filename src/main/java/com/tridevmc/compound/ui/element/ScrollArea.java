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

import javax.annotation.Nonnull;
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
 *             col.e(new Label("Item " + i));
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
        var tree = scope.getTree();
        if (tree != null) {
            var node = tree.getNodeForElement(this);
            if (node != null) {
                node.getLayoutProperties().clip();
            }
        }

        // Bind scroll states so changes trigger layout
        scope.bindLayout(scrollX);
        scope.bindLayout(scrollY);

        // Set unbounded max size in scrolling dimension to allow content to expand beyond viewport
        scope.e(new Box(), box -> {
            if (direction == Direction.VERTICAL) {
                box.layout().maxHeight(Integer.MAX_VALUE);
            } else if (direction == Direction.HORIZONTAL) {
                box.layout().maxWidth(Integer.MAX_VALUE);
            }
            scope.slotInto(CONTENT_SLOT, box);
        });

        scope.onScroll(event -> {
            var bounds = this.getBounds();

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
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        // ScrollArea fills available space (viewport size)
        // Child size determines scroll range, not viewport size
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        Size childSize = measuredChildren.get(0);

        int maxScrollX = Math.max(0, childSize.width() - bounds.width());
        int maxScrollY = Math.max(0, childSize.height() - bounds.height());

        int clampedX = Math.clamp(scrollX.get(), 0, maxScrollX);
        int clampedY = Math.clamp(scrollY.get(), 0, maxScrollY);

        if (scrollX.get() != clampedX) scrollX.set(clampedX);
        if (scrollY.get() != clampedY) scrollY.set(clampedY);

        Bounds childBounds = new Bounds(
                new Position(bounds.x() - scrollX.get(), bounds.y() - scrollY.get()),
                childSize
        );

        return List.of(childBounds);
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
