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
    public Rect2F getScreenspaceRect(IScreenContext screen, IElement element, Rect2F rect) {
        float width = screen.getWidth();
        float height = screen.getHeight();
        if (this.parent != null) {
            var parentDimensions = this.parent.getDrawnDimensions(screen);
            width = parentDimensions.getWidth();
            height = parentDimensions.getHeight();
        }

        // Round centered positions to avoid subpixel misalignment
        var offsetX = this.horizontal ? Math.round((width / 2F) - (rect.getWidth() / 2F)) : 0;
        var offsetY = this.vertical ? Math.round((height / 2F) - (rect.getHeight() / 2F)) : 0;
        return rect.offsetPosition(offsetX, offsetY);
    }

}
