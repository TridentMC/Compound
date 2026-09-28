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
import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.cursor.UICursor;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;


import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A scrollable, single-selection list with keyboard navigation and optional composed rows.
 * Retain the instance to preserve selection and scroll position across recomposition.
 * Replacing the items resets both. The default rows display each item's string value.
 *
 * @param <T> the item type.
 */
public class ListView<T> extends BaseElement implements IComposableElement {

    private static final int DEFAULT_ITEM_HEIGHT = 16;
    private static final int DEFAULT_MAX_VISIBLE_ITEMS = 10;

    private final State<Integer> selectedIndex = State.of(-1);
    private final State<Integer> hoverIndex = State.of(-1);
    private final State<Boolean> enabled = State.of(true);
    private final List<T> items = Lists.newArrayList();
    private final ScrollArea scrollArea = new ScrollArea();

    private Function<T, String> displayTextProvider = Object::toString;
    private BiConsumer<T, ICompositionScope> rowContent;
    private Consumer<T> onSelectionChanged;
    private int itemHeight = DEFAULT_ITEM_HEIGHT;
    private int maxVisibleItems = DEFAULT_MAX_VISIBLE_ITEMS;

    /**
     * Creates an empty list with 16-pixel rows and a preferred maximum of ten visible rows.
     */
    public ListView() {
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        // Color suppliers update selection and hover without rebuilding every row.

        scope.onKeyPress(event -> {
            if (!this.enabled.get() || this.items.isEmpty()) return false;
            int current = this.selectedIndex.get();
            int index = switch (event.keyCode()) {
                case InputConstants.KEY_UP -> current < 0 ? 0 : Math.max(0, current - 1);
                case InputConstants.KEY_DOWN -> Math.min(this.items.size() - 1, current + 1);
                case InputConstants.KEY_HOME -> 0;
                case InputConstants.KEY_END -> this.items.size() - 1;
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
                                        if (!this.enabled.get() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
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

    /** {@inheritDoc} */
    @Override
    public boolean isFocusable() {
        return this.enabled.get();
    }

    /** {@inheritDoc} */
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

    /**
     * Copies the supplied items, clearing selection, hover, and scroll position.
     *
     * @param items the new items in display order.
     */
    public void setItems(List<T> items) {
        this.items.clear();
        this.items.addAll(items);
        this.selectedIndex.set(-1);
        this.hoverIndex.set(-1);
        this.scrollArea.scrollTo(0, 0);
        this.invalidateComposition();
    }

    /**
     * Appends an item without changing selection.
     *
     * @param item the item to append.
     */
    public void addItem(T item) {
        this.items.add(item);
        this.invalidateComposition();
    }

    /**
     * Removes an item and preserves the selected item where possible. No callback is fired.
     *
     * @param index the zero-based index; out-of-range indices are ignored.
     */
    public void removeItem(int index) {
        if (index >= 0 && index < this.items.size()) {
            this.items.remove(index);
            if (this.selectedIndex.get() == index) {
                this.selectedIndex.set(-1);
            } else if (this.selectedIndex.get() > index) {
                this.selectedIndex.set(this.selectedIndex.get() - 1);
            }
            this.hoverIndex.set(-1);
            this.invalidateComposition();
        }
    }

    /**
     * Returns the selected item.
     *
     * @return the item, or null if there is no selection.
     */
    public T getSelected() {
        int index = this.selectedIndex.get();
        if (index >= 0 && index < this.items.size()) {
            return this.items.get(index);
        }
        return null;
    }

    /**
     * Selects and reveals an item, notifying the callback when the selection changes.
     *
     * @param index the zero-based index; out-of-range indices are ignored.
     */
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
     * Replaces default labels with composed row content. The list still owns row selection.
     *
     * @param rowContent the item and row-scope callback, or null to restore default labels.
     */
    public void setRowContent(BiConsumer<T, ICompositionScope> rowContent) {
        this.rowContent = rowContent;
        this.invalidateComposition();
    }

    /**
     * Replaces the selection-change callback for user and programmatic selection.
     *
     * @param listener the callback, or null to remove it.
     */
    public void setOnSelectionChanged(Consumer<T> listener) {
        this.onSelectionChanged = listener;
    }

    /**
     * Sets the height of each row.
     *
     * @param height the positive row height in GUI pixels.
     * @throws IllegalArgumentException if height is not positive.
     */
    public void setItemHeight(int height) {
        if (height <= 0) throw new IllegalArgumentException("List row height must be positive");
        this.itemHeight = height;
        this.invalidateComposition();
    }

    /**
     * Caps the preferred height by a count of rows unless the list fills its available height.
     *
     * @param max the positive visible row count.
     * @throws IllegalArgumentException if max is not positive.
     */
    public void setMaxVisibleItems(int max) {
        if (max <= 0) throw new IllegalArgumentException("Visible list row count must be positive");
        this.maxVisibleItems = max;
        this.invalidateComposition();
    }

    /**
     * Returns the number of items.
     *
     * @return the item count.
     */
    public int getItemCount() {
        return this.items.size();
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
        int visibleItems = Math.min(this.items.size(), this.maxVisibleItems);
        return new Size(
                constraints.maxWidth(),
                visibleItems * this.itemHeight
        );
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    /** {@inheritDoc} */
    @Override
    public UICursor getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}
