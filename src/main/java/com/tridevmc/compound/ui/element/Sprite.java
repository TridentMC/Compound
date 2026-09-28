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

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.sprite.IScreenSprite;

import java.util.List;
import java.util.function.Supplier;

/**
 * Draws a screen sprite in the assigned bounds. A null sprite suppresses drawing.
 * Sprite suppliers are sampled each frame.
 */
public class Sprite extends PrimitiveElement {

    private Supplier<IScreenSprite> spriteSupplier;

    /**
     * Creates a sprite element.
     *
     * @param sprite the sprite to draw, or null to suppress drawing
     */
    public Sprite(IScreenSprite sprite) {
        this(() -> sprite);
    }

    /**
     * Creates a sprite element.
     *
     * @param spriteSupplier the non-null sprite supplier; a null result suppresses drawing
     */
    public Sprite(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        // If fillMax is set, the sprite should NOT affect parent sizing
        // This allows sprites to fill available space in Stack without dominating measurement
        if (ownProperties.isFillMaxWidth() && ownProperties.isFillMaxHeight()) {
            return new Size(0, 0);
        }
        if (ownProperties.isFillMaxWidth()) {
            return new Size(0, constraints.maxHeight());
        }
        if (ownProperties.isFillMaxHeight()) {
            return new Size(constraints.maxWidth(), 0);
        }
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    protected void drawElement(IScreenContext context, Bounds bounds) {
        IScreenSprite sprite = this.spriteSupplier.get();
        if (sprite == null) {
            return;
        }

        // Note: Scissor test is handled by UITree renderNode() via context.enableScissor()
        // GL scissor test should clip this sprite automatically
        var rect = new Rect2F(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        context.drawSprite(sprite, rect);
    }

    /**
     * Evaluates the current sprite supplier.
     *
     * @return the current sprite, or null
     */
    public IScreenSprite getSprite() {
        return this.spriteSupplier.get();
    }

    /**
     * Replaces the sprite supplier with a constant.
     *
     * @param sprite the sprite to draw, or null to suppress drawing
     */
    public void setSprite(IScreenSprite sprite) {
        this.spriteSupplier = () -> sprite;
    }

    /**
     * Replaces the sprite supplier sampled during drawing.
     *
     * @param spriteSupplier the non-null sprite supplier; a null result suppresses drawing
     */
    public void setSpriteSupplier(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }
}
