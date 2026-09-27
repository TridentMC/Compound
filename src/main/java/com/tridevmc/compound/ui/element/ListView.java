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
import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A scrollable list of items with selection support.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new ListView<>(), list -> {
 *     list.getElement().setItems(List.of("Item 1", "Item 2", "Item 3"));
 *     list.getElement().setOnSelectionChanged(item -> {
 *         System.out.println("Selected: " + item);
 *     });
 * });
 * </pre>
 */
public class ListView<T> extends BaseElement implements IComposableElement {

    private static final int DEFAULT_ITEM_HEIGHT = 16;
    private static final int DEFAULT_MAX_VISIBLE_ITEMS = 10;

    private final State<Integer> selectedIndex = new StateImpl<>(-1);
    private final State<Integer> hoverIndex = new StateImpl<>(-1);
    private final State<Boolean> enabled = new StateImpl<>(true);
    private final List<T> items = Lists.newArrayList();
    private final ScrollArea scrollArea = new ScrollArea();

    private Function<T, String> displayTextProvider = Object::toString;
    private BiConsumer<T, ICompositionScope> rowContent;
    private Consumer<T> onSelectionChanged;
    private int itemHeight = DEFAULT_ITEM_HEIGHT;
    private int maxVisibleItems = DEFAULT_MAX_VISIBLE_ITEMS;

    public ListView() {
    }

    @Override
    public void compose(ICompositionScope scope) {
        // NOTE: We do NOT bind selectedIndex/hoverIndex here.
        // Selection and hover visuals are rendered dynamically via color suppliers
        // that are evaluated every frame. This avoids expensive full recomposition
        // of all list items on every click or mouse movement.

        scope.onKeyPress(event -> {
            if (!this.enabled.get() || this.items.isEmpty()) return false;
            int current = this.selectedIndex.get();
            int index = switch (event.keyCode()) {
                case GLFW.GLFW_KEY_UP -> current < 0 ? 0 : Math.max(0, current - 1);
                case GLFW.GLFW_KEY_DOWN -> Math.min(this.items.size() - 1, current + 1);
                case GLFW.GLFW_KEY_HOME -> 0;
                case GLFW.GLFW_KEY_END -> this.items.size() - 1;
                default -> -1;
            };
            if (index < 0) return false;
            this.select(index);
            this.revealSelection();
            return true;
        });

        scope.e(new Surface(0xFF181818), background -> {
            background.layout().fillMax();
            background.fillSlot(Surface.CONTENT_SLOT, listContent -> {
                listContent.e(this.scrollArea, scrollArea -> {
                    scrollArea.layout().fillMax();
                    if (!this.getNode().getLayoutProperties().isFillMaxHeight()) {
                        scrollArea.layout().maxHeight(this.maxVisibleItems * this.itemHeight);
                    }

                    scrollArea.fillSlot(ScrollArea.CONTENT_SLOT, content -> {
                        content.e(new Column(), column -> {
                            column.layout().fillMaxWidth();

                            for (int i = 0; i < this.items.size(); i++) {
                                final int index = i;
                                T item = this.items.get(i);

                                column.e(new Stack(), row -> {
                                    row.layout().fixedHeight(this.itemHeight).fillMaxWidth();
                                    row.onClick(event -> {
                                        if (!this.enabled.get() || event.button() != 0) return false;
                                        scope.requestFocus();
                                        this.select(index);
                                        return true;
                                    });
                                    row.onMouseEnter(() -> this.hoverIndex.set(index));
                                    row.onMouseExit(() -> {
                                        if (this.hoverIndex.get() == index) this.hoverIndex.set(-1);
                                    });
                                    row.e(new Surface(
                                            () -> this.hoverIndex.get() == index ? 0xFF303030 : 0x00000000,
                                            () -> this.selectedIndex.get() == index ? 0xFFFFFFFF : 0x00000000,
                                            1), surface -> {
                                        surface.layout().fillMax();
                                        surface.fillSlot(Surface.CONTENT_SLOT, cell -> {
                                            if (this.rowContent != null) {
                                                this.rowContent.accept(item, cell);
                                            } else {
                                                cell.e(new Box(), box -> {
                                                    box.layout().fillMax().padding(4, 0).contentAlignment(Alignment.CENTER_LEFT);
                                                    box.e(new Label(Component.literal(this.displayTextProvider.apply(item))));
                                                });
                                            }
                                        });
                                    });
                                });
                            }
                        });
                    });
                });
            });
        });
    }

