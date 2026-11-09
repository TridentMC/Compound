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

import com.mojang.blaze3d.platform.InputConstants;

import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.resources.Identifier;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * A clipped, single-axis viewport for {@link #CONTENT_SLOT}. Content is measured without a
 * bound on the scrolling axis. Displayed scrollbars reserve a gutter outside the content viewport.
 */
public class ScrollArea extends Element implements IComposableElement {

    /** The slot containing this element's consumer-provided content. */
    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private static final int SCROLLBAR_PADDING = 4;

    private static final IScreenSprite SCROLLER_BG_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/scroller_background"));

    private final State<Integer> scrollX = State.of(0);
    private final State<Integer> scrollY = State.of(0);
    private final State<Boolean> scrollbarHovered = State.of(false);
    private Direction direction = Direction.VERTICAL;
    private ScrollbarStyle scrollbarStyle = ScrollbarStyle.LIST;
    private int scrollSpeed = 20;
    private boolean showScrollbar = true;

    private int contentHeight;
    private int viewportHeight;
    private int contentWidth;
    private int viewportWidth;

    /**
     * Creates a scroll viewport; the no-argument form scrolls vertically.
     */
    public ScrollArea() {
        this(Direction.VERTICAL);
    }

    /**
     * Creates a scroll viewport; the no-argument form scrolls vertically.
     *
     * @param direction the non-null scrolling axis
     */
    public ScrollArea(Direction direction) {
        this.direction = Objects.requireNonNull(direction);
    }

    /**
     * Changes the scrolling axis, resets both offsets to zero, and recomposes the viewport.
     *
     * @param direction the non-null scrolling axis
     * @return this element
     */
    public ScrollArea direction(Direction direction) {
        if (this.direction != Objects.requireNonNull(direction)) {
            this.direction = direction;
            this.scrollTo(0, 0);
            this.invalidate();
        }
        return this;
    }

    /**
     * Sets the pixel multiplier for wheel scrolling.
     *
     * @param speed the pixel multiplier per wheel step
     * @return this element
     */
    public ScrollArea scrollSpeed(int speed) {
        this.scrollSpeed = speed;
        return this;
    }

    /**
     * Controls scrollbar visibility and its reserved gutter; wheel scrolling remains available.
     *
     * @param show whether to display it
     * @return this element
     */
    public ScrollArea showScrollbar(boolean show) {
        if (this.showScrollbar != show) {
            this.showScrollbar = show;
            this.invalidate();
        }
        return this;
    }

    /**
     * Changes the scrollbar appearance and gutter geometry.
     *
     * @param style the non-null scrollbar style
     * @return this element
     */
    public ScrollArea scrollbarStyle(ScrollbarStyle style) {
        if (this.scrollbarStyle != Objects.requireNonNull(style)) {
            this.scrollbarStyle = style;
            this.invalidate();
        }
        return this;
    }

    /**
     * Returns the active scrollbar style.
     *
     * @return the active style
     */
    public ScrollbarStyle getScrollbarStyle() {
        return this.scrollbarStyle;
    }

    private void invalidate() {
        this.scrollbarHovered.set(false);
        this.invalidateComposition();
    }

    /**
     * Requests pixel offsets, clamped to the content range during the next layout pass.
     *
     * @param x the requested horizontal pixel offset
     * @param y the requested vertical pixel offset
     */
    public void scrollTo(int x, int y) {
        this.scrollX.set(x);
        this.scrollY.set(y);
    }

    /**
     * Returns the live horizontal offset state; layout clamps its value to the content range.
     *
     * @return the live state; changes notify its observers
     */
    public State<Integer> getScrollXState() {
        return this.scrollX;
    }

    /**
     * Returns the live vertical offset state; layout clamps its value to the content range.
     *
     * @return the live state; changes notify its observers
     */
    public State<Integer> getScrollYState() {
        return this.scrollY;
    }

    /**
     * Returns the vertical overflow measured by the latest layout pass.
     *
     * @return the nonnegative vertical overflow in pixels
     */
    public int getMaxScrollY() {
        return Math.max(0, this.contentHeight - this.viewportHeight);
    }

    /**
     * Returns the horizontal overflow measured by the latest layout pass.
     *
     * @return the nonnegative horizontal overflow in pixels
     */
    public int getMaxScrollX() {
        return Math.max(0, this.contentWidth - this.viewportWidth);
    }

    /**
     * {@inheritDoc}
     */
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
        scope.e(new Stack(), contentStack -> {
            if (this.direction == Direction.VERTICAL) {
                contentStack.layout().unboundedHeight();
                if (this.showScrollbar) {
                    contentStack.layout().margin(0, 0, this.scrollbarStyle.thickness(this.direction) + SCROLLBAR_PADDING, 0);
                }
            } else {
                contentStack.layout().unboundedWidth();
                if (this.showScrollbar) {
                    contentStack.layout().margin(0, 0, 0, this.scrollbarStyle.thickness(this.direction) + SCROLLBAR_PADDING);
                }
            }
            scope.slotInto(CONTENT_SLOT, contentStack);
        });

        if (this.showScrollbar) {
            scope.e(new ScrollbarElement(), sb -> {
                if (this.direction == Direction.VERTICAL) {
                    sb.layout().fixedWidth(this.scrollbarStyle.thickness(this.direction)).fillMaxHeight();
                } else {
                    sb.layout().fixedHeight(this.scrollbarStyle.thickness(this.direction)).fillMaxWidth();
                }
            });
        }

        scope.onScroll(event -> {
            int maximum = this.direction == Direction.VERTICAL ? this.getMaxScrollY() : this.getMaxScrollX();
            if (maximum <= 0) return false;
            var scroll = this.direction == Direction.VERTICAL ? this.scrollY : this.scrollX;
            double wheel = this.direction == Direction.HORIZONTAL && event.scrollX() != 0
                    ? event.scrollX() : event.scrollY();
            int delta = (int) Math.round(wheel * this.scrollSpeed);
            int next = Math.clamp(scroll.get() - delta, 0, maximum);
            if (next == scroll.get()) return false;
            scroll.set(next);
            return true;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        var content = measuredChildren.isEmpty() ? new Size(0, 0) : measuredChildren.getFirst();
        this.contentHeight = content.height();
        this.contentWidth = content.width();
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    /**
     * {@inheritDoc}
     */
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
            int width = Math.min(this.scrollbarStyle.thickness(this.direction), bounds.width());
            int scrollbarX = bounds.x() + bounds.width() - width;
            scrollbarBounds = new Bounds(
                    new Position(scrollbarX, bounds.y()),
                    new Size(width, bounds.height())
            );
        } else {
            int height = Math.min(this.scrollbarStyle.thickness(this.direction), bounds.height());
            int scrollbarY = bounds.y() + bounds.height() - height;
            scrollbarBounds = new Bounds(
                    new Position(bounds.x(), scrollbarY),
                    new Size(bounds.width(), height)
            );
        }

        return List.of(childBounds, scrollbarBounds);
    }

    private int getScrollerY() {
        int maxScroll = this.getMaxScrollY();
        if (maxScroll <= 0) return 0;
        return (int) ((float) this.scrollY.get() / maxScroll * (this.trackLength(this.viewportHeight) - getScrollerHeight()));
    }

    private int getScrollerHeight() {
        return this.scrollbarStyle.thumbLength(this.trackLength(this.viewportHeight), this.contentHeight, Direction.VERTICAL);
    }

    private int getScrollerWidth() {
        return this.scrollbarStyle.thumbLength(this.trackLength(this.viewportWidth), this.contentWidth, Direction.HORIZONTAL);
    }

    private int getScrollerX() {
        int maxScroll = this.getMaxScrollX();
        if (maxScroll <= 0) return 0;
        return (int) ((float) this.scrollX.get() / maxScroll * (this.trackLength(this.viewportWidth) - getScrollerWidth()));
    }

    private int trackLength(int length) {
        return Math.max(0, length - this.scrollbarStyle.inset * 2);
    }

    /** The viewport's single scrolling axis. */
    public enum Direction {
        /** Scrolls content vertically and places the gutter on the right. */
        VERTICAL,
        /** Scrolls content horizontally and places the gutter at the bottom. */
        HORIZONTAL
    }

    /** Vanilla scrollbar appearances with their corresponding track and thumb geometry. */
    public enum ScrollbarStyle {
        /** Narrow list scrollbar with a thumb proportional to the visible content. */
        LIST(6, 32, true, "widget/scroller", "widget/scroller") {
            @Override
            void composeTrack(ICompositionScope scope) {
                scope.e(new Sprite(SCROLLER_BG_SPRITE));
            }
        },
        /** Creative-inventory scrollbar with a fixed-size grippy thumb and a recessed track. */
        GRIPPY(12, 15, false, "container/creative_inventory/scroller",
                "container/creative_inventory/scroller_disabled") {
            @Override
            void composeTrack(ICompositionScope scope) {
                scope.e(new Surface(0xFFFFFFFF), outer -> outer.fillSlot(Surface.CONTENT_SLOT,
                        content -> content.e(new Surface(0xFF373737), dark -> {
                            dark.layout().fillMax().margin(0, 0, 1, 1);
                            dark.fillSlot(Surface.CONTENT_SLOT, interior -> interior.e(
                                    new Surface(0xFF8B8B8B), fill -> fill.layout().fillMax().margin(1, 1, 0, 0)));
                        })));
            }
        };

        private final int width;
        private final int minimumLength;
        private final int inset;
        private final boolean proportional;
        private final IScreenSprite thumb;
        private final IScreenSprite disabledThumb;

        ScrollbarStyle(int width, int minimumLength, boolean proportional, String thumb, String disabledThumb) {
            this.inset = proportional ? 0 : 1;
            this.width = width + this.inset * 2;
            this.minimumLength = minimumLength;
            this.proportional = proportional;
            this.thumb = IScreenSprite.of(Identifier.withDefaultNamespace(thumb));
            this.disabledThumb = IScreenSprite.of(Identifier.withDefaultNamespace(disabledThumb));
        }

        private int thickness(Direction direction) {
            return !this.proportional && direction == Direction.HORIZONTAL
                    ? this.minimumLength + this.inset * 2 : this.width;
        }

        private int thumbLength(int viewport, int content, Direction direction) {
            if (!this.proportional) {
                int length = direction == Direction.VERTICAL ? this.minimumLength : this.width - this.inset * 2;
                return Math.min(length, viewport);
            }
            int maximum = Math.max(1, viewport - Math.min(8, viewport / 2));
            maximum = Math.min(viewport, maximum);
            int minimum = Math.min(this.minimumLength, maximum);
            int length = content <= 0 ? viewport : (int) ((long) viewport * viewport / content);
            return Math.clamp(length, minimum, maximum);
        }

        abstract void composeTrack(ICompositionScope scope);
    }

    private class ScrollbarElement extends Element implements IComposableElement {

        @Override
        public void compose(ICompositionScope scope) {
            scope.onMouseEnter(() -> ScrollArea.this.scrollbarHovered.set(true));
            scope.onMouseExit(() -> ScrollArea.this.scrollbarHovered.set(false));

            scope.onClick(event -> {
                return event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.setScrollFromPointer(event.x(), event.y());
            });

            scope.onMouseDrag(event -> {
                return event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.setScrollFromPointer(event.x(), event.y());
            });

            ScrollArea.this.scrollbarStyle.composeTrack(scope);
            scope.e(new Sprite(() -> {
                var style = ScrollArea.this.scrollbarStyle;
                return this.canScroll() ? style.thumb : style.proportional ? null : style.disabledThumb;
            }));
        }

        private boolean canScroll() {
            return (ScrollArea.this.direction == Direction.VERTICAL
                    ? ScrollArea.this.getMaxScrollY() : ScrollArea.this.getMaxScrollX()) > 0;
        }

        @Override
        public boolean isVisible() {
            return this.canScroll() || !ScrollArea.this.scrollbarStyle.proportional;
        }

        @Override
        public UICursor getCursor(int x, int y) {
            if (this.canScroll() && ScrollArea.this.scrollbarHovered.get()) {
                return ScrollArea.this.direction == Direction.VERTICAL
                        ? CompoundCursors.VRESIZE
                        : CompoundCursors.HRESIZE;
            }
            return null;
        }

        private boolean setScrollFromPointer(int x, int y) {
            var owner = ScrollArea.this;
            var bounds = owner.getBounds();
            boolean vertical = owner.direction == Direction.VERTICAL;
            int maxScroll = vertical ? owner.getMaxScrollY() : owner.getMaxScrollX();
            if (maxScroll <= 0) return false;
            int length = owner.trackLength(vertical ? bounds.height() : bounds.width());
            int thumbLength = vertical ? owner.getScrollerHeight() : owner.getScrollerWidth();
            int travel = length - thumbLength;
            if (travel <= 0) return false;
            int offset = vertical ? y - bounds.y() : x - bounds.x();
            float ratio = (offset - owner.scrollbarStyle.inset - thumbLength / 2F) / travel;
            var scroll = vertical ? owner.scrollY : owner.scrollX;
            scroll.set(Math.clamp((int) (ratio * maxScroll), 0, maxScroll));
            return true;
        }

        @Override
        public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
            return new Size(0, 0);
        }

        @Override
        public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
            var owner = ScrollArea.this;
            int inset = owner.scrollbarStyle.inset;
            var thumb = owner.direction == Direction.VERTICAL
                    ? new Bounds(bounds.x() + inset, bounds.y() + inset + owner.getScrollerY(),
                            Math.max(0, bounds.width() - inset * 2), owner.getScrollerHeight())
                    : new Bounds(bounds.x() + inset + owner.getScrollerX(), bounds.y() + inset,
                            owner.getScrollerWidth(), Math.max(0, bounds.height() - inset * 2));
            return List.of(bounds, thumb);
        }
    }
}
