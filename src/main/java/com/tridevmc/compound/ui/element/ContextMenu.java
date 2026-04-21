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

/**
 * A right-click context menu component for displaying action options.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new ContextMenu(), menu -> {
 *     menu.getElement().addItem("Copy", () -> copy());
 *     menu.getElement().addItem("Paste", () -> paste());
 *     menu.getElement().addItem("Delete", () -> delete());
 * });
 * </pre>
 */
public class ContextMenu extends BaseElement implements IComposableElement {

    private static final int ITEM_HEIGHT = 16;
    private static final int MIN_WIDTH = 120;
    private static final int BACKGROUND_COLOR = 0xFF303030;
    private static final int BORDER_COLOR = 0xFF505050;
    private static final int HOVER_COLOR = 0xFF3366CC;

    private final State<Boolean> visible = new StateImpl<>(false);
    private final State<Integer> hoverIndex = new StateImpl<>(-1);
    private final List<MenuItem> items = Lists.newArrayList();
    private int x;
    private int y;

    public ContextMenu() {
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.visible);
        scope.bind(this.hoverIndex);

        if (!this.visible.get()) return;

        scope.e(new Stack(), menuStack -> {
            int menuWidth = this.calculateWidth();
            int menuHeight = this.items.size() * ITEM_HEIGHT;

            menuStack.layout()
                    .fixedSize(menuWidth, menuHeight)
                    .margin(this.x, this.y, 0, 0);

            menuStack.e(new Rect(BACKGROUND_COLOR), bg -> bg.layout().fillMax());
            menuStack.e(new Rect(BORDER_COLOR), border -> border.layout().margin(-1).fillMax());

            for (int i = 0; i < this.items.size(); i++) {
                final int index = i;
                MenuItem item = this.items.get(i);
                boolean isHovered = this.hoverIndex.get() == index;

                menuStack.e(new Stack(), itemStack -> {
                    itemStack.layout()
                            .fixedHeight(ITEM_HEIGHT)
                            .margin(0, index * ITEM_HEIGHT, 0, 0);

                    if (isHovered) {
                        itemStack.e(new Rect(HOVER_COLOR), hover -> hover.layout().fillMax());
                    }

                    itemStack.e(new Label(
                            item.label,
                            () -> item.enabled ? (isHovered ? 0xFFFFFFFF : 0xFFFFFF) : 0x808080,
                            () -> false
                    ), label -> label.layout().contentAlignment(Alignment.CENTER_LEFT).padding(4, 0));
                });

                scope.onClick(event -> {
                    if (!item.enabled) return false;
                    this.hide();
                    item.action.run();
                    return true;
                });

                scope.onMouseMove(event -> {
                    this.hoverIndex.set(index);
                    return false;
                });
            }
        });

        scope.onClick(event -> {
            int menuWidth = this.calculateWidth();
            int menuHeight = this.items.size() * ITEM_HEIGHT;

            if (event.x() < this.x || event.x() >= this.x + menuWidth ||
event.y() < this.y || event.y() >= this.y + menuHeight) {
                this.hide();
                return false;
            }
            return true;
        });
    }

    private int calculateWidth() {
        var font = Minecraft.getInstance().font;
        int maxWidth = MIN_WIDTH;
        for (MenuItem item : this.items) {
            maxWidth = Math.max(maxWidth, font.width(item.label) + 16);
        }
        return maxWidth;
    }

    public void show(int x, int y) {
        this.x = x;
        this.y = y;
        this.hoverIndex.set(-1);
        this.visible.set(true);
    }

    public void hide() {
        this.visible.set(false);
    }

    public boolean isVisible() {
        return this.visible.get();
    }

    public void addItem(String label, Runnable action) {
        this.items.add(new MenuItem(Component.literal(label), action, true));
    }

    public void addItem(Component label, Runnable action) {
        this.items.add(new MenuItem(label, action, true));
    }

    public void addSeparator() {
        this.items.add(new MenuItem(Component.literal(""), () -> {}, false));
    }

    public void clearItems() {
        this.items.clear();
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(0, 0);
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    private static class MenuItem {
        Component label;
        Runnable action;
        boolean enabled;

        MenuItem(Component label, Runnable action, boolean enabled) {
            this.label = label;
            this.action = action;
            this.enabled = enabled;
        }
    }
}
