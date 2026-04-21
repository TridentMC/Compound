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
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.tridevmc.compound.ui.CompoundCursors;
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
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

    private Function<T, String> displayTextProvider = Object::toString;
    private Consumer<T> onSelectionChanged;
    private int itemHeight = DEFAULT_ITEM_HEIGHT;
    private int maxVisibleItems = DEFAULT_MAX_VISIBLE_ITEMS;

    public ListView() {
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.enabled);
        scope.bind(this.selectedIndex);
        scope.bind(this.hoverIndex);

        scope.e(new ScrollArea(), scrollArea -> {
            scrollArea.layout().fillMax();

            scrollArea.fillSlot(ScrollArea.CONTENT_SLOT, content -> {
                content.e(new Column(), column -> {
                    column.layout().fillMaxWidth();

                    for (int i = 0; i < this.items.size(); i++) {
                        final int index = i;
                        T item = this.items.get(i);
                        boolean isSelected = this.selectedIndex.get() == index;
                        boolean isHovered = this.hoverIndex.get() == index;

                        column.e(new Stack(), itemStack -> {
                            itemStack.layout().fixedHeight(this.itemHeight).fillMaxWidth();

                            if (isSelected) {
                                itemStack.e(new Rect(0xFF3366CC), bg -> bg.layout().fillMax());
                            } else if (isHovered) {
                                itemStack.e(new Rect(0xFF224488), bg -> bg.layout().fillMax());
                            }

                            itemStack.e(new Label(
                                    Component.literal(this.displayTextProvider.apply(item)),
                                    () -> isSelected || isHovered ? 0xFFFFFFFF : 0xFFFFFF,
                                    () -> false
                            ), label -> label.layout().contentAlignment(Alignment.CENTER_LEFT).padding(4, 0));
                        });

                        scope.onClick(event -> {
                            if (!this.enabled.get()) return false;
                            this.select(index);
                            return true;
                        });

                        scope.onMouseMove(event -> {
                            this.hoverIndex.set(index);
                            return false;
                        });
                    }
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
        }
    }

    public void setItems(List<T> items) {
        this.items.clear();
        this.items.addAll(items);
        this.selectedIndex.set(-1);
    }

    public void addItem(T item) {
        this.items.add(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < this.items.size()) {
            this.items.remove(index);
            if (this.selectedIndex.get() == index) {
                this.selectedIndex.set(-1);
            } else if (this.selectedIndex.get() > index) {
                this.selectedIndex.set(this.selectedIndex.get() - 1);
            }
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
        this.select(index);
    }

    public void setDisplayTextProvider(Function<T, String> provider) {
        this.displayTextProvider = provider;
    }

    public void setOnSelectionChanged(Consumer<T> listener) {
        this.onSelectionChanged = listener;
    }

    public void setItemHeight(int height) {
        this.itemHeight = height;
    }

    public void setMaxVisibleItems(int max) {
        this.maxVisibleItems = max;
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
    public CursorType getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}
