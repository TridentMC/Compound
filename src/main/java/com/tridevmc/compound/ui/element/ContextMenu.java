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
import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;



import javax.annotation.Nonnull;
import java.util.List;

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

    private final State<Boolean> visible = new StateImpl<>(false);
    private final List<MenuItem> items = Lists.newArrayList();
    private int x;
    private int y;

    public ContextMenu() {
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.visible);
        if (!this.visible.get()) return;
        var tree = scope.getTree();
        this.getNode().getLayoutProperties().layer(150).fillMax();
        tree.setInputRoot(this.getNode());
        scope.onClick(event -> { this.hide(); return true; });
        scope.onKeyPress(event -> {
            if (event.keyCode() == InputConstants.KEY_ESCAPE) this.hide();
            return true;
        });
        var viewport = tree.getViewportSize();
        int menuWidth = Math.min(this.calculateWidth(), viewport.width());
        int contentHeight = this.items.stream().mapToInt(item -> item.separator() ? 5 : ITEM_HEIGHT).sum();
        int menuHeight = Math.min(contentHeight + 2, viewport.height());
        scope.e(new Surface(0xFF202020, 0xFFA0A0A0, 1), surface -> {
            surface.layout().fixedSize(menuWidth, menuHeight);
            surface.fillSlot(Surface.CONTENT_SLOT, content -> content.e(new ScrollArea().showScrollbar(contentHeight + 2 > viewport.height()), scroll -> {
                scroll.layout().fillMax().margin(1);
                scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(new Column(), column -> {
                    column.layout().fillMaxWidth();
                    for (var item : this.items) {
                        if (item.separator()) {
                            column.e(new Divider(0xFF808080, 1), divider -> divider.layout().fillMaxWidth().fixedHeight(1).margin(2));
                        } else {
                            column.e(new Button(item.enabled), button -> {
                                button.layout().fillMaxWidth().fixedHeight(ITEM_HEIGHT);
                                button.fillSlot(Button.CONTENT_SLOT, label -> label.e(new Label(item.label)));
                                button.getElement().addPressListener((x, y) -> { this.hide(); item.action.run(); });
                            });
                        }
                    }
                }));
            }));
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
        this.visible.set(true);
        this.invalidateComposition();
    }

    public void hide() {
        if (this.getNode() != null) this.getNode().getTree().clearInputRoot(this.getNode());
        this.visible.set(false);
    }

    public boolean isVisible() {
        return this.visible.get();
    }

    public void addItem(String label, Runnable action) {
        this.addItem(Component.literal(label), action);
    }

    public void addItem(Component label, Runnable action) {
        this.items.add(new MenuItem(label, action, true));
        this.invalidateComposition();
    }

    public void addSeparator() {
        this.items.add(new MenuItem(Component.literal(""), () -> {}, false));
        this.invalidateComposition();
    }

    public void clearItems() {
        this.items.clear();
        this.invalidateComposition();
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
        return this.visible.get() ? new Size(constraints.maxWidth(), constraints.maxHeight()) : new Size(0, 0);
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> children) {
        if (children.isEmpty()) return List.of();
        var viewport = this.getNode().getTree().getViewportSize();
        var size = children.get(0);
        return List.of(new Bounds(Math.clamp(this.x, 0, Math.max(0, viewport.width() - size.width())),
                Math.clamp(this.y, 0, Math.max(0, viewport.height() - size.height())), size.width(), size.height()));
    }

    private record MenuItem(Component label, Runnable action, boolean enabled) {
        boolean separator() {
            return !this.enabled && this.label.getString().isEmpty();
        }
    }
}
