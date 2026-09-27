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

import javax.annotation.Nonnull;
import java.util.List;

public class Divider extends BaseElement implements IComposableElement {

    private static final int DEFAULT_COLOR = 0xFF808080;
    private static final int DEFAULT_THICKNESS = 1;

    private int color;
    private int thickness;

    public Divider() {
        this(DEFAULT_COLOR, DEFAULT_THICKNESS);
    }

    public Divider(int color) {
        this(color, DEFAULT_THICKNESS);
    }

    public Divider(int color, int thickness) {
        this.color = color;
        this.thickness = Math.max(0, thickness);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.e(new Rect(() -> this.color), rect -> rect.layout().fillMax());
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setThickness(int thickness) {
        this.thickness = Math.max(0, thickness);
        this.invalidateLayout();
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        return new Size(constraints.maxWidth(), Math.min(this.thickness, constraints.maxHeight()));
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }
}
