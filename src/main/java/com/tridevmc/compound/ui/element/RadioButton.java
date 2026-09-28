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

import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * A passive square selection indicator composed from colored rectangles.
 * It does not handle clicks or coordinate selection; use {@link RadioButtonGroup}
 * for an interactive group styled with vanilla checkbox sprites.
 */
public class RadioButton extends BaseElement implements IComposableElement {

    private static final int DEFAULT_SIZE = 12;
    private static final int DEFAULT_INSET = 3;

    private final Supplier<Boolean> selected;
    private final Supplier<Integer> outerColor;
    private final Supplier<Integer> innerColor;
    private final int size;
    private final int inset;

    /**
     * Creates a radio button indicator.
     *
     * @param selected  supplier for whether the indicator is selected
     * @param outerColor supplier for the outer ARGB color
     * @param innerColor supplier for the inner selected square's ARGB color
     */
    public RadioButton(Supplier<Boolean> selected, Supplier<Integer> outerColor, Supplier<Integer> innerColor) {
        this.selected = selected;
        this.outerColor = outerColor;
        this.innerColor = innerColor;
        this.size = DEFAULT_SIZE;
        this.inset = DEFAULT_INSET;
    }

    /**
     * Creates a radio button indicator with constant colors.
     *
     * @param selected   whether the indicator is selected
     * @param outerColor the outer ARGB color
     * @param innerColor the inner selected square's ARGB color
     */
    public RadioButton(boolean selected, int outerColor, int innerColor) {
        this(() -> selected, () -> outerColor, () -> innerColor);
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Stack(), stack -> {
            stack.layout().fixedSize(this.size, this.size).contentAlignment(Alignment.CENTER);

            stack.e(new Rect(this.outerColor), outer -> outer.layout().fillMax());

            stack.e(new Rect(() -> this.selected.get() ? this.innerColor.get() : 0x00000000), inner ->
                    inner.layout().fillMax().margin(this.inset));
        });
    }

    /** {@inheritDoc} */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(this.size, this.size);
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }
}
