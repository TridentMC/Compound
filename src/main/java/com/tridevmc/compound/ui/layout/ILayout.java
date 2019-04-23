package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * Defines the layout for a UI element.
 */
public interface ILayout {

    /**
     * Transforms the given rect to match the layout requirements.
     *
     * @param screen the screen that the element is being drawn on.
     * @param rect2D the rect to transform.
     * @return the transformed rect.
     */
    Rect2D getTransformedRect(IScreenContext screen, Rect2D rect2D);

}
