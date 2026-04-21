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
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A tooltip component that appears on hover to provide additional information.
 * Must be used within a composable element's compose() method.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new Button(), button -> {
 *     button.fillSlot(Button.CONTENT_SLOT, content -> {
 *         content.e(new Label(Component.literal("Click me")));
 *     });
 *     // Tooltip is rendered as a separate overlay element
 *     button.e(new Tooltip(() -> Component.literal("Click to save your changes")));
 * });
 * </pre>
 */
public class Tooltip extends BaseElement implements IComposableElement {

    private static final int DEFAULT_MAX_WIDTH = 200;
    private static final int PADDING = 4;
    private static final int BACKGROUND_COLOR = 0xF0100010;
    private static final int BORDER_COLOR_TOP = 0x505000FF;
    private static final int BORDER_COLOR_BOTTOM = 0x5028007F;

    private final Supplier<Component> textSupplier;
    private final State<Boolean> visible = new StateImpl<>(false);
    private int maxWidth = DEFAULT_MAX_WIDTH;
    private int delay = 500;
    private long hoverStartTime = -1;
    private Consumer<Void> onShow;
    private Consumer<Void> onHide;

    public Tooltip(Component text) {
        this(() -> text);
    }

    public Tooltip(Supplier<Component> textSupplier) {
        this.textSupplier = textSupplier;
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.visible);

        scope.onMouseEnter(() -> {
            this.hoverStartTime = System.currentTimeMillis();
        });

        scope.onMouseExit(() -> {
            this.hoverStartTime = -1;
            if (this.visible.get()) {
                this.visible.set(false);
                if (this.onHide != null) {
                    this.onHide.accept(null);
                }
            }
        });

        scope.onMouseMove(event -> {
            if (this.hoverStartTime > 0 && !this.visible.get()) {
                long elapsed = System.currentTimeMillis() - this.hoverStartTime;
                if (elapsed >= this.delay) {
                    this.visible.set(true);
                    if (this.onShow != null) {
                        this.onShow.accept(null);
                    }
                }
            }
            return false;
        });

        if (this.visible.get()) {
            scope.e(new Stack(), stack -> {
                stack.layout().fillMax();

                Component text = this.textSupplier.get();
                if (text == null || text.getString().isEmpty()) return;

                var font = net.minecraft.client.Minecraft.getInstance().font;
                int textWidth = Math.min(font.width(text), this.maxWidth);
                List<String> lines = font.getSplitter().splitLines(text.getString(), this.maxWidth, text.getStyle())
                        .stream()
                        .map(formattedText -> formattedText.getString())
                        .toList();

                int lineHeight = font.lineHeight;
                int totalHeight = lines.size() * lineHeight;
                int tooltipWidth = textWidth + PADDING * 2;
                int tooltipHeight = totalHeight + PADDING * 2;

                Bounds bounds = this.getBounds();
                if (bounds == null) return;

                final int tooltipX = bounds.x();
                final int tooltipY = bounds.y() - tooltipHeight - 4 < 0
                        ? bounds.y() + bounds.height() + 4
                        : bounds.y() - tooltipHeight - 4;

                stack.e(new Stack(), tooltipStack -> {
                    tooltipStack.layout()
                            .fixedSize(tooltipWidth, tooltipHeight)
                            .margin(tooltipX - bounds.x(), tooltipY - bounds.y(), 0, 0);

                    tooltipStack.e(new Rect(BACKGROUND_COLOR), bg -> bg.layout().fillMax());

                    tooltipStack.e(new Rect(BORDER_COLOR_TOP), top -> top.layout().fixedHeight(1).fillMaxWidth());
                    tooltipStack.e(new Rect(BORDER_COLOR_BOTTOM), bottom -> bottom.layout()
                            .fixedHeight(1)
                            .margin(0, tooltipHeight - 1, 0, 0));

                    for (int i = 0; i < lines.size(); i++) {
                        final int lineIndex = i;
                        tooltipStack.e(new Label(
                                Component.literal(lines.get(lineIndex)),
                                0xFFFFFFFF,
                                false
                        ), label -> label.layout()
                                .margin(PADDING, PADDING + lineIndex * lineHeight, 0, 0));
                    }
                });
            });
        }
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        return new Size(0, 0);
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        return List.of();
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setDelay(int delayMs) {
        this.delay = delayMs;
    }

    public void setOnShow(Consumer<Void> onShow) {
        this.onShow = onShow;
    }

    public void setOnHide(Consumer<Void> onHide) {
        this.onHide = onHide;
    }

    public boolean isVisible() {
        return this.visible.get();
    }
}
