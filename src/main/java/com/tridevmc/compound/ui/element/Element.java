package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.layout.ILayout;

import javax.annotation.Nonnull;

public class Element implements IElement {
    private Rect2D dimensions;
    private ILayout layout;

    public Element(Rect2D dimensions, ILayout layout) {
        this.dimensions = dimensions;
        this.layout = layout;
    }

    @Nonnull
    @Override
    public Rect2D getDimensions() {
        return dimensions;
    }

    @Override
    public void setDimensions(@Nonnull Rect2D dimensions) {
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
