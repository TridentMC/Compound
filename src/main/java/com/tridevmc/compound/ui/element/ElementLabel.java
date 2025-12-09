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

public class ElementLabel extends BasePrimitiveElement {

    private Supplier<Component> textSupplier;
    private Supplier<Integer> colorSupplier;
    private Supplier<Boolean> shadowSupplier;

    public ElementLabel(Component text) {
        this(() -> text, () -> 0xFFFFFF, () -> true);
    }

    public ElementLabel(Component text, int color) {
        this(() -> text, () -> color, () -> true);
    }

    public ElementLabel(Component text, int color, boolean shadow) {
        this(() -> text, () -> color, () -> shadow);
    }

    public ElementLabel(Component text, Supplier<Integer> colorSupplier) {
        this(() -> text, colorSupplier, () -> true);
    }

    public ElementLabel(Component text, Supplier<Integer> colorSupplier, Supplier<Boolean> shadowSupplier) {
        this(() -> text, colorSupplier, shadowSupplier);
    }

    public ElementLabel(Supplier<Component> textSupplier, Supplier<Integer> colorSupplier) {
        this(textSupplier, colorSupplier, () -> true);
    }

    public ElementLabel(Supplier<Component> textSupplier, Supplier<Integer> colorSupplier, Supplier<Boolean> shadowSupplier) {
        this.textSupplier = textSupplier;
        this.colorSupplier = colorSupplier;
        this.shadowSupplier = shadowSupplier;
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        var font = Minecraft.getInstance().font;
        var text = this.textSupplier.get();
        int width;
        int height;

        if (font != null) {
            width = font.width(text);
            height = font.lineHeight;
        } else {
            width = text.getString().length() * 6;
            height = 9;
        }

        width = Math.min(width, constraints.maxWidth());
        height = Math.min(height, constraints.maxHeight());

        return new Size(width, height);
    }

    @Override
    protected void drawElement(IScreenContext context, @Nonnull Bounds bounds) {
        boolean shadow = this.shadowSupplier.get();
        int color = this.colorSupplier.get();
        var text = this.textSupplier.get();

        Component coloredText = text.copy().withStyle(style -> style.withColor(color));

        if (shadow) {
            context.drawTextWithShadow(coloredText, bounds.x(), bounds.y());
        } else {
            context.drawText(coloredText, bounds.x(), bounds.y());
        }
    }

    public Component getText() {
        return this.textSupplier.get();
    }

    public void setText(Component text) {
        this.textSupplier = () -> text;
    }

    public Supplier<Component> getTextSupplier() {
        return this.textSupplier;
    }

    public void setTextSupplier(Supplier<Component> textSupplier) {
        this.textSupplier = textSupplier;
    }

    public int getColor() {
        return this.colorSupplier.get();
    }

    public void setColor(int color) {
        this.colorSupplier = () -> color;
    }

    public Supplier<Integer> getColorSupplier() {
        return this.colorSupplier;
    }

    public void setColorSupplier(Supplier<Integer> colorSupplier) {
        this.colorSupplier = colorSupplier;
    }

    public boolean isShadow() {
        return this.shadowSupplier.get();
    }

    public void setShadow(boolean shadow) {
        this.shadowSupplier = () -> shadow;
    }

    public Supplier<Boolean> getShadowSupplier() {
        return this.shadowSupplier;
    }

    public void setShadowSupplier(Supplier<Boolean> shadowSupplier) {
        this.shadowSupplier = shadowSupplier;
    }
}
