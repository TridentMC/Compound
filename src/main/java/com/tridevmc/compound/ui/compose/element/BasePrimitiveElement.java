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
import com.tridevmc.compound.ui.screen.IScreenContext;

import java.util.List;

/**
 * Base class for primitive elements that provides common boilerplate.
 * Handles visibility checks and default placement behavior.
 */
public abstract class BasePrimitiveElement extends BaseElement implements IPrimitiveElement {

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);
    }

    @Override
    public final void draw(IScreenContext context) {
        if (!this.isVisible()) {
            return;
        }

        var bounds = this.getBounds();
        if (bounds == null) {
            return;
        }

        this.drawElement(context, bounds);
    }

    /**
     * Draws this element. Called only when visible and bounds are set.
     *
     * @param context the screen context for drawing
     * @param bounds  the element's bounds (guaranteed non-null)
     */
    protected abstract void drawElement(IScreenContext context, Bounds bounds);
}
