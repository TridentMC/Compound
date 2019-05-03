package com.tridevmc.compound.ui.layout;

import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.element.IElement;
import com.tridevmc.compound.ui.screen.IScreenContext;

import java.util.List;

public class LayoutMulti implements ILayout {

    private List<ILayout> layouts;

    public LayoutMulti(ILayout... layouts) {
        this.layouts = Lists.newArrayList(layouts);
    }

    public void addLayout(ILayout layout) {
        this.layouts.add(layout);
    }

    @Override
    public Rect2D getTransformedRect(IScreenContext screen, IElement element, Rect2D rect2D) {
        for (ILayout layout : this.layouts) {
            rect2D = layout.getTransformedRect(screen, element, rect2D);
        }
        return rect2D;
    }
}
