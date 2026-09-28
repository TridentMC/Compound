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

import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * A composable one-pixel caret. Offset and height suppliers are sampled during composition;
 * recompose when either changes. The color supplier is sampled each frame.
 */
public class Cursor extends BaseElement implements IComposableElement {

    private final Supplier<Integer> offsetSupplier;
    private final Supplier<Integer> colorSupplier;
    private final Supplier<Integer> heightSupplier;

    /**
     * Creates a caret with supplied geometry and color.
     *
     * @param offsetSupplier the non-null supplier of horizontal pixel offsets, sampled during composition
     * @param colorSupplier the non-null supplier of ARGB drawing colors
     * @param heightSupplier the non-null supplier of caret heights in pixels, sampled during composition
     */
    public Cursor(Supplier<Integer> offsetSupplier, Supplier<Integer> colorSupplier, Supplier<Integer> heightSupplier) {
        this.offsetSupplier = offsetSupplier;
        this.colorSupplier = colorSupplier;
        this.heightSupplier = heightSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compose(ICompositionScope scope) {
        int offset = this.offsetSupplier.get();
        int height = this.heightSupplier.get();

        scope.e(new Row(), row -> {
            row.layout().fillMaxHeight();
            row.e(new Spacer(), spacer -> spacer.layout().fixedWidth(offset));
            row.e(new Rect(this.colorSupplier), rect -> rect.layout().fixedSize(1, height));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(0, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }
}
