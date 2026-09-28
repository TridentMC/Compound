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
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.screen.IScreenContext;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Base class for leaf elements that draw within their assigned bounds.
 * Subclasses implement measurement and {@link #drawElement}; primitives have no children.
 * The tree checks visibility before drawing.
 */
public abstract class PrimitiveElement extends Element implements IPrimitiveElement {

    /** Creates an unattached drawing primitive with no children. */
    public PrimitiveElement() {
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties ownProperties, List<Size> measuredChildren) {
        return List.of();
    }

    /** {@inheritDoc} */
    @Override
    public final void draw(IScreenContext context) {
        this.drawElement(context, this.getBounds());
    }

    /**
     * Draws this primitive at its placed screen coordinates.
     * Called by {@link #draw} after the tree has assigned bounds.
     *
     * @param context the screen context for drawing
     * @param bounds  the element's bounds (guaranteed non-null)
     */
    protected abstract void drawElement(IScreenContext context, @Nonnull Bounds bounds);
}

