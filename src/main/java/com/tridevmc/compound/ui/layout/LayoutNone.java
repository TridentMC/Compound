package com.tridevmc.compound.ui.layout;

import com.tridevmc.compound.ui.Rect2D;
import com.tridevmc.compound.ui.screen.IScreenContext;

public class LayoutNone implements ILayout {
    @Override
    public Rect2D getTransformedRect(IScreenContext screen, Rect2D rect2D) {
        return rect2D;
    }
}
