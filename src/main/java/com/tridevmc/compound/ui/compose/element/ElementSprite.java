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

public class ElementSprite extends BasePrimitiveElement {

    private Supplier<IScreenSprite> spriteSupplier;

    public ElementSprite(IScreenSprite sprite) {
        this(() -> sprite);
    }

    public ElementSprite(Supplier<IScreenSprite> spriteSupplier) {
        this.spriteSupplier = spriteSupplier;
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
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
