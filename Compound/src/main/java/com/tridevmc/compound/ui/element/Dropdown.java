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
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;



import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A single-selection dropdown with a viewport-bounded, scrollable popup.
 * Options are copied at construction; the same instance retains its selection.
 * Arrow keys navigate the open popup, Enter or Space confirms, and Escape dismisses it.
 * Programmatic selection does not invoke the selection callback.
 *
 * @param <T> the option value type.
 */
public class Dropdown<T> extends Element implements IComposableElement {

    private static final int DEFAULT_HEIGHT = 20;
    private static final int OPTION_HEIGHT = 16;
    private static final int MAX_VISIBLE_ITEMS = 8;

    private static final IScreenSprite BUTTON_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/button"));
    private static final IScreenSprite BUTTON_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/button_highlighted"));
    private static final IScreenSprite BUTTON_DISABLED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/button_disabled"));

    private final State<Boolean> enabled = State.of(true);
    private final State<Boolean> hovered = State.of(false);
    private final State<Boolean> open = State.of(false);
    private final State<Integer> selectedIndex = State.of(-1);
    private final State<Integer> hoverIndex = State.of(-1);

    private final List<T> options;
    private Function<T, String> displayTextProvider = Object::toString;
    private Consumer<T> onSelectionChanged;
    private Consumer<Void> onOpen;
    private Consumer<Void> onClose;
    private int maxVisibleItems = MAX_VISIBLE_ITEMS;
    private Component placeholder;
    private ScrollArea popupScroll;
    private Bounds popupBounds = new Bounds(0, 0, 0, 0);

    /**
     * Creates an enabled dropdown with no selection.
     *
     * @param options the options, copied in display order.
     */
    public Dropdown(List<T> options) {
        this.options = Lists.newArrayList(options);
    }

