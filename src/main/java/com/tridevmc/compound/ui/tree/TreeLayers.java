package com.tridevmc.compound.ui.tree;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class TreeLayers {
    static boolean isOverlay(ITreeNode node) {
        return node.getLayoutProperties().getLayer() > 0 && !TreeWindows.insideWindow(node);
    }

    static Optional<ITreeNode> group(ITreeNode node) {
        for (var parent = node; parent != null; parent = parent.getParent()) {
            if (isOverlay(parent)) return Optional.of(parent);
        }
        return Optional.empty();
    }

    static void collect(ITreeNode root, List<ITreeNode> result) {
        result.clear();
        if (root.getElement().isVisible() && isOverlay(root)) result.add(root);
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
