package com.tridevmc.compound.ui.tree;

import java.util.Comparator;
import java.util.List;

final class TreeLayers {
    static boolean isOverlay(ITreeNode node) {
        return node.getLayoutProperties().getLayer() > 0 && !TreeWindows.insideWindow(node);
    }

    static void collect(ITreeNode root, List<ITreeNode> result) {
        result.clear();
        collectChildren(root, result);
        result.sort(Comparator.comparingInt(node -> node.getLayoutProperties().getLayer()));
    }

    private static void collectChildren(ITreeNode node, List<ITreeNode> result) {
        if (!node.getElement().isVisible()) return;
        for (var child : node.getChildren()) {
            if (!child.getElement().isVisible()) continue;
            if (isOverlay(child)) result.add(child);
            collectChildren(child, result);
        }
    }
}
