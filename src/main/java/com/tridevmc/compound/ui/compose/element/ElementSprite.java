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

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.compose.layout.Bounds;
import com.tridevmc.compound.ui.compose.layout.Constraints;
import com.tridevmc.compound.ui.compose.layout.Size;
import com.tridevmc.compound.ui.screen.IScreenContext;
import com.tridevmc.compound.ui.sprite.IScreenSprite;

import java.util.List;
import java.util.function.Supplier;

/**
 * A primitive element that renders a sprite (textured image).
 */
public class ElementSprite extends BaseElement implements IPrimitiveElement {

    private Supplier<IScreenSprite> spriteSupplier;

    public ElementSprite(IScreenSprite sprite) {
        this(() -> sprite);
    }

    public ElementSprite(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        // Fill available space by default
        // TODO: Could use sprite's natural size if available
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
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

        IScreenSprite sprite = this.spriteSupplier.get();
        if (sprite == null) {
            return;
        }

        // Convert bounds to Rect2F for sprite drawing
        var rect = new Rect2F(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        context.drawSprite(sprite, rect);
    }

    public IScreenSprite getSprite() {
        return this.spriteSupplier.get();
    }

    public void setSprite(IScreenSprite sprite) {
        this.spriteSupplier = () -> sprite;
    }

    public void setSpriteSupplier(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }
}
