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
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotKey;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * A composable surface with a solid background and an optional uniform border.
 * Replaces ad-hoc construction of backgrounds and borders from raw {@link Rect} primitives.
 */
public class Surface extends BaseElement implements IComposableElement {

    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private final Supplier<Integer> backgroundColor;
    private final Supplier<Integer> borderColor;
    private final int borderWidth;

    /**
     * Creates a surface with the given background and border colors.
     *
     * @param backgroundColor the background color supplier
     * @param borderColor     the border color supplier
     * @param borderWidth     the border width in pixels
     */
    public Surface(Supplier<Integer> backgroundColor, Supplier<Integer> borderColor, int borderWidth) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
    }

    /**
     * Creates a surface with the given background color and no border.
     *
     * @param backgroundColor the background color supplier
     */
    public Surface(Supplier<Integer> backgroundColor) {
        this(backgroundColor, () -> 0, 0);
    }

    /**
     * Creates a surface with constant colors.
     *
     * @param backgroundColor the background color
     * @param borderColor     the border color
     * @param borderWidth     the border width in pixels
     */
    public Surface(int backgroundColor, int borderColor, int borderWidth) {
        this(() -> backgroundColor, () -> borderColor, borderWidth);
    }

    /**
     * Creates a surface with a constant background color and no border.
     *
     * @param backgroundColor the background color
     */
    public Surface(int backgroundColor) {
        this(() -> backgroundColor, () -> 0, 0);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            if (this.borderWidth > 0) {
                stack.e(new Rect(this.borderColor), border -> border.layout().fillMax());
                stack.e(new Rect(this.backgroundColor), bg -> bg.layout().fillMax().margin(this.borderWidth));
            } else {
                stack.e(new Rect(this.backgroundColor), bg -> bg.layout().fillMax());
            }

            scope.slotInto(CONTENT_SLOT, stack);
        });
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(0, 0);
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }
}
