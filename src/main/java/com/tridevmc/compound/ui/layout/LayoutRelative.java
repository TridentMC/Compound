package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2D;
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
    public Rect2D getTransformedRect(IScreenContext screen, IElement element, Rect2D rect) {
        Rect2D parentDimensions = this.relativeTo.getTransformedDimensions(screen);
        return rect.offsetPosition(parentDimensions.getX(), parentDimensions.getY());
    }
}
