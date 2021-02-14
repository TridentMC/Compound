/*
 * Copyright 2018 - 2021 TridentMC
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

/**
 * A layout that centers elements on the screen, or can be combined with LayoutRelative to center within a parent.
 */
public class LayoutCentered implements ILayout {

    private IElement parent;
    private boolean horizontal, vertical;

    public LayoutCentered(IElement parent, boolean horizontal, boolean vertical) {
        this.parent = parent;
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public LayoutCentered(boolean horizontal, boolean vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    @Override
    public Rect2F getTransformedRect(IScreenContext screen, IElement element, Rect2F rect) {
        float width = screen.getWidth();
        float height = screen.getHeight();
        if (this.parent != null) {
            Rect2F parentDimensions = this.parent.getTransformedDimensions(screen);
            width = parentDimensions.getWidth();
            height = parentDimensions.getHeight();
        }

        return rect.offsetPosition(this.horizontal ? (width / 2) - (rect.getWidth() / 2) : 0,
                this.vertical ? (height / 2) - (rect.getHeight() / 2) : 0);
    }
}
