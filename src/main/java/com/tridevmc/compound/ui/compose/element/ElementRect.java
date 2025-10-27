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

import java.util.function.Supplier;

/**
 * A primitive element that renders a colored rectangle.
 */
public class ElementRect extends BaseElement implements IPrimitiveElement {

    private Supplier<Integer> colorSupplier;

    public ElementRect(int color) {
        this(() -> color);
    }

    public ElementRect(Supplier<Integer> colorSupplier) {
        this.colorSupplier = colorSupplier;
    }

    @Override
    public Size measure(Constraints constraints) {
        // Fill available space by default
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

        int color = this.colorSupplier.get();
        context.drawRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), color);
    }

    public Supplier<Integer> getColorSupplier() {
        return this.colorSupplier;
    }

    public void setColorSupplier(Supplier<Integer> colorSupplier) {
        this.colorSupplier = colorSupplier;
    }
}
