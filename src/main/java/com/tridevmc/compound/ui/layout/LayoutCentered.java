package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.screen.IScreenContext;

/**
 * A layout that centers elements on the screen.
 */
public class LayoutCentered implements ILayout {

    private boolean horizontal, vertical;

    public LayoutCentered(boolean horizontal, boolean vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    @Override
    public Rect2D getTransformedRect(IScreenContext screen, Rect2D rect2D) {
        Rect2D out = new Rect2D(rect2D);
        out.setX(this.horizontal ? ((double) screen.getWidth() / 2) - (rect2D.getWidth() / 2) + rect2D.getX() : rect2D.getX());
        out.setY(this.vertical ? ((double) screen.getHeight() / 2) - (rect2D.getHeight() / 2) + rect2D.getY() : rect2D.getY());
        return out;
    }
}
