package com.tridevmc.compound.ui.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

final class InputScopeOrder {
    @Nullable
    static ITreeNode highest(UITree tree, List<ITreeNode> scopes) {
        var root = tree.getRoot();
        if (root == null) return null;
        var nodeOrder = new HashMap<ITreeNode, Integer>();
        tree.walkDepthFirst(root, node -> nodeOrder.put(node, nodeOrder.size()));
        var available = scopes.stream().filter(tree::isAttached)
                .filter(nodeOrder::containsKey).filter(InputScopeOrder::isVisible).toList();
        var scopeNodes = new HashSet<>(available);
        var overlays = new ArrayList<ITreeNode>();
        TreeLayers.collect(root, overlays);
        var overlayOrder = new HashMap<ITreeNode, Integer>();
        for (var index = 0; index < overlays.size(); index++) overlayOrder.put(overlays.get(index), index);
        var priorities = new HashMap<ITreeNode, Integer>();
        for (var scope : available) {
            priorities.put(scope, TreeLayers.group(scope).map(overlayOrder::get).orElse(-1));
        }
        for (var overlay : overlays) {
            for (var owner = overlay; owner != null; owner = owner.getParent()) {
                if (!scopeNodes.contains(owner)) continue;
                if (hasPointerTarget(overlay, overlay, scopeNodes)) {
                    priorities.merge(owner, overlayOrder.get(overlay), Math::max);
                }
                break;
            }
        }
        ITreeNode highest = null;
        for (var scope : available) {
            if (highest == null || priorities.get(scope) > priorities.get(highest)
                    || (priorities.get(scope).equals(priorities.get(highest))
                    && nodeOrder.get(scope) > nodeOrder.get(highest))) highest = scope;
        }
        return highest == null ? root : highest;
    }

    private static boolean hasPointerTarget(ITreeNode node, ITreeNode group,
                                            Set<ITreeNode> scopes) {
        if (!node.getElement().isVisible()) return false;
        // Other input scopes and overlay groups receive their own paint-order priority.
        if (node != group && (scopes.contains(node) || TreeLayers.isOverlay(node))) return false;
        if (node.getElement().isHitTestable()) return true;
        for (var child : node.getChildren()) {
            if (hasPointerTarget(child, group, scopes)) return true;
        }
        return false;
    }

    private static boolean isVisible(ITreeNode node) {
        for (var ancestor = node; ancestor != null; ancestor = ancestor.getParent()) {
            if (!ancestor.getElement().isVisible()) return false;
        }
        return true;
    }
}
