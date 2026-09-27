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

import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A scrollable container that clips its content to a viewport with a visible scrollbar.
 * Uses Minecraft's built-in scrollbar sprites ({@code widget/scroller} and
 * {@code widget/scroller_background}) for a native look.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new ScrollArea(), scroll -> {
 *     scroll.layout().fixedSize(200, 300);
 *     scroll.e(new Column(), col -> {
 *         for (int i = 0; i &lt; 100; i++) {
 *             col.e(new Label(Component.literal("Item " + i)));
 *         }
 *     });
 * });
 * </pre>
 */
public class ScrollArea extends BaseElement implements IComposableElement {

    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MIN_HEIGHT = 32;
    private static final int SCROLLBAR_PADDING = 4;

    private static final IScreenSprite SCROLLER_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/scroller"));
    private static final IScreenSprite SCROLLER_BG_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/scroller_background"));

    private final State<Integer> scrollX = new StateImpl<>(0);
    private final State<Integer> scrollY = new StateImpl<>(0);
    private final State<Boolean> scrollbarHovered = new StateImpl<>(false);
    private Direction direction = Direction.VERTICAL;
    private int scrollSpeed = 20;
    private boolean showScrollbar = true;

    private int contentHeight;
    private int viewportHeight;
    private int contentWidth;
    private int viewportWidth;

    public ScrollArea() {
        this(Direction.VERTICAL);
    }

    public ScrollArea(Direction direction) {
        this.direction = direction;
    }

    public ScrollArea direction(Direction direction) {
        this.direction = direction;
        return this;
    }

    public ScrollArea scrollSpeed(int speed) {
        this.scrollSpeed = speed;
        return this;
    }

    public ScrollArea showScrollbar(boolean show) {
        this.showScrollbar = show;
        return this;
    }

    public void scrollTo(int x, int y) {
        this.scrollX.set(x);
        this.scrollY.set(y);
    }

    public State<Integer> getScrollXState() {
        return this.scrollX;
    }

    public State<Integer> getScrollYState() {
        return this.scrollY;
    }

    public int getMaxScrollY() {
        return Math.max(0, this.contentHeight - this.viewportHeight);
    }

    public int getMaxScrollX() {
        return Math.max(0, this.contentWidth - this.viewportWidth);
    }

    @Override
    public void compose(ICompositionScope scope) {
        var tree = scope.getTree();
        if (tree != null) {
            var node = tree.getNodeForElement(this);
            if (node != null) {
                node.getLayoutProperties().clip();
            }
        }

        scope.bindLayout(this.scrollX);
        scope.bindLayout(this.scrollY);
        // scrollbarHovered is only used for cursor feedback; do not recompose on hover.

        // Use a Stack as the content container so it can report the child's full
        // size to the ScrollArea while reserving space for the scrollbar and a small
        // gap between content and the track.
        scope.e(new Stack(), contentStack -> {
            if (this.direction == Direction.VERTICAL) {
                contentStack.layout().maxHeight(Integer.MAX_VALUE);
                if (this.showScrollbar) {
                    contentStack.layout().margin(0, 0, SCROLLBAR_WIDTH + SCROLLBAR_PADDING, 0);
                }
            } else {
                contentStack.layout().maxWidth(Integer.MAX_VALUE);
                if (this.showScrollbar) {
                    contentStack.layout().margin(0, 0, 0, SCROLLBAR_WIDTH + SCROLLBAR_PADDING);
                }
            }
            scope.slotInto(CONTENT_SLOT, contentStack);
        });

        if (this.showScrollbar) {
            scope.e(new ScrollbarElement(), sb -> sb.layout().fillMax());
        }

        scope.onScroll(event -> {
            var bounds = this.getBounds();
            if (bounds == null) return false;

            if (this.direction == Direction.VERTICAL) {
                int maxScroll = this.getMaxScrollY();
                if (maxScroll > 0) {
                    int delta = (int) Math.round(event.scrollDelta() * this.scrollSpeed);
                    int newY = Math.clamp(this.scrollY.get() - delta, 0, maxScroll);
                    this.scrollY.set(newY);
                    return true;
                }
            } else {
                int maxScroll = this.getMaxScrollX();
                if (maxScroll > 0) {
                    int delta = (int) Math.round(event.scrollDelta() * this.scrollSpeed);
                    int newX = Math.clamp(this.scrollX.get() - delta, 0, maxScroll);
                    this.scrollX.set(newX);
                    return true;
                }
            }

            return false;
        });
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {


        // Only the content child (first child) determines scrollable content size.
        if (!measuredChildren.isEmpty()) {
            Size contentSize = measuredChildren.get(0);
            if (this.direction == Direction.VERTICAL) {
                this.contentHeight = contentSize.height();
                this.contentWidth = contentSize.width();
            } else {
                this.contentWidth = contentSize.width();
                this.contentHeight = contentSize.height();
            }
        } else {
            this.contentHeight = 0;
            this.contentWidth = 0;
        }

        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }

        this.viewportWidth = bounds.width();
        this.viewportHeight = bounds.height();
        Size childSize = measuredChildren.get(0);

        int maxScrollX = Math.max(0, childSize.width() - bounds.width());
        int maxScrollY = Math.max(0, childSize.height() - bounds.height());

        int clampedX = Math.clamp(this.scrollX.get(), 0, maxScrollX);
        int clampedY = Math.clamp(this.scrollY.get(), 0, maxScrollY);

        if (this.scrollX.get() != clampedX) this.scrollX.set(clampedX);
        if (this.scrollY.get() != clampedY) this.scrollY.set(clampedY);

        Bounds childBounds = new Bounds(
                new Position(bounds.x() - this.scrollX.get(), bounds.y() - this.scrollY.get()),
                childSize
        );

        if (measuredChildren.size() == 1) {
            return List.of(childBounds);
        }

        Bounds scrollbarBounds;
        if (this.direction == Direction.VERTICAL) {
            int scrollbarX = bounds.x() + bounds.width() - SCROLLBAR_WIDTH;
            scrollbarBounds = new Bounds(
                    new Position(scrollbarX, bounds.y()),
                    new Size(SCROLLBAR_WIDTH, bounds.height())
            );
        } else {
            int scrollbarY = bounds.y() + bounds.height() - SCROLLBAR_WIDTH;
            scrollbarBounds = new Bounds(
                    new Position(bounds.x(), scrollbarY),
                    new Size(bounds.width(), SCROLLBAR_WIDTH)
            );
        }

        return List.of(childBounds, scrollbarBounds);
    }

