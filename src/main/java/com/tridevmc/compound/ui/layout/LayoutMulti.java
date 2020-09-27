package com.tridevmc.compound.ui.layout;

import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.Rect2F;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

import java.util.List;

/**
 * A layout made up of other layouts, applied in the order they were added.
 */
public class LayoutMulti implements ILayout {

    private List<ILayout> layouts;

    public LayoutMulti(ILayout... layouts) {
        this.layouts = Lists.newArrayList(layouts);
    }

    public void addLayout(ILayout layout) {
        this.layouts.add(layout);
    }

    @Override
    public Rect2F getTransformedRect(IScreenContext screen, IElement element, Rect2F rect) {
        for (ILayout layout : this.layouts) {
            rect = layout.getTransformedRect(screen, element, rect);
        }
        return rect;
    }
}
