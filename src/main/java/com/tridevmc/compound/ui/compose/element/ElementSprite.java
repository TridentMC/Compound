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

/**
 * A primitive element that renders a sprite (textured image).
 */
public class ElementSprite extends BaseElement implements IPrimitiveElement {

    private IScreenSprite sprite;

    public ElementSprite(IScreenSprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public Size measure(Constraints constraints) {
        // Fill available space by default
        // TODO: Could use sprite's natural size if available
        return new Size(constraints.maxWidth(), constraints.maxHeight());
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

        // Convert bounds to Rect2F for sprite drawing
        var rect = new Rect2F(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        context.drawSprite(this.sprite, rect);
    }

    public IScreenSprite getSprite() {
        return this.sprite;
    }

    public void setSprite(IScreenSprite sprite) {
        this.sprite = sprite;
    }
}