    private int getScrollerY() {
        int maxScroll = this.getMaxScrollY();
        if (maxScroll <= 0) return 0;
        return (int) ((float) this.scrollY.get() / maxScroll * (this.viewportHeight - getScrollerHeight()));
    }

    private int getScrollerHeight() {
        if (this.contentHeight <= 0) return SCROLLBAR_MIN_HEIGHT;
        int height = (int) ((float) this.viewportHeight / this.contentHeight * this.viewportHeight);
        return Math.clamp(height, Math.min(SCROLLBAR_MIN_HEIGHT, this.viewportHeight), this.viewportHeight);
    }

    private int getScrollerWidth() {
        if (this.contentWidth <= 0) return SCROLLBAR_MIN_HEIGHT;
        int width = (int) ((float) this.viewportWidth / this.contentWidth * this.viewportWidth);
        return Math.clamp(width, Math.min(SCROLLBAR_MIN_HEIGHT, this.viewportWidth), this.viewportWidth);
    }

    private int getScrollerX() {
        int maxScroll = this.getMaxScrollX();
        if (maxScroll <= 0) return 0;
        return (int) ((float) this.scrollX.get() / maxScroll * (this.viewportWidth - getScrollerWidth()));
    }

    public enum Direction {
        VERTICAL,
        HORIZONTAL
    }

    /**
     * Internal element that renders the scrollbar using vanilla Minecraft sprites.
     * Positioned as the second child of ScrollArea, receiving only the scrollbar track bounds.
     */
    private class ScrollbarElement extends BaseElement implements IComposableElement {

        @Override
        public void compose(ICompositionScope scope) {
            scope.onMouseEnter(() -> ScrollArea.this.scrollbarHovered.set(true));
            scope.onMouseExit(() -> ScrollArea.this.scrollbarHovered.set(false));

            scope.onClick(event -> {
                if (event.button() != 0) return false;
                if (ScrollArea.this.direction == Direction.VERTICAL) {
                    return this.setScrollFromY(event.y());
                } else {
                    return this.setScrollFromX(event.x());
                }
            });

            scope.onMouseDrag(event -> {
                if (ScrollArea.this.direction == Direction.VERTICAL) {
                    return this.setScrollFromY(event.y());
                } else {
                    return this.setScrollFromX(event.x());
                }
            });

            scope.e(new Sprite(SCROLLER_BG_SPRITE));
            scope.e(new Sprite(SCROLLER_SPRITE));
        }

        @Override
        public UICursor getCursor(int x, int y) {
            if (ScrollArea.this.scrollbarHovered.get()) {
                return ScrollArea.this.direction == Direction.VERTICAL
                        ? CompoundCursors.VRESIZE
                        : CompoundCursors.HRESIZE;
            }
            return null;
        }

        private boolean setScrollFromY(int y) {
            Bounds bounds = ScrollArea.this.getBounds();
            if (bounds == null) return false;

            int maxScroll = ScrollArea.this.getMaxScrollY();
            if (maxScroll <= 0) return false;

            int trackHeight = bounds.height();
            int scrollerH = ScrollArea.this.getScrollerHeight();
            float ratio = (float) (y - bounds.y() - scrollerH / 2) / (trackHeight - scrollerH);
            int newY = Math.clamp((int) (ratio * maxScroll), 0, maxScroll);
            ScrollArea.this.scrollY.set(newY);
            return true;
        }

        private boolean setScrollFromX(int x) {
            Bounds bounds = ScrollArea.this.getBounds();
            if (bounds == null) return false;

            int maxScroll = ScrollArea.this.getMaxScrollX();
            if (maxScroll <= 0) return false;

            int trackWidth = bounds.width();
            int scrollerW = ScrollArea.this.getScrollerWidth();
            float ratio = (float) (x - bounds.x() - scrollerW / 2) / (trackWidth - scrollerW);
            int newX = Math.clamp((int) (ratio * maxScroll), 0, maxScroll);
            ScrollArea.this.scrollX.set(newX);
            return true;
        }

        @Override
        public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
            return new Size(0, 0);
        }

        @Override
        public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
            var owner = ScrollArea.this;
            var thumb = owner.direction == Direction.VERTICAL
                    ? new Bounds(bounds.x(), bounds.y() + owner.getScrollerY(), bounds.width(), owner.getScrollerHeight())
                    : new Bounds(bounds.x() + owner.getScrollerX(), bounds.y(), owner.getScrollerWidth(), bounds.height());
            return List.of(bounds, thumb);
        }
    }
}
