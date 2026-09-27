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
import com.tridevmc.compound.ui.animation.AnimatedState;
import com.tridevmc.compound.ui.animation.Easing;
import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Position;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.sprite.IScreenSprite;
import com.tridevmc.compound.ui.state.State;
import com.tridevmc.compound.ui.state.StateImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;



import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ToggleSwitch extends BaseElement implements IComposableElement {

    private static final int DEFAULT_WIDTH = 50;
    private static final int DEFAULT_HEIGHT = 20;
    private static final int HANDLE_WIDTH = 8;

    private static final IScreenSprite TRACK_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/slider"));
    private static final IScreenSprite TRACK_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/slider_highlighted"));
    private static final IScreenSprite HANDLE_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/slider_handle"));
    private static final IScreenSprite HANDLE_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/slider_handle_highlighted"));
    private class ToggleThumb extends BaseElement implements IComposableElement {
        private final Supplier<IScreenSprite> spriteSupplier;
        private final Supplier<Integer> positionSupplier;
        private final Supplier<Integer> colorSupplier;

        ToggleThumb(Supplier<IScreenSprite> spriteSupplier,
                    Supplier<Integer> positionSupplier,
                    Supplier<Integer> colorSupplier) {
            this.spriteSupplier = spriteSupplier;
            this.positionSupplier = positionSupplier;
            this.colorSupplier = colorSupplier;
        }

        @Override
        public void compose(ICompositionScope scope) {
            var sprite = this.spriteSupplier.get();
            if (sprite != null) {
                scope.e(new Sprite(sprite), s -> s.layout().fixedSize(HANDLE_WIDTH, DEFAULT_HEIGHT));
            } else {
                scope.e(new Rect(this.colorSupplier), r -> r.layout().fixedSize(HANDLE_WIDTH, DEFAULT_HEIGHT));
            }
        }

        @Override
        public Size measure(Constraints constraints, LayoutProperties ownProperties, List<Size> measuredChildren) {
            return new Size(constraints.maxWidth(), DEFAULT_HEIGHT);
        }

        @Override
        public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
            int handleX = this.positionSupplier.get();
            return List.of(new Bounds(
                    new Position(bounds.x() + handleX, bounds.y()),
                    new Size(HANDLE_WIDTH, bounds.height())
            ));
        }
    }

    private final State<Boolean> on = new StateImpl<>(false);
    private final State<Boolean> enabled = new StateImpl<>(true);
    private final State<Boolean> hovered = new StateImpl<>(false);
    private final List<Consumer<Boolean>> changeListeners = Lists.newArrayList();

    private int onColor = 0xFF4CAF50;
    private int offColor = 0xFF757575;
    private int thumbColor = 0xFFFFFFFF;
    private int disabledColor = 0xFF404040;
    private int hoverColor = 0xFF90CAF9;
    private Component onLabel;
    private Component offLabel;
    private boolean useTextures = true;
    private AnimatedState<Integer> thumbAnimation;

    public ToggleSwitch() {
    }

    public ToggleSwitch(boolean initialState) {
        this.on.set(initialState);
    }

    @Override
    public void onDetached() {
        this.thumbAnimation = null;
        this.hovered.set(false);
    }

    @Override
    public Component getNarrationMessage() {
        return Component.translatable(this.on.get() ? "options.on" : "options.off");
    }

    @Override
    public void compose(ICompositionScope scope) {
        if (this.thumbAnimation == null) {
            int initialX = this.on.get() ? DEFAULT_WIDTH - HANDLE_WIDTH : 0;
            this.thumbAnimation = scope.animateInt(initialX, 150, Easing.EASE_OUT);
        }
        scope.retainAnimation(this.thumbAnimation);

        scope.bind(this.enabled);
        scope.bind(this.hovered);
        scope.bind(this.on);
        scope.bindLayout(this.thumbAnimation);

        scope.onMouseEnter(() -> this.hovered.set(true));
        scope.onMouseExit(() -> this.hovered.set(false));

        scope.onClick(event -> {
            if (!this.enabled.get() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
            scope.requestFocus();
            this.toggle();
            return true;
        });

        scope.onKeyPress(event -> {
            if (!this.enabled.get()) return false;
            if (event.keyCode() == InputConstants.KEY_SPACE ||
                    event.keyCode() == InputConstants.KEY_RETURN) {
                this.toggle();
                return true;
            }
            return false;
        });

        scope.e(new Stack(), stack -> {
            stack.layout().fixedSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

            if (this.useTextures) {
                IScreenSprite trackSprite = !this.enabled.get()
                        ? TRACK_SPRITE
                        : this.hovered.get()
                        ? TRACK_HIGHLIGHTED_SPRITE
                        : TRACK_SPRITE;
                stack.e(new Sprite(trackSprite), bg -> bg.layout().fillMax());

                stack.e(new ToggleThumb(
                        () -> !this.enabled.get()
                                ? HANDLE_SPRITE
                                : this.hovered.get()
                                ? HANDLE_HIGHLIGHTED_SPRITE
                                : HANDLE_SPRITE,
                        () -> this.thumbAnimation.get(),
                        () -> this.thumbColor
                ), handle -> handle.layout().fillMax());
            } else {
                int trackColor;
                if (!this.enabled.get()) {
                    trackColor = this.disabledColor;
                } else if (this.hovered.get()) {
                    trackColor = this.hoverColor;
                } else if (this.on.get()) {
                    trackColor = this.onColor;
                } else {
                    trackColor = this.offColor;
                }

                stack.e(new Rect(() -> trackColor), bg -> bg.layout().fillMax());

                stack.e(new ToggleThumb(
                        () -> null,
                        () -> this.thumbAnimation.get(),
                        () -> this.thumbColor
                ), handle -> handle.layout().fillMax());
            }

            if (this.onLabel != null && this.on.get()) {
                stack.e(new Label(this.onLabel, 0xFFFFFFFF, false),
                        label -> label.layout().contentAlignment(Alignment.CENTER));
            } else if (this.offLabel != null && !this.on.get()) {
                stack.e(new Label(this.offLabel, 0xFFFFFFFF, false),
                        label -> label.layout().contentAlignment(Alignment.CENTER));
            }
        });
    }

    @Override
    public boolean isFocusable() { return this.enabled.get(); }

    private void toggle() {
        boolean newValue = !this.on.get();
        this.on.set(newValue);

        if (this.thumbAnimation != null) {
            int targetX = newValue ? DEFAULT_WIDTH - HANDLE_WIDTH : 0;
            this.thumbAnimation.set(targetX);
        }

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        this.changeListeners.forEach(listener -> listener.accept(newValue));
    }

    public boolean isOn() {
        return this.on.get();
    }

    public void setOn(boolean on) {
        this.on.set(on);
        if (this.thumbAnimation != null) {
            int targetX = on ? DEFAULT_WIDTH - HANDLE_WIDTH : 0;
            this.thumbAnimation.set(targetX);
        }
    }

    public State<Boolean> getOnState() {
        return this.on;
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public void setOnChanged(Consumer<Boolean> listener) {
        this.changeListeners.clear();
        this.changeListeners.add(listener);
    }

    public void addOnChangedListener(Consumer<Boolean> listener) {
        this.changeListeners.add(listener);
    }

    public void setOnColor(int color) {
        this.onColor = color;
        this.invalidateComposition();
    }

    public void setOffColor(int color) {
        this.offColor = color;
        this.invalidateComposition();
    }

    public void setThumbColor(int color) {
        this.thumbColor = color;
    }

    public void setOnLabel(Component label) {
        this.onLabel = label;
        this.invalidateComposition();
    }

    public void setOffLabel(Component label) {
        this.offLabel = label;
        this.invalidateComposition();
    }

    public void setUseTextures(boolean useTextures) {
        this.useTextures = useTextures;
        this.invalidateComposition();
    }

    public boolean isUsingTextures() {
        return this.useTextures;
    }

    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @Override
    public List<Bounds> place(@Nonnull Bounds bounds, LayoutProperties props, List<Size> measuredChildren) {
        if (measuredChildren.isEmpty()) {
            return List.of();
        }
        return List.of(bounds);
    }

    @Override
    public UICursor getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}
