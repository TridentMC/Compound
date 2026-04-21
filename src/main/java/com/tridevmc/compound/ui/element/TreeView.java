/*
 * Copyright 2018 - 2024 TridentMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridevmc.compound.ui.element;

import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.scope.IContainerScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A hierarchical tree view component for displaying nested data structures.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new TreeView<>(), tree -> {
 *     var root = tree.getElement().getRoot();
 *     root.addChild("Folder 1", child1 -> {
 *         child1.addChild("File 1.1");
 *         child1.addChild("File 1.2");
 *     });
 *     root.addChild("Folder 2", child2 -> {
 *         child2.addChild("File 2.1");
 *     });
 * });
 * </pre>
 */
public class TreeView<T> extends BaseElement implements IComposableElement {

    private static final int DEFAULT_ITEM_HEIGHT = 16;
    private static final int INDENT_SIZE = 16;

    private final State<Boolean> enabled = new StateImpl<>(true);
    private final TreeNode<T> root;
    private Function<T, String> displayTextProvider = Object::toString;
    private Consumer<TreeNode<T>> onSelectionChanged;
    private int itemHeight = DEFAULT_ITEM_HEIGHT;

    public TreeView() {
        this.root = new TreeNode<>(null, null);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.enabled);

        scope.e(new ScrollArea(), scrollArea -> {
            scrollArea.layout().fillMax();

            scrollArea.fillSlot(ScrollArea.CONTENT_SLOT, content -> {
                content.e(new Column(), column -> {
                    column.layout().fillMaxWidth();
                    this.renderNode(column, this.root, 0);
                });
            });
        });
    }

    private void renderNode(IContainerScope<Column> columnScope, TreeNode<T> node, int depth) {
        if (node != this.root) {
            final TreeNode<T> currentNode = node;
            boolean isSelected = node.isSelected();
            boolean isHovered = node.isHovered();

            columnScope.e(new Stack(), itemStack -> {
                itemStack.layout().fixedHeight(this.itemHeight).fillMaxWidth();

                if (isSelected) {
                    itemStack.e(new Rect(0xFF3366CC), bg -> bg.layout().fillMax());
                } else if (isHovered) {
                    itemStack.e(new Rect(0xFF224488), bg -> bg.layout().fillMax());
                }

                itemStack.e(new Label(
                        Component.literal(this.getNodeText(currentNode)),
                        () -> isSelected || isHovered ? 0xFFFFFFFF : 0xFFFFFF,
                        () -> false
                ), label -> label.layout()
                        .contentAlignment(Alignment.CENTER_LEFT)
                        .padding(4 + depth * INDENT_SIZE, 0));
            });

            columnScope.onClick(event -> {
                if (!this.enabled.get()) return false;
                this.selectNode(currentNode);
                return true;
            });

            columnScope.onMouseMove(event -> {
                currentNode.setHovered(true);
                return false;
            });
        }

        if (node.isExpanded()) {
            for (TreeNode<T> child : node.getChildren()) {
                this.renderNode(columnScope, child, depth + 1);
            }
        }
    }

    private String getNodeText(TreeNode<T> node) {
        if (node.getValue() == null) return "";
        String text = this.displayTextProvider.apply(node.getValue());
        if (!node.getChildren().isEmpty()) {
            text = (node.isExpanded() ? "▼ " : "▶ ") + text;
        }
        return text;
    }

    private void selectNode(TreeNode<T> node) {
        if (!node.getChildren().isEmpty()) {
            node.setExpanded(!node.isExpanded());
        }

        this.deselectAll(this.root);
        node.setSelected(true);

        if (this.onSelectionChanged != null) {
            this.onSelectionChanged.accept(node);
        }
    }

    private void deselectAll(TreeNode<T> node) {
        node.setSelected(false);
        for (TreeNode<T> child : node.getChildren()) {
            this.deselectAll(child);
        }
    }

    public TreeNode<T> getRoot() {
        return this.root;
    }

    public void setDisplayTextProvider(Function<T, String> provider) {
        this.displayTextProvider = provider;
    }

    public void setOnSelectionChanged(Consumer<TreeNode<T>> listener) {
        this.onSelectionChanged = listener;
    }

    public void setItemHeight(int height) {
        this.itemHeight = height;
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    /**
     * A node in the tree structure.
     */
    public static class TreeNode<T> {
        private final T value;
        private final TreeNode<T> parent;
        private final List<TreeNode<T>> children = Lists.newArrayList();
        private boolean expanded = true;
        private boolean selected = false;
        private boolean hovered = false;

        TreeNode(T value, TreeNode<T> parent) {
            this.value = value;
            this.parent = parent;
        }

        public TreeNode<T> addChild(T value) {
            TreeNode<T> child = new TreeNode<>(value, this);
            this.children.add(child);
            return child;
        }

        public TreeNode<T> addChild(T value, Consumer<TreeNode<T>> configurator) {
            TreeNode<T> child = this.addChild(value);
            configurator.accept(child);
            return child;
        }

        public void removeChild(TreeNode<T> child) {
            this.children.remove(child);
        }

        public T getValue() {
            return this.value;
        }

        public TreeNode<T> getParent() {
            return this.parent;
        }

        public List<TreeNode<T>> getChildren() {
            return this.children;
        }

        public boolean isExpanded() {
            return this.expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        public boolean isSelected() {
            return this.selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public boolean isHovered() {
            return this.hovered;
        }

        public void setHovered(boolean hovered) {
            this.hovered = hovered;
        }
    }
}
