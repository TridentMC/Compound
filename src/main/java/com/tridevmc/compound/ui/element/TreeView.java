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

import com.mojang.blaze3d.platform.InputConstants;

import com.google.common.collect.Lists;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.scope.IContainerScope;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;


import javax.annotation.Nonnull;
import java.util.List;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A scrollable hierarchy with expandable rows and keyboard selection.
 * Add top-level nodes to the invisible root returned by {@link #getRoot()}.
 * Retain the instance to preserve its nodes, expansion state, selection, and scroll position.
 *
 * @param <T> the node value type.
 */
public class TreeView<T> extends BaseElement implements IComposableElement {

    private static final int DEFAULT_ITEM_HEIGHT = 16;
    private static final int INDENT_SIZE = 16;

    private final State<Boolean> enabled = State.of(true);
    private final TreeNode<T> root;
    private final ScrollArea scrollArea = new ScrollArea();
    private Function<T, String> displayTextProvider = Object::toString;
    private Consumer<TreeNode<T>> onSelectionChanged;
    private int itemHeight = DEFAULT_ITEM_HEIGHT;

    /**
     * Creates an empty hierarchy with an invisible root and 16-pixel rows.
     */
    public TreeView() {
        this.root = new TreeNode<>(null, null, this::invalidateComposition);
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.enabled);

        scope.onKeyPress(event -> this.navigate(event.keyCode()));
        scope.e(new Surface(0xFF181818), background -> {
            background.layout().fillMax();
            background.fillSlot(Surface.CONTENT_SLOT, body -> body.e(this.scrollArea, scroll -> {
                scroll.layout().fillMax();
                scroll.fillSlot(ScrollArea.CONTENT_SLOT, content -> content.e(new Column(), column -> {
                    column.layout().fillMaxWidth();
                    this.renderNode(column, this.root, -1, scope);
                }));
            }));
        });
    }

    private boolean navigate(int key) {
        if (!this.enabled.get()) return false;
        var nodes = this.visibleNodes();
        if (nodes.isEmpty()) return false;
        int selected = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).isSelected()) selected = i;
        }
        int target = switch (key) {
            case InputConstants.KEY_UP -> Math.max(0, selected - 1);
            case InputConstants.KEY_DOWN -> Math.min(nodes.size() - 1, selected + 1);
            case InputConstants.KEY_HOME -> 0;
            case InputConstants.KEY_END -> nodes.size() - 1;
            default -> -1;
        };
        if (target >= 0) {
            this.selectNode(nodes.get(target));
            this.revealRow(target);
            Minecraft.getInstance().getNarrator().saySystemNow(this.getNarrationMessage());
            return true;
        }
        if (key != InputConstants.KEY_LEFT && key != InputConstants.KEY_RIGHT) return false;
        TreeNode<T> node = nodes.get(Math.max(0, selected));
        this.selectNode(node);
        if (key == InputConstants.KEY_LEFT) {
            if (node.isExpanded() && !node.children.isEmpty()) node.setExpanded(false);
            else if (node.parent != this.root) this.selectNode(node.parent);
        } else if (!node.children.isEmpty()) {
            if (!node.isExpanded()) node.setExpanded(true);
            else this.selectNode(node.children.getFirst());
        }
        nodes = this.visibleNodes();
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).isSelected()) this.revealRow(i);
        }
        Minecraft.getInstance().getNarrator().saySystemNow(this.getNarrationMessage());
        return true;
    }

    private List<TreeNode<T>> visibleNodes() {
        List<TreeNode<T>> nodes = Lists.newArrayList();
        this.appendVisible(this.root, nodes);
        return nodes;
    }

    private void appendVisible(TreeNode<T> node, List<TreeNode<T>> nodes) {
        if (node != this.root) nodes.add(node);
        if (node.isExpanded()) {
            for (var child : node.children) this.appendVisible(child, nodes);
        }
    }

    private void revealRow(int index) {
        int top = index * this.itemHeight;
        int scroll = this.scrollArea.getScrollYState().get();
        int height = this.scrollArea.getBounds().height();
        if (height <= 0) return;
        if (top < scroll) scroll = top;
        else if (top + this.itemHeight > scroll + height) scroll = top + this.itemHeight - height;
        this.scrollArea.scrollTo(0, Math.clamp(scroll, 0, this.scrollArea.getMaxScrollY()));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFocusable() {
        return this.enabled.get();
    }

    /** {@inheritDoc} */
    @Override
    public Component getNarrationMessage() {
        for (var node : this.visibleNodes()) {
            if (node.isSelected()) {
                String state = node.children.isEmpty() ? "" : node.isExpanded() ? ", expanded" : ", collapsed";
                return Component.literal(this.displayTextProvider.apply(node.value) + state);
            }
        }
        return Component.literal("Tree");
    }

    private void renderNode(IContainerScope<Column> columnScope, TreeNode<T> node, int depth,
                            ICompositionScope scope) {
        if (node != this.root) {
            final TreeNode<T> currentNode = node;

            columnScope.e(new Stack(), itemStack -> {
                itemStack.layout().fixedHeight(this.itemHeight).fillMaxWidth();

                itemStack.e(new Surface(
                        () -> currentNode.isHovered() ? 0xFF303030 : 0x00000000,
                        () -> currentNode.isSelected() ? 0xFFFFFFFF : 0x00000000, 1),
                        bg -> bg.layout().fillMax());

                itemStack.e(new Box(), labelBox -> {
                    labelBox.layout().fillMax().contentAlignment(Alignment.CENTER_LEFT)
                            .padding(4 + depth * INDENT_SIZE, 0);
                    labelBox.e(new Label(Component.literal(this.getNodeText(currentNode)),
                            () -> this.enabled.get() ? 0xFFFFFF : 0xA0A0A0));
                });

                itemStack.onMouseEnter(() -> currentNode.setHovered(true));
                itemStack.onMouseExit(() -> currentNode.setHovered(false));

                itemStack.onClick(event -> {
                    if (!this.enabled.get() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                    scope.requestFocus();
                    if (!currentNode.children.isEmpty()) {
                        currentNode.setExpanded(!currentNode.isExpanded());
                    }
                    this.selectNode(currentNode);
                    Minecraft.getInstance().getNarrator().saySystemNow(this.getNarrationMessage());
                    return true;
                });
            });
        }

        if (node.isExpanded()) {
            for (TreeNode<T> child : node.getChildren()) {
                this.renderNode(columnScope, child, depth + 1, scope);
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
        if (node.isSelected()) return;
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

    /**
     * Returns the invisible root whose children are the top-level rows.
     *
     * @return the retained root, with a null value and no parent.
     */
    public TreeNode<T> getRoot() {
        return this.root;
    }

    /**
     * Sets the function used to turn values into display labels.
     *
     * @param provider the value-to-text function.
     */
    public void setDisplayTextProvider(Function<T, String> provider) {
        this.displayTextProvider = provider;
        this.invalidateComposition();
    }

    /**
     * Replaces the callback for selection made through tree interaction.
     *
     * @param listener the callback receiving the selected node, or null to remove it.
     */
    public void setOnSelectionChanged(Consumer<TreeNode<T>> listener) {
        this.onSelectionChanged = listener;
    }

    /**
     * Sets the height of each row.
     *
     * @param height the positive row height in GUI pixels.
     * @throws IllegalArgumentException if height is not positive.
     */
    public void setItemHeight(int height) {
        if (height <= 0) throw new IllegalArgumentException("Tree row height must be positive");
        this.itemHeight = height;
        this.invalidateComposition();
    }

    /**
     * Returns whether user interaction is enabled.
     *
     * @return true when enabled.
     */
    public boolean isEnabled() {
        return this.enabled.get();
    }

    /**
     * Changes whether user interaction is enabled.
     *
     * @param enabled whether to accept user interaction.
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    /** {@inheritDoc} */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    /**
     * A node owned by a tree, with retained expansion, selection, and hover state.
     * Add and remove children through its methods so the tree is invalidated.
     *
     * @param <T> the node value type.
     */
    public static class TreeNode<T> {
        private final T value;
        private final TreeNode<T> parent;
        private final List<TreeNode<T>> children = Lists.newArrayList();
        private final Runnable invalidate;
        private boolean expanded = true;
        private boolean selected = false;
        private boolean hovered = false;

        TreeNode(T value, TreeNode<T> parent, Runnable invalidate) {
            this.value = value;
            this.parent = parent;
            this.invalidate = invalidate;
        }

        /**
         * Appends an expanded child and invalidates the owning tree.
         *
         * @param value the child value.
         * @return the new child.
         */
        public TreeNode<T> addChild(T value) {
            TreeNode<T> child = new TreeNode<>(value, this, this.invalidate);
            this.children.add(child);
            this.invalidate.run();
            return child;
        }

        /**
         * Appends a child and configures it before returning.
         *
         * @param value the child value.
         * @param configurator the callback receiving the new child.
         * @return the configured child.
         */
        public TreeNode<T> addChild(T value, Consumer<TreeNode<T>> configurator) {
            TreeNode<T> child = this.addChild(value);
            configurator.accept(child);
            return child;
        }

        /**
         * Removes a direct child if present and invalidates the owning tree.
         *
         * @param child the child to remove.
         */
        public void removeChild(TreeNode<T> child) {
            if (this.children.remove(child)) this.invalidate.run();
        }

        /**
         * Returns this node's value.
         *
         * @return the node value; the invisible root has a null value.
         */
        public T getValue() {
            return this.value;
        }

        /**
         * Returns this node's parent.
         *
         * @return the parent, or null for the invisible root.
         */
        public TreeNode<T> getParent() {
            return this.parent;
        }

        /**
         * Returns an unmodifiable live view of this node's children.
         *
         * @return the children in display order.
         */
        public List<TreeNode<T>> getChildren() {
            return Collections.unmodifiableList(this.children);
        }

        /**
         * Returns whether this node's descendants are expanded.
         *
         * @return true when expanded.
         */
        public boolean isExpanded() {
            return this.expanded;
        }

        /**
         * Changes expansion and invalidates the tree when needed.
         *
         * @param expanded whether to expand descendants.
         */
        public void setExpanded(boolean expanded) {
            if (this.expanded == expanded) return;
            this.expanded = expanded;
            this.invalidate.run();
        }

        /**
         * Returns this node's selection flag.
         *
         * @return true when marked selected.
         */
        public boolean isSelected() {
            return this.selected;
        }

        /**
         * Sets only this node's selection flag; it neither deselects other nodes nor invokes the tree callback.
         *
         * @param selected the new flag.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        /**
         * Returns this node's hover flag.
         *
         * @return true when marked hovered.
         */
        public boolean isHovered() {
            return this.hovered;
        }

        /**
         * Sets this node's hover flag.
         *
         * @param hovered the new flag.
         */
        public void setHovered(boolean hovered) {
            this.hovered = hovered;
        }
    }
}
