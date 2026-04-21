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

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 150;
    private static final int BACKGROUND_COLOR = 0xF0000000;
    private static final int PANEL_COLOR = 0xFF303030;
    private static final int BORDER_COLOR = 0xFF505050;

    private final State<Boolean> visible = new StateImpl<>(true);
    private Component title;
    private Component message;
    private final List<ModalButton> buttons = new java.util.ArrayList<>();
    private Consumer<Void> onClose;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;

    public Modal(Component title) {
        this.title = title;
    }

    public Modal(Component title, Component message) {
        this.title = title;
        this.message = message;
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.visible);

        if (!this.visible.get()) return;

        scope.e(new Stack(), overlayStack -> {
            overlayStack.layout().fillMax();

            overlayStack.e(new Rect(BACKGROUND_COLOR), bg -> bg.layout().fillMax());

            overlayStack.e(new Stack(), modalStack -> {
                modalStack.layout().fixedSize(this.width, this.height).contentAlignment(Alignment.CENTER);

                modalStack.e(new Rect(PANEL_COLOR), panel -> panel.layout().fillMax());
                modalStack.e(new Rect(BORDER_COLOR), border -> border.layout().margin(-1).fillMax());

                modalStack.e(new Box(), contentBox -> {
                    contentBox.layout().padding(16).fillMax();

                    contentBox.e(new Column(), column -> {
                        column.layout().fillMax().spacing(8);

                        if (this.title != null) {
                            column.e(new Label(this.title, 0xFFFFFFFF, false),
                                    title -> title.layout().contentAlignment(Alignment.CENTER));
                        }

                        if (this.message != null) {
                            column.e(new Label(this.message, 0xFFCCCCCC, false),
                                    msg -> msg.layout().contentAlignment(Alignment.CENTER).fillMax());
                        }

                        if (!this.buttons.isEmpty()) {
                            column.e(new Row(), buttonRow -> {
                                buttonRow.layout().spacing(8).contentAlignment(Alignment.CENTER);

                                for (ModalButton button : this.buttons) {
                                    buttonRow.e(new Button(), btn -> {
                                        btn.layout().fixedHeight(20);
                                        btn.fillSlot(Button.CONTENT_SLOT, btnContent -> {
                                            btnContent.e(new Label(button.label, 0xFFFFFF, false));
                                        });
                                        btn.getElement().addPressListener((x, y) -> button.action.run());
                                    });
                                }
                            });
                        }
                    });
                });
            });
        });
    }

    public void show() {
        this.visible.set(true);
    }

    public void close() {
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
    }

    public void setMessage(Component message) {
        this.message = message;
    }

    public void addButton(String label, Runnable action) {
        this.buttons.add(new ModalButton(Component.literal(label), action));
    }

    public void addButton(Component label, Runnable action) {
        this.buttons.add(new ModalButton(label, action));
    }

    public void clearButtons() {
        this.buttons.clear();
    }

    public void setOnClose(Consumer<Void> onClose) {
        this.onClose = onClose;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
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

    private static class ModalButton {
        Component label;
        Runnable action;

        ModalButton(Component label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }
}
