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

public class Spacer extends BaseElement implements IPrimitiveElement {

    private int width;
    private int height;
    private boolean flexible;
    public Spacer() {
        this(0, 0, true);
    }

    public Spacer(int width, int height) {
        this(width, height, false);
    }

    private Spacer(int width, int height, boolean flexible) {
        this.width = width;
        this.height = height;
        this.flexible = flexible;
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
        if (this.flexible) {
            return new Size(constraints.maxWidth(), constraints.maxHeight());
        } else {
            return new Size(
                    Math.min(this.width, constraints.maxWidth()),
                    Math.min(this.height, constraints.maxHeight())
            );
        }
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties ownProperties, List<Size> measuredChildren) {
        return List.of();
    }

    @Override
    public void draw(IScreenContext context) {
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
        this.invalidateLayout();
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
        this.invalidateLayout();
    }

    public boolean isFlexible() {
        return this.flexible;
    }

    public void setFlexible(boolean flexible) {
        this.flexible = flexible;
        this.invalidateLayout();
    }
}
