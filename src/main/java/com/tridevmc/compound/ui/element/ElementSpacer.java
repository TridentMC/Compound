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
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.screen.IScreenContext;

import java.util.List;

/**
 * A primitive element that takes up space but doesn't render anything.
 * Useful for creating gaps and flexible spacing in layouts.
 */
public class ElementSpacer extends BaseElement implements IPrimitiveElement {

    private int width;
    private int height;
    private boolean flexible;

    /**
     * Creates a flexible spacer that expands to fill available space.
     */
    public ElementSpacer() {
        this(0, 0, true);
    }

    /**
     * Creates a fixed-size spacer.
     *
     * @param width  the width of the spacer
     * @param height the height of the spacer
     */
    public ElementSpacer(int width, int height) {
        this(width, height, false);
    }

    private ElementSpacer(int width, int height, boolean flexible) {
        this.width = width;
        this.height = height;
        this.flexible = flexible;
    }

    @Override
    public Size measure(Constraints constraints, List<IElement> children) {
        if (this.flexible) {
            // Flexible spacer fills available space
            return new Size(constraints.maxWidth(), constraints.maxHeight());
        } else {
            // Fixed spacer uses specified dimensions
            return new Size(
                    Math.min(this.width, constraints.maxWidth()),
                    Math.min(this.height, constraints.maxHeight())
            );
        }
    }

    @Override
    public void place(Bounds bounds, List<IElement> children) {
        this.setBounds(bounds);
    }

    @Override
    public void draw(IScreenContext context) {
        // Spacers don't draw anything
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isFlexible() {
        return this.flexible;
    }

    public void setFlexible(boolean flexible) {
        this.flexible = flexible;
    }
}
