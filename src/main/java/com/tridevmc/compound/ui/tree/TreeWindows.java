package com.tridevmc.compound.ui.tree;

import com.tridevmc.compound.ui.element.internal.IWindowedElement;
import com.tridevmc.compound.ui.layout.Bounds;

final class TreeWindows {
    static boolean reconcile(ITreeNode node, Bounds viewport) {
        if (node.getBounds() == null) return false;
        for (var parent = node; parent != null; parent = parent.getParent()) {
            if (parent.getLayoutProperties().isClip() && parent.getBounds() != null) {
                viewport = viewport.intersection(parent.getBounds());
            }
        }
        return ((IWindowedElement) node.getElement()).prepareWindow(viewport);
    }

    static boolean insideWindow(ITreeNode node) {
        for (var parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.getElement() instanceof IWindowedElement) return true;
        }
        return false;
    }
}
