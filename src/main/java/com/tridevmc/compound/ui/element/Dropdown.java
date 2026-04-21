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
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A dropdown menu component for selecting a single option from a list of choices.
 * Essential for configuration UIs and forms.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * var options = List.of("Peaceful", "Easy", "Normal", "Hard");
 * scope.e(new Dropdown<>(options, "Normal"), dropdown -> {
 *     dropdown.getElement().setOnSelectionChanged((old, newVal) -> {
 *         setDifficulty(newVal);
 *     });
 * });
 * </pre>
 */
public class Dropdown<T> extends BaseElement implements IComposableElement {

    private static final int DEFAULT_HEIGHT = 20;
    private static final int OPTION_HEIGHT = 16;
    private static final int MAX_VISIBLE_ITEMS = 8;

    private static final IScreenSprite BUTTON_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/button"));
    private static final IScreenSprite BUTTON_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/button_highlighted"));
    private static final IScreenSprite BUTTON_DISABLED_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/button_disabled"));

    private final State<Boolean> enabled = new StateImpl<>(true);
    private final State<Boolean> hovered = new StateImpl<>(false);
    private final State<Boolean> open = new StateImpl<>(false);
    private final State<Integer> selectedIndex = new StateImpl<>(-1);
    private final State<Integer> hoverIndex = new StateImpl<>(-1);

    private final List<T> options;
    private Function<T, String> displayTextProvider = Object::toString;
    private Consumer<T> onSelectionChanged;
    private Consumer<Void> onOpen;
    private Consumer<Void> onClose;
    private int maxVisibleItems = MAX_VISIBLE_ITEMS;
    private Component placeholder;

    public Dropdown(List<T> options) {
        this.options = Lists.newArrayList(options);
    }

    public Dropdown(List<T> options, T selected) {
        this(options);
        int index = this.options.indexOf(selected);
        if (index >= 0) {
            this.selectedIndex.set(index);
        }
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.enabled);
        scope.bind(this.hovered);
        scope.bind(this.open);
        scope.bind(this.selectedIndex);
        scope.bind(this.hoverIndex);

        scope.onMouseEnter(() -> this.hovered.set(true));
        scope.onMouseExit(() -> this.hovered.set(false));

        scope.onClick(event -> {
            if (!this.enabled.get()) return false;
            this.toggleOpen();
            return true;
        });

        if (this.open.get()) {
            scope.onClick(event -> {
                Bounds bounds = this.getBounds();
                if (bounds == null) return false;

                int dropdownTop = bounds.y() + bounds.height();
                int dropdownHeight = Math.min(this.options.size(), this.maxVisibleItems) * OPTION_HEIGHT;

                int relativeY = event.y() - dropdownTop;
                if (relativeY >= 0 && relativeY < dropdownHeight &&
event.x() >= bounds.x() && event.x() < bounds.x() + bounds.width()) {
                    int index = relativeY / OPTION_HEIGHT;
                    if (index >= 0 && index < this.options.size()) {
                        this.select(index);
                        return true;
                    }
                }

                this.setOpen(false);
                return false;
            });

            scope.onMouseMove(event -> {
                Bounds bounds = this.getBounds();
                if (bounds == null) return false;

                int dropdownTop = bounds.y() + bounds.height();
                int dropdownHeight = Math.min(this.options.size(), this.maxVisibleItems) * OPTION_HEIGHT;

                int relativeY = event.y() - dropdownTop;
                if (relativeY >= 0 && relativeY < dropdownHeight &&
event.x() >= bounds.x() && event.x() < bounds.x() + bounds.width()) {
                    int index = relativeY / OPTION_HEIGHT;
                    this.hoverIndex.set(index);
                } else {
                    this.hoverIndex.set(-1);
                }
                return false;
            });
        }

        scope.onKeyPress(event -> {
            if (!this.enabled.get()) return false;

            if (this.open.get()) {
                return switch (event.keyCode()) {
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_UP -> {
                        int newIndex = this.hoverIndex.get() - 1;
                        if (newIndex < 0) newIndex = this.options.size() - 1;
                        this.hoverIndex.set(newIndex);
                        yield true;
                    }
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN -> {
                        int newIndex = this.hoverIndex.get() + 1;
                        if (newIndex >= this.options.size()) newIndex = 0;
                        this.hoverIndex.set(newIndex);
                        yield true;
                    }
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER -> {
                        int index = this.hoverIndex.get();
                        if (index >= 0 && index < this.options.size()) {
                            this.select(index);
                        }
                        yield true;
                    }
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE -> {
                        int index = this.hoverIndex.get();
                        if (index >= 0 && index < this.options.size()) {
                            this.select(index);
                        }
                        yield true;
                    }
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE -> {
                        this.setOpen(false);
                        yield true;
                    }
                    default -> false;
                };
            } else {
                if (event.keyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER ||
                        event.keyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) {
                    this.setOpen(true);
                    return true;
                }
            }
            return false;
        });

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            IScreenSprite bgSprite;
            if (!this.enabled.get()) {
                bgSprite = BUTTON_DISABLED_SPRITE;
            } else if (this.hovered.get() || this.open.get()) {
                bgSprite = BUTTON_HIGHLIGHTED_SPRITE;
            } else {
                bgSprite = BUTTON_SPRITE;
            }

