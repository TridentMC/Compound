/*
 * Copyright 2018 - 2022 TridentMC
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

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tridevmc.compound.ui.ICompoundUI;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.layout.ILayout;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;


/**
 * A simple text label element to add to UIs, contains auto resize and text wrapping functionality.
 */
public class ElementLabel extends Element {

    private final Font fontRenderer;

    private Component text;
    private boolean drawShadow, wrapText, autoSize;
    private int maxWidth, maxHeight;

    private List<FormattedCharSequence> lines = Lists.newArrayList();
    private int longestLineWidth;

    public ElementLabel(Rect2F dimensions, ILayout layout, Font fontRenderer, boolean drawShadow, boolean wrapText, boolean autoSize, int maxWidth, int maxHeight) {
        super(dimensions, layout);
        this.fontRenderer = fontRenderer;
        this.drawShadow = drawShadow;
        this.wrapText = wrapText;
        this.autoSize = autoSize;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public ElementLabel(Rect2F dimensions, ILayout layout, Font fontRenderer, boolean drawShadow, boolean wrapText, boolean autoSize) {
        this(dimensions, layout, fontRenderer, drawShadow, wrapText, autoSize, -1, -1);
    }

    public ElementLabel(Rect2F dimensions, ILayout layout, Font fontRenderer) {
        this(dimensions, layout, fontRenderer, true, true, true, -1, -1);
    }

    public ElementLabel(Rect2F dimensions, ILayout layout) {
        this(dimensions, layout, Minecraft.getInstance().font);
    }

    @Override
    public void drawForeground(ICompoundUI ui) {
        IScreenContext screen = ui.getScreenContext();
        Rect2F dimensions = this.getTransformedDimensions(screen);

        double nextYLevel = dimensions.getY();
        for (var line : this.lines) {
            if (nextYLevel + this.fontRenderer.lineHeight > dimensions.getY() + dimensions.getHeight()) {
                nextYLevel = Math.min(dimensions.getY(), dimensions.getHeight() - this.fontRenderer.lineHeight);
            }

            if (this.drawShadow) {
                screen.drawFormattedCharSequenceWithShadow(line, dimensions.getX(), (float) nextYLevel);
            } else {
                screen.drawFormattedCharSequence(line, dimensions.getX(), (float) nextYLevel);
            }

            nextYLevel += this.fontRenderer.lineHeight;
        }
    }

    private void resize() {
        if (this.autoSize) {
            int newWidth = Math.min(this.getMaxWidth(), this.longestLineWidth);
            int newHeight = Math.min(this.getMaxHeight(), this.lines.size() * (this.fontRenderer.lineHeight + 2));
            this.setDimensions(this.getDimensions().setSize(newWidth, newHeight));
        }
    }

    public void setText(String text) {
        this.setText(Component.translatable(text));
    }

    public void setText(Component text) {
        this.text = text;

        this.lines = this.wrapText ? ComponentRenderUtils.wrapComponents(this.text, this.getMaxWidth(), this.fontRenderer) : Lists.newArrayList(text.getVisualOrderText());
        this.longestLineWidth = this.lines.stream()
                .mapToInt(this.fontRenderer::width) // getStringWidth
                .max()
                .orElseGet(this::getMaxWidth);
        this.resize();
    }

    public Component getText() {
        return this.text;
    }

    private int getMaxWidth() {
        if (this.autoSize) {
            return this.maxWidth == -1 ? Integer.MAX_VALUE : this.maxWidth;
        } else {
            return (int) this.getDimensions().getWidth();
        }
    }

    private int getMaxHeight() {
        if (this.autoSize) {
            return this.maxHeight == -1 ? Integer.MAX_VALUE : this.maxHeight;
        } else {
            return (int) this.getDimensions().getHeight();
        }
    }

}
