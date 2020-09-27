package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * A layout that makes no changes to the rect that it gets passed.
 */
public class LayoutNone implements ILayout {
    @Override
    public Rect2F getTransformedRect(IScreenContext screen, IElement element, Rect2F rect) {
        return rect;
    }
}
