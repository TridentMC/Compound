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

import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A primitive for styled text with optional wrapping. Suppliers are sampled during measurement
 * and drawing; bind layout state when changing text can alter its measured size.
 */
public class Text extends PrimitiveElement {

    private Supplier<Component> textSupplier;
    private IntSupplier colorSupplier;
    private BooleanSupplier shadowSupplier;
    private boolean wrap;
    private Component wrappedText;
    private int wrappedWidth = -1;
    private List<Component> wrappedLines = List.of();

    /**
     * Creates styled text. Omitted color and shadow settings use light gray text with a shadow.
     *
     * @param text the non-null styled text
     */
    public Text(Component text) {
        this(() -> text, () -> 0xE0E0E0, () -> true);
    }

    /**
     * Creates styled text. Omitted color and shadow settings use light gray text with a shadow.
     *
     * @param text the non-null styled text
     * @param color the drawing color
     */
    public Text(Component text, int color) {
        this(() -> text, () -> color, () -> true);
    }

    /**
     * Creates styled text. Omitted color and shadow settings use light gray text with a shadow.
     *
     * @param textSupplier the non-null supplier of non-null styled text
     * @param colorSupplier the non-null supplier of drawing colors
     * @param shadowSupplier the non-null supplier controlling text shadows
     */
    public Text(Supplier<Component> textSupplier, IntSupplier colorSupplier, BooleanSupplier shadowSupplier) {
        this.textSupplier = textSupplier;
        this.colorSupplier = colorSupplier;
        this.shadowSupplier = shadowSupplier;
    }

    /**
     * Sets the drawing color.
     *
     * @param color the drawing color
     * @return this element
     */
    public Text setColor(int color) {
        this.colorSupplier = () -> color;
        return this;
    }

    /**
     * Sets whether text is drawn with a shadow.
     *
     * @param shadow whether to draw a text shadow
     * @return this element
     */
    public Text setShadow(boolean shadow) {
        this.shadowSupplier = () -> shadow;
        return this;
    }

    /**
     * Sets whether text wraps to the available width. Request layout when changing this after mounting.
     *
     * @param wrap whether to wrap text to the available width
     * @return this element
     */
    public Text setWrap(boolean wrap) {
        this.wrap = wrap;
        return this;
    }

    private List<Component> wrappedLines(Component text, int width) {
        int available = Math.max(1, width);
        if (!text.equals(this.wrappedText) || available != this.wrappedWidth) {
            this.wrappedText = text.copy();
            this.wrappedWidth = available;
            this.wrappedLines = Minecraft.getInstance().font.getSplitter()
                    .splitLines(text, available, text.getStyle()).stream().map(line -> {
                        var component = Component.empty();
                        line.visit((style, part) -> {
                            component.append(Component.literal(part).setStyle(style));
                            return Optional.empty();
                        }, text.getStyle());
                        return (Component) component;
                    }).toList();
        }
        return this.wrappedLines;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getNarrationMessage() {
        return this.textSupplier.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        var font = Minecraft.getInstance().font;
        var text = this.textSupplier.get();
        int width;
        int height;

        width = font.width(text);
        height = font.lineHeight;
        if (this.wrap) {
            var lines = this.wrappedLines(text, constraints.maxWidth());
            width = lines.stream().mapToInt(font::width).max().orElse(0);
            height = Math.max(1, lines.size()) * font.lineHeight;
        }

        width = Math.min(width, constraints.maxWidth());
        height = Math.min(height, constraints.maxHeight());

        return new Size(width, height);
    }

    @Override
    protected void drawElement(IScreenContext context, @Nonnull Bounds bounds) {
        var font = Minecraft.getInstance().font;
        var text = this.textSupplier.get();
        if (this.wrap) {
            var lines = this.wrappedLines(text, bounds.width());
            int y = bounds.y();
            for (var line : lines) {
                if (y + font.lineHeight > bounds.bottom()) break;
                var colored = line.copy().withStyle(style -> style.withColor(this.colorSupplier.getAsInt()));
                if (this.shadowSupplier.getAsBoolean()) context.drawTextWithShadow(colored, bounds.x(), y);
                else context.drawText(colored, bounds.x(), y);
                y += font.lineHeight;
            }
            return;
        }
        // Draw text vertically centered within bounds
        int color = this.colorSupplier.getAsInt();
        boolean shadow = this.shadowSupplier.getAsBoolean();
        Component coloredText = text.copy().withStyle(style -> style.withColor(color));

        int textY = bounds.y() + (bounds.height() - font.lineHeight) / 2;

        if (shadow) {
            context.drawTextWithShadow(coloredText, bounds.x(), textY);
        } else {
            context.drawText(coloredText, bounds.x(), textY);
        }
    }
}