    private void select(int index) {
        if (index < 0 || index >= this.items.size()) return;

        int oldIndex = this.selectedIndex.get();
        if (oldIndex != index) {
            this.selectedIndex.set(index);

            SoundManager soundManager = Minecraft.getInstance().getSoundManager();
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            if (this.onSelectionChanged != null) {
                this.onSelectionChanged.accept(this.items.get(index));
            }
            Minecraft.getInstance().getNarrator().saySystemNow(this.getNarrationMessage());
        }
    }

    @Override
    public boolean isFocusable() {
        return this.enabled.get();
    }

    @Override
    public Component getNarrationMessage() {
        int index = this.selectedIndex.get();
        return Component.literal(index >= 0 && index < this.items.size()
                ? this.displayTextProvider.apply(this.items.get(index)) : "List");
    }

    private void revealSelection() {
        int index = this.selectedIndex.get();
        if (index < 0) return;
        int top = index * this.itemHeight;
        int bottom = top + this.itemHeight;
        int scroll = this.scrollArea.getScrollYState().get();
        int height = this.scrollArea.getBounds().height();
        if (height <= 0) return;
        if (top < scroll) scroll = top;
        else if (bottom > scroll + height) scroll = bottom - height;
        this.scrollArea.scrollTo(0, Math.clamp(scroll, 0, this.scrollArea.getMaxScrollY()));
    }

    public void setItems(List<T> items) {
        this.items.clear();
        this.items.addAll(items);
        this.selectedIndex.set(-1);
        this.hoverIndex.set(-1);
        this.scrollArea.scrollTo(0, 0);
        this.invalidate();
    }

    public void addItem(T item) {
        this.items.add(item);
        this.invalidate();
    }

    public void removeItem(int index) {
        if (index >= 0 && index < this.items.size()) {
            this.items.remove(index);
            if (this.selectedIndex.get() == index) {
                this.selectedIndex.set(-1);
            } else if (this.selectedIndex.get() > index) {
                this.selectedIndex.set(this.selectedIndex.get() - 1);
            }
            this.hoverIndex.set(-1);
            this.invalidate();
        }
    }

    public T getSelected() {
        int index = this.selectedIndex.get();
        if (index >= 0 && index < this.items.size()) {
            return this.items.get(index);
        }
        return null;
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= this.items.size()) return;
        if (this.selectedIndex.get() != index) {
            this.selectedIndex.set(index);
            if (this.onSelectionChanged != null) {
                this.onSelectionChanged.accept(this.items.get(index));
            }
        }
        this.revealSelection();
    }

    public void setDisplayTextProvider(Function<T, String> provider) {
        this.displayTextProvider = provider;
        this.invalidate();
    }

    public void setRowContent(BiConsumer<T, ICompositionScope> rowContent) {
        this.rowContent = rowContent;
        this.invalidate();
    }

    public void setOnSelectionChanged(Consumer<T> listener) {
        this.onSelectionChanged = listener;
    }

    public void setItemHeight(int height) {
        if (height <= 0) throw new IllegalArgumentException("List row height must be positive");
        this.itemHeight = height;
        this.invalidate();
    }

    public void setMaxVisibleItems(int max) {
        if (max <= 0) throw new IllegalArgumentException("Visible list row count must be positive");
        this.maxVisibleItems = max;
        this.invalidate();
    }

    public int getItemCount() {
        return this.items.size();
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
        int visibleItems = Math.min(this.items.size(), this.maxVisibleItems);
        return new Size(
                constraints.maxWidth(),
                visibleItems * this.itemHeight
        );
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    @Override
    public UICursor getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}
