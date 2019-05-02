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
    public Rect2D getTransformedRect(IScreenContext screen, Rect2D rect2D) {
        Rect2D out = this.relativeTo.getTransformedDimensions(screen);
        out.setX(out.getX() + rect2D.getX());
        out.setY(out.getY() + rect2D.getY());
        out.setWidth(rect2D.getWidth());
        out.setHeight(rect2D.getHeight());
        return out;
    }
}
