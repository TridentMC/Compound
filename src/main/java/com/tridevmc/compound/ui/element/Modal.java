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

import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * A modal dialog overlay for displaying important information or requiring user confirmation.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new Modal(Component.literal("Confirm Delete")), modal -> {
 *     modal.getElement().setMessage(Component.literal("Are you sure you want to delete this file?"));
 *     modal.getElement().addButton("Cancel", () -> modal.getElement().close());
 *     modal.getElement().addButton("Delete", () -> {
 *         performDelete();
 *         modal.getElement().close();
 *     });
 * });
 * </pre>
 */
public class Modal extends BaseElement implements IComposableElement {

    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 150;

    private final State<Boolean> visible = new StateImpl<>(true);
    private Component title;
    private Component message;
    private final List<ModalButton> buttons = new java.util.ArrayList<>();
    private Consumer<Void> onClose;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;

    public Modal(Component title) {
        this.title = title;
        this.invalidate();
    }

    public Modal(Component title, Component message) {
        this.title = title;
        this.invalidate();
        this.message = message;
        this.invalidate();
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.visible);

        if (!this.visible.get()) return;
        var tree = scope.getTree();
        this.getNode().getLayoutProperties().layer(200).fillMax();
        tree.setInputRoot(this.getNode());
        scope.onClick(event -> true);
        scope.onScroll(event -> true);
        scope.onKeyPress(event -> {
            if (event.keyCode() == 256) this.close();
            return true;
        });
        scope.onCharTyped(event -> true);
        scope.e(new Stack(), overlay -> {
            overlay.layout().fillMax().contentAlignment(Alignment.CENTER);
            overlay.e(new Rect(0x90000000), background -> background.layout().fillMax());
            overlay.e(new Panel(), panel -> {
                var viewport = tree.getViewportSize();
                int panelWidth = Math.min(this.width, Math.max(80, viewport.width() - 16));
                int panelHeight = Math.min(this.height, Math.max(60, viewport.height() - 16));
                panel.layout().fixedSize(panelWidth, panelHeight);
                panel.fillSlot(Panel.CONTENT_SLOT, content -> content.e(new Box(), padding -> {
                    padding.layout().fillMax().padding(12);
                    padding.e(new Column(), column -> {
                        column.layout().fillMax().spacing(8);
                        if (this.title != null) column.e(new Label(this.title, 0xFF404040, false));
                        scope.slotInto(CONTENT_SLOT, body -> {
                            if (this.message != null) body.e(new Label(this.message, 0xFF404040, false).setWrap(true));
                        }, column);
                        column.e(new Spacer(), spacer -> spacer.layout().weight(1));
                        column.e(new Row(), row -> {
                            row.layout().fillMaxWidth().fixedHeight(20).spacing(6);
                            for (var button : this.buttons) {
                                row.e(new Button(), action -> {
                                    action.layout().weight(1).fixedHeight(20);
                                    action.fillSlot(Button.CONTENT_SLOT, label -> label.e(new Label(button.label)));
                                    action.getElement().addPressListener((x, y) -> button.action.run());
                                });
                            }
                        });
                    });
                }));
            });
        });
    }


    public void show() {
        this.visible.set(true);
    }

    public void close() {
        if (this.getNode() != null) this.getNode().getTree().clearInputRoot(this.getNode());
        this.visible.set(false);
        if (this.onClose != null) {
            this.onClose.accept(null);
        }
    }

    public boolean isVisible() {
        return this.visible.get();
    }

    public void setTitle(Component title) {
        this.title = title;
        this.invalidate();
    }

    public void setMessage(Component message) {
        this.message = message;
        this.invalidate();
    }

    public void addButton(String label, Runnable action) {
        this.addButton(Component.literal(label), action);
    }

    public void addButton(Component label, Runnable action) {
        this.buttons.add(new ModalButton(label, action));
        this.invalidate();
    }

    public void clearButtons() {
        this.buttons.clear();
        this.invalidate();
    }

    public void setOnClose(Consumer<Void> onClose) {
        this.onClose = onClose;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        this.invalidate();
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

    private void invalidate() {
        var node = this.getNode();
        if (node != null) node.getTree().requestRecompose(node);
    }

    private record ModalButton(Component label, Runnable action) {}
}
