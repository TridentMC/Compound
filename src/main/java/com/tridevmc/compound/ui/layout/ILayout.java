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

/**
 * Defines the layout for a UI element.
 */
public interface ILayout {

    /**
     * Transforms the given rect into screenspace, this is used to determine the position of the element on the screen.
     *
     * @param screen  the screen that the element is being drawn on.
     * @param element the element that
     * @param rect    the rect to transform.
     * @return the transformed rect.
     */
    Rect2F getScreenspaceRect(IScreenContext screen, IElement element, Rect2F rect);

    /**
     * Gets the rect used for drawing the element on the screen, allows the layout to effectively overwrite the position of an element even when the element uses a managed matrix stack.
     *
     * @param screen  the screen that the element is being drawn on.
     * @param element the element that is being drawn.
     * @param rect    the rect that is being drawn.
     * @return the rect to draw the element at.
     */
    default Rect2F getDrawnRect(IScreenContext screen, IElement element, Rect2F rect) {
        return !element.useManagedMatrix() ? this.getScreenspaceRect(screen, element, rect) : rect;
    }

    /**
     * Applies the layout to the active matrix stack, pushes and pops are expected to be handled by the caller.
     *
     * @param screen  the screen that the element is being drawn on.
     * @param element the element that is being drawn.
     */
    default void applyToMatrix(IScreenContext screen, IElement element) {
        var origin = this.getScreenspaceRect(screen, element, element.getDimensions().setPosition(0, 0));

        screen.getActiveStack().translate(origin.getX(), origin.getY(), 0);
    }

}
