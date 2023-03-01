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

package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

import javax.annotation.Nonnull;

/**
 * A layout that makes the positioning of elements relative to the top left of the provided element.
 */
public class LayoutRelative implements ILayout {

    private IElement relativeTo;

    public LayoutRelative(@Nonnull IElement relativeTo) {
        this.relativeTo = relativeTo;
    }

    @Override
    public Rect2F getTransformedRect(IScreenContext screen, IElement element, Rect2F rect) {
        Rect2F parentDimensions = this.relativeTo.getTransformedDimensions(screen);
        return rect.offsetPosition(parentDimensions.getX(), parentDimensions.getY());
    }

}
