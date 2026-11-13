package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.geometry.api.LocalPoint;
import java.util.Optional;

final class PointerPosition {
    private PointerPosition() {
    }

    static Optional<LocalPoint> local(Element element, int x, int y) {
        var node = element.getNode();
        if (node == null || node.getBounds() == null) return Optional.empty();
        var geometry = node.getFrameGeometry();
        return geometry == null
                ? Optional.of(new LocalPoint(x - node.getBounds().x(), y - node.getBounds().y()))
                : geometry.toLocal(x, y);
    }
}
