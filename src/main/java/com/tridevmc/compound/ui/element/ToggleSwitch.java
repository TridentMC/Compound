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
 * An on/off toggle switch component with a sliding thumb animation.
 * Alternative to checkbox for boolean options.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new ToggleSwitch(), toggle -> {
 *     toggle.getElement().setOn(true);
 *     toggle.getElement().setOnChanged(on -> {
 *         config.setFeatureEnabled(on);
 *     });
 * });
 * </pre>
 */
public class ToggleSwitch extends BaseElement implements IComposableElement {

    private static final int DEFAULT_WIDTH = 32;
    private static final int DEFAULT_HEIGHT = 16;
    private static final int THUMB_SIZE = 14;
    private static final int PADDING = 1;

    private final State<Boolean> on = new StateImpl<>(false);
    private final State<Boolean> enabled = new StateImpl<>(true);
    private final State<Boolean> hovered = new StateImpl<>(false);
    private final List<Consumer<Boolean>> changeListeners = Lists.newArrayList();

    private int onColor = 0xFF4CAF50;
    private int offColor = 0xFF757575;
    private int thumbColor = 0xFFFFFFFF;
    private int disabledColor = 0xFF404040;
    private Component onLabel;
    private Component offLabel;

    public ToggleSwitch() {
    }

    public ToggleSwitch(boolean initialState) {
        this.on.set(initialState);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.enabled);
        scope.bind(this.hovered);
        scope.bind(this.on);

        scope.onMouseEnter(() -> this.hovered.set(true));
        scope.onMouseExit(() -> this.hovered.set(false));

        scope.onClick(event -> {
            if (!this.enabled.get()) return false;
            this.toggle();
            return true;
        });

        scope.onKeyPress(event -> {
            if (!this.enabled.get()) return false;
            if (event.keyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE ||
event.keyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                this.toggle();
                return true;
            }
            return false;
        });

        scope.e(new Stack(), stack -> {
            stack.layout().fixedSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

            int trackColor;
            if (!this.enabled.get()) {
                trackColor = this.disabledColor;
            } else if (this.on.get()) {
                trackColor = this.onColor;
            } else {
                trackColor = this.offColor;
            }

            stack.e(new Rect(trackColor), bg -> bg.layout().fillMax());

            int thumbX = this.on.get() ? DEFAULT_WIDTH - THUMB_SIZE - PADDING : PADDING;
            stack.e(new Rect(() -> this.thumbColor), thumb ->
                    thumb.layout().fixedSize(THUMB_SIZE, THUMB_SIZE)
                            .margin(thumbX, PADDING, 0, 0));

            if (this.onLabel != null && this.on.get()) {
                stack.e(new Label(this.onLabel, 0xFFFFFFFF, false),
                        label -> label.layout().contentAlignment(Alignment.CENTER));
            } else if (this.offLabel != null && !this.on.get()) {
                stack.e(new Label(this.offLabel, 0xFFFFFFFF, false),
                        label -> label.layout().contentAlignment(Alignment.CENTER));
            }
        });
    }

    private void toggle() {
        boolean newValue = !this.on.get();
        this.on.set(newValue);

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        this.changeListeners.forEach(listener -> listener.accept(newValue));
    }

    public boolean isOn() {
        return this.on.get();
    }

    public void setOn(boolean on) {
        this.on.set(on);
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
    }

    public void setOffColor(int color) {
        this.offColor = color;
    }

    public void setThumbColor(int color) {
        this.thumbColor = color;
    }

    public void setOnLabel(Component label) {
        this.onLabel = label;
    }

    public void setOffLabel(Component label) {
        this.offLabel = label;
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
    public CursorType getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}
