/*
 * Copyright 2018 - 2022 TridentMC
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

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.layout.ILayout;

import javax.annotation.Nonnull;

/**
 * Simple base class for elements that includes some boilerplate for layouts and dimensions.
 */
public class Element implements IElement {

    private Rect2F dimensions;
    private ILayout layout;

    public Element(Rect2F dimensions, ILayout layout) {
        this.dimensions = dimensions;
        this.layout = layout;
    }

    @Nonnull
    @Override
    public Rect2F getDimensions() {
        return dimensions;
    }

    @Override
    public void setDimensions(@Nonnull Rect2F dimensions) {
        this.dimensions = dimensions;
    }

    @Nonnull
    @Override
    public ILayout getLayout() {
        return layout;
    }

    @Override
    public void setLayout(@Nonnull ILayout layout) {
        this.layout = layout;
    }

}
