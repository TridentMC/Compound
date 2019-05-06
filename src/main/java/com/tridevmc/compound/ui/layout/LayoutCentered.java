package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2D;
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
    public Rect2D getTransformedRect(IScreenContext screen, IElement element, Rect2D rect) {
        double width = screen.getWidth();
        double height = screen.getHeight();
        if (this.parent != null) {
            Rect2D parentDimensions = this.parent.getTransformedDimensions(screen);
            width = parentDimensions.getWidth();
            height = parentDimensions.getHeight();
        }

        return rect.offsetPosition(this.horizontal ? (width / 2) - (rect.getWidth() / 2) : 0,
                this.vertical ? (height / 2) - (rect.getHeight() / 2) : 0);
    }
}
