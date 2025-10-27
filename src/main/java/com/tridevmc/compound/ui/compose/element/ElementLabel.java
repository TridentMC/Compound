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

package com.tridevmc.compound.ui.compose.element;

import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.screen.IScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * A primitive element that renders text.
 */
public class ElementLabel extends BaseElement implements IPrimitiveElement {

    private Component text;
    private int color;
    private boolean shadow;

    public ElementLabel(Component text) {
        this(text, 0xFFFFFF, true);
    }

    public ElementLabel(Component text, int color) {
        this(text, color, true);
    }

    public ElementLabel(Component text, int color, boolean shadow) {
        this.text = text;
        this.color = color;
        this.shadow = shadow;
    }

    @Override
    public Size measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        int width = font.width(this.text);
        int height = font.lineHeight;

        // Constrain to available space
        width = Math.min(width, constraints.maxWidth());
        height = Math.min(height, constraints.maxHeight());

        return new Size(width, height);
    }

    @Override
    public void place(Bounds bounds) {
        this.setBounds(bounds);
    }

    @Override
    public void draw(IScreenContext context) {
        if (!this.isVisible()) {
            return;
        }

        var bounds = this.getBounds();
        if (bounds == null) {
            return;
        }

        // TODO: Color support - the drawText methods don't take a color parameter
        // May need to use lower-level drawing or look for alternate methods
        if (this.shadow) {
            context.drawTextWithShadow(this.text, bounds.x(), bounds.y());
        } else {
            context.drawText(this.text, bounds.x(), bounds.y());
        }
    }

    public Component getText() {
        return this.text;
    }

    public void setText(Component text) {
        this.text = text;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isShadow() {
        return this.shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }
}
