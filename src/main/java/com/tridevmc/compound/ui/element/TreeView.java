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
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.scope.IContainerScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

public class TreeView<T> extends BaseElement implements IComposableElement {

    private static final int DEFAULT_ITEM_HEIGHT = 16;
    private static final int INDENT_SIZE = 16;

    private final State<Boolean> enabled = new StateImpl<>(true);
    private final TreeNode<T> root;
    private final ScrollArea scrollArea = new ScrollArea();
    private Function<T, String> displayTextProvider = Object::toString;
    private Consumer<TreeNode<T>> onSelectionChanged;
    private int itemHeight = DEFAULT_ITEM_HEIGHT;

    public TreeView() {
        this.root = new TreeNode<>(null, null, this::invalidate);
    }

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
            case GLFW.GLFW_KEY_UP -> Math.max(0, selected - 1);
            case GLFW.GLFW_KEY_DOWN -> Math.min(nodes.size() - 1, selected + 1);
            case GLFW.GLFW_KEY_HOME -> 0;
            case GLFW.GLFW_KEY_END -> nodes.size() - 1;
            default -> -1;
        };
        if (target >= 0) {
            this.selectNode(nodes.get(target));
            this.revealRow(target);
            Minecraft.getInstance().getNarrator().saySystemNow(this.getNarrationMessage());
            return true;
        }
        if (key != GLFW.GLFW_KEY_LEFT && key != GLFW.GLFW_KEY_RIGHT) return false;
        TreeNode<T> node = nodes.get(Math.max(0, selected));
        this.selectNode(node);
        if (key == GLFW.GLFW_KEY_LEFT) {
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

    @Override
    public boolean isFocusable() {
        return this.enabled.get();
    }

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

                // Hover handlers on the item scope, not the column scope
                itemStack.onMouseEnter(() -> currentNode.setHovered(true));
                itemStack.onMouseExit(() -> currentNode.setHovered(false));

                // Click handler on the item scope
                itemStack.onClick(event -> {
                    if (!this.enabled.get() || event.button() != 0) return false;
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

    public TreeNode<T> getRoot() {
        return this.root;
    }

    public void setDisplayTextProvider(Function<T, String> provider) {
        this.displayTextProvider = provider;
        this.invalidate();
    }

    public void setOnSelectionChanged(Consumer<TreeNode<T>> listener) {
        this.onSelectionChanged = listener;
    }

    public void setItemHeight(int height) {
        if (height <= 0) throw new IllegalArgumentException("Tree row height must be positive");
        this.itemHeight = height;
        this.invalidate();
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    private void invalidate() {
        var node = this.getNode();
        if (node != null) node.getTree().requestRecompose(node);
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

        public TreeNode<T> addChild(T value) {
            TreeNode<T> child = new TreeNode<>(value, this, this.invalidate);
            this.children.add(child);
            this.invalidate.run();
            return child;
        }

        public TreeNode<T> addChild(T value, Consumer<TreeNode<T>> configurator) {
            TreeNode<T> child = this.addChild(value);
            configurator.accept(child);
            return child;
        }

        public void removeChild(TreeNode<T> child) {
            if (this.children.remove(child)) this.invalidate.run();
        }

        public T getValue() {
            return this.value;
        }

        public TreeNode<T> getParent() {
            return this.parent;
        }

        public List<TreeNode<T>> getChildren() {
            return Collections.unmodifiableList(this.children);
        }

        public boolean isExpanded() {
            return this.expanded;
        }

        public void setExpanded(boolean expanded) {
            if (this.expanded == expanded) return;
            this.expanded = expanded;
            this.invalidate.run();
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
