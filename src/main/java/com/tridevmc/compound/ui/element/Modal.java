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

import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.network.chat.Component;



import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * An initially visible modal dialog with a title, scrolling body, and fixed action row.
 * Fill {@link #CONTENT_SLOT} to replace the fallback message with arbitrary content.
 * While visible, the dialog captures input; Escape closes it, while action buttons
 * only run their supplied actions. Retain the instance to call {@link #show()} again.
 */
public class Modal extends Element implements IComposableElement {

    /**
     * The scrolling body content; replaces the fallback message when supplied.
     */
    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 150;

    private final State<Boolean> visible = State.of(true);
    private Component title;
    private Component message;
    private final List<ModalButton> buttons = new java.util.ArrayList<>();
    private Consumer<Void> onClose;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;

    /**
     * Creates a visible dialog with an empty body and no actions.
     *
     * @param title the title, or null to omit it.
     */
    public Modal(Component title) {
        this.title = title;
        this.invalidateComposition();
    }

    /**
     * Creates a visible dialog with a fallback message and no actions.
     *
     * @param title the title, or null to omit it.
     * @param message the body message, or null for none.
     */
    public Modal(Component title, Component message) {
        this.title = title;
        this.invalidateComposition();
        this.message = message;
        this.invalidateComposition();
    }

    /** {@inheritDoc} */
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
            if (event.keyCode() == InputConstants.KEY_ESCAPE) this.close();
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
                        column.e(new ScrollArea(), scroll -> {
                            scroll.layout().fillMaxWidth().weight(1);
                            scroll.fillSlot(ScrollArea.CONTENT_SLOT, body -> body.e(new Column(), bodyColumn -> {
                                bodyColumn.layout().fillMaxWidth().spacing(8);
                                scope.slotInto(CONTENT_SLOT, fallback -> {
                                    if (this.message != null) fallback.e(new Label(this.message, 0xFF404040, false).setWrap(true));
                                }, bodyColumn);
                            }));
                        });
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


    /**
     * Shows the dialog; its next composition captures input.
     */
    public void show() {
        this.visible.set(true);
    }

    /**
     * Hides the dialog, releases input capture, and invokes the close callback.
     * The callback is invoked even if the dialog was already hidden.
     */
    public void close() {
        if (this.getNode() != null) this.getNode().getTree().clearInputRoot(this.getNode());
        this.visible.set(false);
        if (this.onClose != null) {
            this.onClose.accept(null);
        }
    }

    /**
     * Returns whether the dialog is shown.
     *
     * @return true when visible.
     */
    public boolean isVisible() {
        return this.visible.get();
    }

    /**
     * Replaces the dialog title.
     *
     * @param title the title, or null to omit it.
     */
    public void setTitle(Component title) {
        this.title = title;
        this.invalidateComposition();
    }

    /**
     * Replaces the fallback body message; a filled content slot takes precedence.
     *
     * @param message the message, or null for none.
     */
    public void setMessage(Component message) {
        this.message = message;
        this.invalidateComposition();
    }

    /**
     * Appends an action button. The action must call {@link #close()} if it should dismiss the dialog.
     *
     * @param label the button label.
     * @param action the action to run.
     */
    public void addButton(String label, Runnable action) {
        this.addButton(Component.literal(label), action);
    }

    /**
     * Appends an action button. The action must call {@link #close()} if it should dismiss the dialog.
     *
     * @param label the button label.
     * @param action the action to run.
     */
    public void addButton(Component label, Runnable action) {
        this.buttons.add(new ModalButton(label, action));
        this.invalidateComposition();
    }

    /**
     * Removes all action buttons.
     */
    public void clearButtons() {
        this.buttons.clear();
        this.invalidateComposition();
    }

    /**
     * Replaces the callback invoked by {@link #close()}.
     *
     * @param onClose the callback, invoked with null, or null to remove it.
     */
    public void setOnClose(Consumer<Void> onClose) {
        this.onClose = onClose;
    }

    /**
     * Sets the requested panel size; composition limits it to the available viewport.
     *
     * @param width the requested width in GUI pixels.
     * @param height the requested height in GUI pixels.
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        this.invalidateComposition();
    }

    /** {@inheritDoc} */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(0, 0);
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    private record ModalButton(Component label, Runnable action) {}
}
