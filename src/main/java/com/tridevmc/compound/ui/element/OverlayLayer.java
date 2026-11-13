package com.tridevmc.compound.ui.element;

import com.tridevmc.compound.ui.tree.ITreeNode;

final class OverlayLayer {
    static int aboveAncestors(ITreeNode node, int preferred) {
        int layer = Math.max(preferred, node.getLayoutProperties().getLayer());
        for (var parent = node.getParent(); parent != null; parent = parent.getParent()) {
            int parentLayer = parent.getLayoutProperties().getLayer();
            layer = Math.max(layer, parentLayer == Integer.MAX_VALUE
                    ? Integer.MAX_VALUE : parentLayer + 1);
        }
        return layer;
    }
}
