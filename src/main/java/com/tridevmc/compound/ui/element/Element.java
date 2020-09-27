package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.layout.ILayout;

import javax.annotation.Nonnull;

/**
 * Simple base class for elements that includes some boilerplate for layouts and dimensions.
 */
public class Element implements IElement {
    private Rect2F dimensions;
    private ILayout layout;

    public Element(Rect2F dimensions, ILayout layout) {
        this.dimensions = dimensions;
        this.layout = layout;
    }

    @Nonnull
    @Override
    public Rect2F getDimensions() {
        return dimensions;
    }

    @Override
    public void setDimensions(@Nonnull Rect2F dimensions) {
        this.dimensions = dimensions;
    }

    @Nonnull
    @Override
    public ILayout getLayout() {
        return layout;
    }

    @Override
    public void setLayout(@Nonnull ILayout layout) {
        this.layout = layout;
    }
}