            stack.e(new Sprite(bgSprite), s -> s.layout().fillMax());

            stack.e(new Label(
                    () -> {
                        int index = this.selectedIndex.get();
                        if (index >= 0 && index < this.options.size()) {
                            return Component.literal(this.displayTextProvider.apply(this.options.get(index)));
                        }
                        return this.placeholder != null ? this.placeholder : Component.literal("Select...");
                    },
                    () -> this.enabled.get() ? 0xFFFFFF : 0x808080,
                    () -> false
            ), label -> label.layout().contentAlignment(Alignment.CENTER_LEFT).padding(4, 0));
        });

        if (this.open.get()) {
            this.renderDropdownList(scope);
        }
    }

    private void renderDropdownList(ICompositionScope scope) {
        Bounds bounds = this.getBounds();
        if (bounds == null) return;

        int visibleCount = Math.min(this.options.size(), this.maxVisibleItems);
        int dropdownHeight = visibleCount * OPTION_HEIGHT;

        scope.e(new Stack(), dropdownStack -> {
            dropdownStack.layout()
                    .fixedSize(bounds.width(), dropdownHeight)
                    .margin(0, bounds.height(), 0, 0);

            dropdownStack.e(new Rect(0xFF000000), bg -> bg.layout().fillMax());
            dropdownStack.e(new Rect(0xFF404040), border -> border.layout().fillMax());

            for (int i = 0; i < visibleCount; i++) {
                final int index = i;
                T option = this.options.get(i);
                boolean isHovered = this.hoverIndex.get() == index;
                boolean isSelected = this.selectedIndex.get() == index;

                dropdownStack.e(new Stack(), optionStack -> {
                    optionStack.layout()
                            .fixedHeight(OPTION_HEIGHT)
                            .margin(0, index * OPTION_HEIGHT, 0, 0);

                    if (isHovered) {
                        optionStack.e(new Rect(0xFF3366CC), r -> r.layout().fillMax());
                    } else if (isSelected) {
                        optionStack.e(new Rect(0xFF224488), r -> r.layout().fillMax());
                    }

                    optionStack.e(new Label(
                            Component.literal(this.displayTextProvider.apply(option)),
                            () -> isHovered || isSelected ? 0xFFFFFFFF : 0xFFFFFF,
                            () -> false
                    ), label -> label.layout().contentAlignment(Alignment.CENTER_LEFT).padding(4, 0));
                });
            }
        });
    }

    private void toggleOpen() {
        this.setOpen(!this.open.get());
    }

    private void setOpen(boolean open) {
        boolean wasOpen = this.open.get();
        this.open.set(open);

        if (open && !wasOpen) {
            this.hoverIndex.set(this.selectedIndex.get());
            if (this.onOpen != null) {
                this.onOpen.accept(null);
            }
        } else if (!open && wasOpen) {
            if (this.onClose != null) {
                this.onClose.accept(null);
            }
        }
    }

    private void select(int index) {
        if (index < 0 || index >= this.options.size()) return;

        T oldValue = this.selectedIndex.get() >= 0 ? this.options.get(this.selectedIndex.get()) : null;
        T newValue = this.options.get(index);

        this.selectedIndex.set(index);
        this.setOpen(false);

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        if (this.onSelectionChanged != null) {
            this.onSelectionChanged.accept(newValue);
        }
    }

    public T getSelected() {
        int index = this.selectedIndex.get();
        if (index >= 0 && index < this.options.size()) {
            return this.options.get(index);
        }
        return null;
    }

    public void setSelected(T value) {
        int index = this.options.indexOf(value);
        if (index >= 0) {
            this.selectedIndex.set(index);
        }
    }

    public void setDisplayTextProvider(Function<T, String> provider) {
        this.displayTextProvider = provider;
    }

    public void setOnSelectionChanged(Consumer<T> listener) {
        this.onSelectionChanged = listener;
    }

    public void setOnOpen(Consumer<Void> listener) {
        this.onOpen = listener;
    }

    public void setOnClose(Consumer<Void> listener) {
        this.onClose = listener;
    }

    public void setMaxVisibleItems(int max) {
        this.maxVisibleItems = max;
    }

    public void setPlaceholder(Component placeholder) {
        this.placeholder = placeholder;
    }

    public boolean isOpen() {
        return this.open.get();
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
        return new Size(constraints.maxWidth(), DEFAULT_HEIGHT);
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


