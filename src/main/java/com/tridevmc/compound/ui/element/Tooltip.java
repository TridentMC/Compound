/*
 * Copyright 2018 - 2026 TridentMC
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

import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.slot.SlotKey;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wraps {@link #CONTENT_SLOT} with a delayed, vanilla-rendered hover tooltip.
 * The default delay is 500 milliseconds and the default wrapping width is 200 GUI pixels.
 * The tooltip is hidden when another input surface obscures its content.
 */
public class Tooltip extends Element implements IComposableElement {
    /**
     * The content whose bounds determine tooltip hover.
     */
    public static final SlotKey CONTENT_SLOT = new SlotKey("content");
    private final Supplier<Component> textSupplier;
    private int maxWidth = 200;
    private int delay = 500;
    private long hoverStart = -1;
    private boolean visible;
    private Consumer<Void> onShow;
    private Consumer<Void> onHide;

    /**
     * Creates a tooltip with fixed text.
     *
     * @param text the text, or null to render no tooltip.
     */
    public Tooltip(Component text) {
        this(() -> text);
    }

    /**
     * Creates a tooltip whose text is read while it is visible.
     *
     * @param textSupplier the supplier; a null or empty result renders no tooltip.
     */
    public Tooltip(Supplier<Component> textSupplier) {
        this.textSupplier = textSupplier;
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Box(), content -> scope.slotInto(CONTENT_SLOT, content));
        scope.e(new NativeTooltip(), tooltip -> tooltip.layout().fixedSize(0, 0).layer(300));
    }

    /** {@inheritDoc} */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
        return children.isEmpty() ? new Size(0, 0) : children.get(0);
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> children) {
        return List.of(bounds, new Bounds(bounds.x(), bounds.y(), 0, 0));
    }

    /** {@inheritDoc} */
    @Override
    public void onDetached() {
        this.hoverStart = -1;
        this.visible = false;
    }

    /**
     * Sets the tooltip text wrapping width.
     *
     * @param width the width in GUI pixels, clamped to at least one.
     */
    public void setMaxWidth(int width) { this.maxWidth = Math.max(1, width); }
    /**
     * Sets how long the content must be hovered before showing the tooltip.
     *
     * @param milliseconds the delay, clamped to at least zero.
     */
    public void setDelay(int milliseconds) { this.delay = Math.max(0, milliseconds); }
    /**
     * Replaces the callback for a hover-driven transition to visible.
     *
     * @param callback the callback, invoked with null, or null to remove it.
     */
    public void setOnShow(Consumer<Void> callback) { this.onShow = callback; }
    /**
     * Replaces the callback for a hover-driven transition to hidden.
     *
     * @param callback the callback, invoked with null, or null to remove it.
     */
    public void setOnHide(Consumer<Void> callback) { this.onHide = callback; }

    private class NativeTooltip extends PrimitiveElement {
        @Override
        public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
            return new Size(0, 0);
        }

        @Override
        protected void drawElement(IScreenContext context, Bounds bounds) {
            var owner = Tooltip.this;
            var anchor = owner.getBounds();
            boolean hovered = anchor.contains((int) context.getMouseX(), (int) context.getMouseY());
            if (hovered) {
                var target = owner.getNode().getTree().findNodeAt((int) context.getMouseX(), (int) context.getMouseY());
                hovered = target == owner.getNode() || (target != null && owner.getNode().isAncestorOf(target));
            }
            long now = System.currentTimeMillis();
            if (!hovered) owner.hoverStart = -1;
            else if (owner.hoverStart < 0) owner.hoverStart = now;
            boolean show = hovered && now - owner.hoverStart >= owner.delay;
            if (show != owner.visible) {
                owner.visible = show;
                var callback = show ? owner.onShow : owner.onHide;
                if (callback != null) callback.accept(null);
            }
            if (show) {
                var text = owner.textSupplier.get();
                if (text != null && !text.getString().isEmpty()) {
                    context.drawProcessorAsTooltip(context.getFont().split(text, owner.maxWidth),
                            (int) context.getMouseX(), (int) context.getMouseY(), context.getFont());
                }
            }
        }
    }
}