    /**
     * Creates a dropdown selecting the first option equal to the supplied value.
     *
     * @param options the options, copied in display order.
     * @param selected the initial option; an absent value leaves the selection empty.
     */
    public Dropdown(List<T> options, T selected) {
        this(options);
        int index = this.options.indexOf(selected);
        if (index >= 0) {
            this.selectedIndex.set(index);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.open);
        scope.onMouseEnter(() -> this.hovered.set(true));
        scope.onMouseExit(() -> this.hovered.set(false));
        scope.onClick(event -> {
            if (!this.enabled.get() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
            scope.requestFocus();
            this.toggleOpen();
            return true;
        });
        scope.onKeyPress(event -> {
            if (!this.enabled.get()) return false;
            return switch (event.keyCode()) {
                case InputConstants.KEY_ESCAPE -> {
                    if (!this.open.get()) yield false;
                    this.setOpen(false);
                    yield true;
                }
                case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_SPACE -> {
                    if (this.open.get()) this.select(this.hoverIndex.get());
                    else this.setOpen(true);
                    yield true;
                }
                case InputConstants.KEY_DOWN, InputConstants.KEY_UP -> {
                    if (this.options.isEmpty()) yield false;
                    if (!this.open.get()) this.setOpen(true);
                    int step = event.keyCode() == InputConstants.KEY_DOWN ? 1 : -1;
                    int index = Math.floorMod(this.hoverIndex.get() + step, this.options.size());
                    this.hoverIndex.set(index);
                    if (this.popupScroll != null) {
                        int top = this.popupScroll.getScrollYState().get();
                        int row = index * OPTION_HEIGHT;
                        int height = Math.max(OPTION_HEIGHT, this.popupBounds.height());
                        this.popupScroll.scrollTo(0, row < top ? row : Math.max(top, row + OPTION_HEIGHT - height));
                    }
                    yield true;
                }
                default -> false;
            };
        });
        scope.e(new Stack(), stack -> {
            stack.layout().fillMax().contentAlignment(Alignment.CENTER_LEFT);
            stack.e(new Sprite(() -> !this.enabled.get() ? BUTTON_DISABLED_SPRITE
                    : this.hovered.get() || scope.isFocused() || this.open.get()
                    ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE), sprite -> sprite.layout().fillMax());
            stack.e(new Label(() -> {
                int index = this.selectedIndex.get();
                return index >= 0 && index < this.options.size()
                        ? Component.literal(this.displayTextProvider.apply(this.options.get(index)))
                        : this.placeholder != null ? this.placeholder : Component.literal("Select...");
            }, () -> this.enabled.get() ? 0xFFFFFF : 0x808080, () -> true),
                    label -> label.layout().margin(5, 1, 5, 0));
        });
        if (!this.open.get()) return;
        var tree = scope.getTree();
        tree.setInputRoot(this.getNode());
        this.popupScroll = new ScrollArea().scrollSpeed(OPTION_HEIGHT);
        int visibleHeight = Math.max(0, this.calculatePopupBounds().height() - 2);
        this.popupScroll.scrollTo(0, Math.max(0, (this.hoverIndex.get() + 1) * OPTION_HEIGHT - visibleHeight));
        scope.e(new Popup(), popup -> {
            var viewport = tree.getViewportSize();
            popup.layout().fixedSize(viewport.width(), viewport.height()).layer(100);
            popup.onClick(event -> { this.setOpen(false); return true; });
            popup.e(new Surface(0xFF202020, 0xFFA0A0A0, 1), surface -> {
                var bounds = this.calculatePopupBounds();
                surface.layout().fixedSize(bounds.width(), bounds.height());
                surface.fillSlot(Surface.CONTENT_SLOT, content -> {
                    content.e(this.popupScroll, scroll -> {
                        scroll.layout().fillMax().margin(1);
                        scroll.fillSlot(ScrollArea.CONTENT_SLOT, items -> {
                            items.e(new Column(), column -> {
                                column.layout().fillMaxWidth();
                                for (int i = 0; i < this.options.size(); i++) {
                                    final int index = i;
                                    column.e(ListItem.builder(Component.literal(this.displayTextProvider.apply(this.options.get(i))))
                                            .selected(() -> this.selectedIndex.get() == index)
                                            .hovered(() -> this.hoverIndex.get() == index).build(), row -> {
                                        row.layout().fillMaxWidth().fixedHeight(OPTION_HEIGHT);
                                        row.getElement().setClickHandler(() -> this.select(index));
                                        row.getElement().setHoverHandlers(() -> this.hoverIndex.set(index), () -> {});
                                    });
                                }
                            });
                        });
                    });
                });
            });
        });
    }

    private Bounds calculatePopupBounds() {
        var anchor = this.getBounds();
        var viewport = this.getNode().getTree().getViewportSize();
        int below = Math.max(0, viewport.height() - anchor.bottom() - 2);
        int above = Math.max(0, anchor.y() - 2);
        int wanted = Math.min(this.options.size(), this.maxVisibleItems) * OPTION_HEIGHT + 2;
        boolean upwards = below < wanted && above > below;
        int height = Math.min(wanted, upwards ? above : below);
        int width = Math.min(anchor.width(), viewport.width());
        this.popupBounds = new Bounds(Math.clamp(anchor.x(), 0, Math.max(0, viewport.width() - width)),
                upwards ? anchor.y() - height : anchor.bottom(), width, height);
        return this.popupBounds;
    }

    private class Popup extends Container {
        @Override
        public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
            return getNode().getTree().getViewportSize();
        }

        @Override
        public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> children) {
            return List.of(Dropdown.this.calculatePopupBounds());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFocusable() {
        return this.enabled.get();
    }


    private void toggleOpen() {
        this.setOpen(!this.open.get());
    }

    private void setOpen(boolean open) {
        if (open && (!this.enabled.get() || this.options.isEmpty())) return;
        boolean wasOpen = this.open.get();
        if (!open && this.getNode() != null) this.getNode().getTree().clearInputRoot(this.getNode());
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

        T newValue = this.options.get(index);

        this.selectedIndex.set(index);
        this.setOpen(false);

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        if (this.onSelectionChanged != null) {
            this.onSelectionChanged.accept(newValue);
        }
    }

    /**
     * Returns the selected option.
     *
     * @return the selected value, or null when no option is selected.
     */
    public T getSelected() {
        int index = this.selectedIndex.get();
        if (index >= 0 && index < this.options.size()) {
            return this.options.get(index);
        }
        return null;
    }

    /**
     * Selects the first equal option silently; an absent value leaves the selection unchanged.
     *
     * @param value the option to select.
     */
    public void setSelected(T value) {
        int index = this.options.indexOf(value);
        if (index >= 0) {
            this.selectedIndex.set(index);
        }
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
     * Replaces the callback invoked when a user confirms a popup option, even if unchanged.
     *
     * @param listener the callback, or null to remove it.
     */
    public void setOnSelectionChanged(Consumer<T> listener) {
        this.onSelectionChanged = listener;
    }

    /**
     * Replaces the callback for a transition to the open state.
     *
     * @param listener the callback, invoked with null, or null to remove it.
     */
    public void setOnOpen(Consumer<Void> listener) {
        this.onOpen = listener;
    }

    /**
     * Replaces the callback for a transition to the closed state.
     *
     * @param listener the callback, invoked with null, or null to remove it.
     */
    public void setOnClose(Consumer<Void> listener) {
        this.onClose = listener;
    }

    /**
     * Sets the preferred visible option count; viewport space can reduce it further.
     *
     * @param max the count, clamped to at least one.
     */
    public void setMaxVisibleItems(int max) {
        this.maxVisibleItems = Math.max(1, max);
        this.invalidateComposition();
    }

    /**
     * Sets the label displayed when nothing is selected.
     *
     * @param placeholder the label, or null to use the default Select... text.
     */
    public void setPlaceholder(Component placeholder) {
        this.placeholder = placeholder;
    }

    /**
     * Returns whether the options popup is open.
     *
     * @return true when open.
     */
    public boolean isOpen() {
        return this.open.get();
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
     * Changes whether user interaction is enabled. Disabling also closes an open popup.
     *
     * @param enabled whether to accept user interaction.
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        if (!enabled) this.setOpen(false);
    }

    /** {@inheritDoc} */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> children) {
        return new Size(constraints.maxWidth(), DEFAULT_HEIGHT);
    }

    /** {@inheritDoc} */
    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> children) {
        if (children.size() < 2) return List.of(bounds);
        var viewport = this.getNode().getTree().getViewportSize();
        return List.of(bounds, new Bounds(0, 0, viewport.width(), viewport.height()));
    }

    /** {@inheritDoc} */
    @Override
    public UICursor getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}


