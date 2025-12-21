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
import java.util.function.Supplier;

/**
 * A primitive element for rendering text with support for highlighting.
 */
public class Text extends BasePrimitiveElement {

    private Supplier<Component> textSupplier;
    private Supplier<Integer> colorSupplier;
    private Supplier<Boolean> shadowSupplier;

    // Highlighting state
    private Supplier<Integer> highlightStartSupplier;
    private Supplier<Integer> highlightEndSupplier;
    private Supplier<Integer> highlightColorSupplier;

    public Text(Component text) {
        this(() -> text, () -> 0xE0E0E0, () -> true);
    }

    public Text(Component text, int color) {
        this(() -> text, () -> color, () -> true);
    }

    public Text(Supplier<Component> textSupplier, Supplier<Integer> colorSupplier, Supplier<Boolean> shadowSupplier) {
        this.textSupplier = textSupplier;
        this.colorSupplier = colorSupplier;
        this.shadowSupplier = shadowSupplier;
        this.highlightStartSupplier = () -> -1;
        this.highlightEndSupplier = () -> -1;
        this.highlightColorSupplier = () -> 0x800000FF;
    }

    // Builder-style configuration methods
    public Text setColor(int color) {
        this.colorSupplier = () -> color;
        return this;
    }

    public Text setShadow(boolean shadow) {
        this.shadowSupplier = () -> shadow;
        return this;
    }

    /**
     * Sets the highlight range and color.
     *
     * @param start inclusive start index
     * @param end exclusive end index
     * @param color ARGB color of the highlight
     * @return this element
     */
    public Text setHighlight(int start, int end, int color) {
        this.highlightStartSupplier = () -> start;
        this.highlightEndSupplier = () -> end;
        this.highlightColorSupplier = () -> color;
        return this;
    }

    public Text setHighlight(Supplier<Integer> start, Supplier<Integer> end, Supplier<Integer> color) {
        this.highlightStartSupplier = start;
        this.highlightEndSupplier = end;
        this.highlightColorSupplier = color;
        return this;
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        var font = Minecraft.getInstance().font;
        var text = this.textSupplier.get();
        boolean shadow = this.shadowSupplier.get();
        int width;
        int height;

        width = font.width(text);
        height = font.lineHeight;

        width = Math.min(width, constraints.maxWidth());
        height = Math.min(height, constraints.maxHeight());

        return new Size(width, height);
    }

    @Override
    protected void drawElement(IScreenContext context, @Nonnull Bounds bounds) {
        var font = Minecraft.getInstance().font;
        var text = this.textSupplier.get();
        String stringText = text.getString();

        int start = this.highlightStartSupplier.get();
        int end = this.highlightEndSupplier.get();
        int highlightColor = this.highlightColorSupplier.get();

        // Draw highlight if valid range
        if (start != end && start >= 0 && end >= 0) {
            int min = Math.min(start, end);
            int max = Math.min(stringText.length(), Math.max(start, end));
            
            if (min < max) {
                String beforeHighlight = stringText.substring(0, min);
                String highlightedText = stringText.substring(min, max);
                
                int xOffset = font.width(beforeHighlight);
                int highlightWidth = font.width(highlightedText);
                
                context.drawRect(
                    bounds.x() + xOffset,
                    bounds.y(), 
                    highlightWidth,
                    font.lineHeight,
                    highlightColor
                );
            }
        }

        // Draw text at bounds position
        int color = this.colorSupplier.get();
        boolean shadow = this.shadowSupplier.get();
        Component coloredText = text.copy().withStyle(style -> style.withColor(color));

        if (shadow) {
            context.drawTextWithShadow(coloredText, bounds.x(), bounds.y());
        } else {
            context.drawText(coloredText, bounds.x(), bounds.y());
        }
    }
}
