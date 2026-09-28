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
import com.tridevmc.compound.ui.slot.SlotKey;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * A vanilla-styled button with centered content in {@link #CONTENT_SLOT}.
 * Enabled, visible buttons accept left clicks and focused Enter or Space activation.
 * Retain the instance to preserve its state and registered listeners.
 * <p>
 * For a text button, fill the content slot with a {@link Label}.
 */
public class Button extends BaseElement implements IComposableElement {

    /**
     * The centered button content, commonly a {@link Label}.
     */
    public static final SlotKey CONTENT_SLOT = new SlotKey("content");

    private static final IScreenSprite DEFAULT_ENABLED_SPRITE = IScreenSprite.of(Identifier.withDefaultNamespace("widget/button"));
    private static final IScreenSprite DEFAULT_DISABLED_SPRITE = IScreenSprite.of(Identifier.withDefaultNamespace("widget/button_disabled"));
    private static final IScreenSprite DEFAULT_HIGHLIGHTED_SPRITE = IScreenSprite.of(Identifier.withDefaultNamespace("widget/button_highlighted"));

    /**
     * The global vertical content offset in GUI pixels, applied when a button composes.
     * The default of one pixel matches vanilla button text positioning.
     */
    public static int VANILLA_TEXT_Y_OFFSET = 1;

    private Supplier<IScreenSprite> normalSprite = () -> DEFAULT_ENABLED_SPRITE;
    private Supplier<IScreenSprite> highlightedSprite = () -> DEFAULT_HIGHLIGHTED_SPRITE;
    private Supplier<IScreenSprite> disabledSprite = () -> DEFAULT_DISABLED_SPRITE;
    private final State<Boolean> enabled;
    private final State<Boolean> visible;
    private final State<Boolean> hovered;
    private final State<Boolean> focused;
    private final List<IButtonPressListener> pressListeners;
    private final List<IButtonHoverListener> hoverListeners;

    /**
     * Creates an enabled, visible button.
     */
    public Button() {
        this(true, true);
    }

    /**
     * Creates a visible button.
     *
     * @param enabled whether user activation is allowed.
     */
    public Button(boolean enabled) {
        this(enabled, true);
    }

    /**
     * Creates a button with the given interaction and visibility states.
     *
     * @param enabled whether user activation is allowed.
     * @param visible whether the button composes its content.
     */
    public Button(boolean enabled, boolean visible) {
        this.enabled = State.of(enabled);
        this.visible = State.of(visible);
        this.hovered = State.of(false);
        this.focused = State.of(false);
        this.pressListeners = Lists.newArrayList();
        this.hoverListeners = Lists.newArrayList();
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.visible);

        scope.onMouseEnter(() -> {
            this.hovered.set(true);
            var bounds = this.getBounds();
            if (bounds != null) {
                this.hoverListeners.forEach(listener -> listener.onButtonHover(
                        bounds.x() + bounds.width() / 2.0,
                        bounds.y() + bounds.height() / 2.0,
                        true
                ));
            }
        });

        scope.onMouseExit(() -> {
            this.hovered.set(false);
            var bounds = this.getBounds();
            if (bounds != null) {
                this.hoverListeners.forEach(listener -> listener.onButtonHover(
                        bounds.x() + bounds.width() / 2.0,
                        bounds.y() + bounds.height() / 2.0,
                        false
                ));
            }
        });

        scope.onFocusGained(() -> this.focused.set(true));
        scope.onFocusLost(() -> this.focused.set(false));

        scope.onClick(event -> {
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.canPress()) {
                return false;
            }

            scope.requestFocus();
            var bounds = this.getBounds();
            int x = event.x();
            int y = event.y();

            if (x >= bounds.x() && x < bounds.x() + bounds.width() &&
                    y >= bounds.y() && y < bounds.y() + bounds.height()) {

                SoundManager soundManager = Minecraft.getInstance().getSoundManager();
                soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                this.pressListeners.forEach(listener -> listener.onButtonPress(x, y));
                return true;
            } else {
                return false;
            }
        });

        scope.onKeyPress(event -> {
            if (!this.canPress() || !scope.isFocused()) return false;
            if (event.keyCode() != InputConstants.KEY_SPACE && event.keyCode() != InputConstants.KEY_RETURN
                    && event.keyCode() != InputConstants.KEY_NUMPADENTER) return false;
            var bounds = this.getBounds();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.pressListeners.forEach(listener -> listener.onButtonPress(bounds.x(), bounds.y()));
            return true;
        });
        if (!this.visible.get()) return;
        scope.e(new Stack(), stack -> {
            stack.layout().contentAlignment(Alignment.CENTER);

            stack.e(new Sprite(() -> !this.enabled.get() ? this.disabledSprite.get()
                    : this.hovered.get() || this.focused.get() ? this.highlightedSprite.get()
                    : this.normalSprite.get()), sprite -> sprite.layout().fillMax());

            stack.e(new Box(), contentWrapper -> {
                contentWrapper.layout().margin(0, VANILLA_TEXT_Y_OFFSET, 0, 0);
                scope.slotInto(CONTENT_SLOT, contentWrapper);
            });
        });
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

    /** {@inheritDoc} */
    @Override
    public boolean isFocusable() {
        return this.canPress();
    }

    private boolean canPress() {
        return this.visible.get() && this.enabled.get();
    }

    /**
     * Replaces the sprites used for the button states.
     *
     * @param normal the idle sprite supplier.
     * @param highlighted the hovered or focused sprite supplier.
     * @param disabled the disabled sprite supplier.
     */
    public void setSprites(Supplier<IScreenSprite> normal, Supplier<IScreenSprite> highlighted,
                           Supplier<IScreenSprite> disabled) {
        this.normalSprite = normal;
        this.highlightedSprite = highlighted;
        this.disabledSprite = disabled;
    }

    /**
     * Replaces the sprites used for the button states.
     *
     * @param normal the idle sprite.
     * @param highlighted the hovered or focused sprite.
     * @param disabled the disabled sprite.
     */
    public void setSprites(IScreenSprite normal, IScreenSprite highlighted, IScreenSprite disabled) {
        this.setSprites(() -> normal, () -> highlighted, () -> disabled);
    }

    /**
     * Adds a listener for mouse and keyboard activation.
     *
     * @param listener the listener receiving GUI coordinates; keyboard activation uses the button origin.
     */
    public void addPressListener(IButtonPressListener listener) {
        this.pressListeners.add(listener);
    }

    /**
     * Adds a listener for pointer entry and exit.
     *
     * @param listener the listener receiving the button center and whether the pointer entered.
     */
    public void addHoverListener(IButtonHoverListener listener) {
        this.hoverListeners.add(listener);
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

    /**
     * Returns whether the button is visible.
     *
     * @return true when the button composes its content.
     */
    public boolean isVisible() {
        return this.visible.get();
    }

    /**
     * Changes visibility and whether the button can receive focus or activation.
     *
     * @param visible whether to show the button.
     */
    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    /**
     * Returns the current pointer hover state.
     *
     * @return true while the pointer is over the button.
     */
    public boolean isHovered() {
        return this.hovered.get();
    }

    /**
     * Returns the live enabled state owned by this button.
     *
     * @return the retained enabled state, not a snapshot.
     */
    public State<Boolean> getEnabledState() {
        return this.enabled;
    }

    /**
     * Returns the live visible state owned by this button.
     *
     * @return the retained visible state, not a snapshot.
     */
    public State<Boolean> getVisibleState() {
        return this.visible;
    }

    /**
     * Returns the live hovered state owned by this button.
     *
     * @return the retained hovered state, not a snapshot.
     */
    public State<Boolean> getHoveredState() {
        return this.hovered;
    }

    /** {@inheritDoc} */
    @Override
    public UICursor getCursor(int x, int y) {
        return this.canPress() ? CompoundCursors.HAND : null;
    }

    /**
     * Returns the live focused state owned by this button.
     *
     * @return the retained focused state, not a snapshot.
     */
    public State<Boolean> getFocusedState() {
        return this.focused;
    }
}
