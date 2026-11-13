package com.tridevmc.molecule.ui;

import com.tridevmc.compound.ui.element.Element;
import com.tridevmc.compound.ui.element.IComposableElement;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;

import java.util.List;

abstract class GalleryModule extends Element implements IComposableElement {
    @Override
    public Size measure(Constraints constraints, LayoutProperties properties, List<Size> children) {
        return children.isEmpty() ? Size.ZERO : children.getFirst();
    }

    @Override
    public List<Bounds> place(Bounds bounds, LayoutProperties properties, List<Size> children) {
        return children.isEmpty() ? List.of() : List.of(bounds);
    }
}
