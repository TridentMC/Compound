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

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * A primitive rectangle with a vertical gradient between two ARGB colors.
 */
public class GradientRect extends PrimitiveElement {

    private IntSupplier topColorSupplier;
    private IntSupplier bottomColorSupplier;

    /**
     * Creates a vertically shaded rectangle.
     *
     * @param topColor the top-edge ARGB color
     * @param bottomColor the bottom-edge ARGB color
     */
    public GradientRect(int topColor, int bottomColor) {
        this(() -> topColor, () -> bottomColor);
    }

    /**
     * Creates a vertically shaded rectangle.
     *
     * @param topColorSupplier the non-null supplier of top-edge ARGB colors
     * @param bottomColorSupplier the non-null supplier of bottom-edge ARGB colors
     */
    public GradientRect(IntSupplier topColorSupplier, IntSupplier bottomColorSupplier) {
        this.topColorSupplier = topColorSupplier;
        this.bottomColorSupplier = bottomColorSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    protected void drawElement(IScreenContext context, @Nonnull Bounds bounds) {
        int topColor = this.topColorSupplier.getAsInt();
        int bottomColor = this.bottomColorSupplier.getAsInt();
        context.drawGradientRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), topColor, bottomColor);
    }

    /**
     * Returns the supplier for the gradient's top edge.
     *
     * @return the configured supplier
     */
    public IntSupplier getTopColorSupplier() {
        return this.topColorSupplier;
    }

    /**
     * Replaces the top-edge color supplier sampled during drawing.
     *
     * @param topColorSupplier the non-null supplier of top-edge ARGB colors
     */
    public void setTopColorSupplier(IntSupplier topColorSupplier) {
        this.topColorSupplier = topColorSupplier;
    }

    /**
     * Returns the supplier for the gradient's bottom edge.
     *
     * @return the configured supplier
     */
    public IntSupplier getBottomColorSupplier() {
        return this.bottomColorSupplier;
    }

    /**
     * Replaces the bottom-edge color supplier sampled during drawing.
     *
     * @param bottomColorSupplier the non-null supplier of bottom-edge ARGB colors
     */
    public void setBottomColorSupplier(IntSupplier bottomColorSupplier) {
        this.bottomColorSupplier = bottomColorSupplier;
    }
}
