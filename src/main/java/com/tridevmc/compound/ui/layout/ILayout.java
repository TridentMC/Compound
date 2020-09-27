package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * Defines the layout for a UI element.
 */
public interface ILayout {

    /**
     * Transforms the given rect to match the layout requirements.
     *
     * @param screen  the screen that the element is being drawn on.
     * @param element the element that
     * @param rect    the rect to transform.
     * @return the transformed rect.
     */
    Rect2F getTransformedRect(IScreenContext screen, IElement element, Rect2F rect);

}
