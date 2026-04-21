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

/**
 * A boolean toggle component with a box and optional label.
 * Used for enabling/disabling options and multi-select scenarios.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new Checkbox(Component.literal("Enable Sounds")), checkbox -> {
 *     checkbox.getElement().setChecked(true);
 *     checkbox.getElement().setOnCheckedChanged(checked -> {
 *         config.setSoundsEnabled(checked);
 *     });
 * });
 * </pre>
 */
public class Checkbox extends BaseElement implements IComposableElement {

    private static final int BOX_SIZE = 16;
    private static final IScreenSprite DEFAULT_BOX_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/checkbox"));
    private static final IScreenSprite DEFAULT_CHECKED_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/checkbox_checked"));
    private static final IScreenSprite DEFAULT_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted"));

    private final State<Boolean> checked = new StateImpl<>(false);
    private final State<Boolean> enabled = new StateImpl<>(true);
    private final State<Boolean> hovered = new StateImpl<>(false);
    private final List<Consumer<Boolean>> checkedChangeListeners = Lists.newArrayList();
    private Component label;
    private int labelColor = 0xFFFFFF;
    private int spacing = 4;
    private boolean labelRight = true;

    public Checkbox() {
    }

    public Checkbox(Component label) {
        this.label = label;
    }

    public Checkbox(Component label, boolean checked) {
        this.label = label;
        this.checked.set(checked);
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.enabled);
        scope.bind(this.hovered);
        scope.bind(this.checked);

        scope.onMouseEnter(() -> this.hovered.set(true));
        scope.onMouseExit(() -> this.hovered.set(false));

        scope.onClick(event -> {
            if (!this.enabled.get()) return false;
            this.toggle();
            return true;
        });

        scope.onKeyPress(event -> {
            if (!this.enabled.get()) return false;
            if (event.keyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) {
                this.toggle();
                return true;
            }
            return false;
        });

        scope.e(new Row(), row -> {
            row.layout().spacing(this.spacing).verticalAlignment(Alignment.CENTER);

            if (this.label != null && !this.labelRight) {
                row.e(new Label(this.label, () -> this.enabled.get() ? this.labelColor : 0x808080, () -> false));
            }

            row.e(new Stack(), boxStack -> {
                boxStack.layout().fixedSize(BOX_SIZE, BOX_SIZE);

                IScreenSprite boxSprite;
                if (!this.enabled.get()) {
                    boxSprite = DEFAULT_BOX_SPRITE;
                } else if (this.hovered.get()) {
                    boxSprite = DEFAULT_HIGHLIGHTED_SPRITE;
                } else {
                    boxSprite = DEFAULT_BOX_SPRITE;
                }

                boxStack.e(new Sprite(boxSprite), s -> s.layout().fillMax());

                if (this.checked.get()) {
                    boxStack.e(new Sprite(DEFAULT_CHECKED_SPRITE), s -> s.layout().fillMax());
                }
            });

            if (this.label != null && this.labelRight) {
                row.e(new Label(this.label, () -> this.enabled.get() ? this.labelColor : 0x808080, () -> false));
            }
        });
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

    private void toggle() {
        boolean newValue = !this.checked.get();
        this.checked.set(newValue);

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        this.checkedChangeListeners.forEach(listener -> listener.accept(newValue));
    }

    public boolean isChecked() {
        return this.checked.get();
    }

    public void setChecked(boolean checked) {
        this.checked.set(checked);
    }

    public State<Boolean> getCheckedState() {
        return this.checked;
    }

    public boolean isEnabled() {
        return this.enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public Component getLabel() {
        return this.label;
    }

    public void setLabel(Component label) {
        this.label = label;
    }

    public int getLabelColor() {
        return this.labelColor;
    }

    public void setLabelColor(int color) {
        this.labelColor = color;
    }

    public int getSpacing() {
        return this.spacing;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    public boolean isLabelRight() {
        return this.labelRight;
    }

    public void setLabelRight(boolean right) {
        this.labelRight = right;
    }

    public void setOnCheckedChanged(Consumer<Boolean> listener) {
        this.checkedChangeListeners.clear();
        this.checkedChangeListeners.add(listener);
    }

    public void addCheckedChangeListener(Consumer<Boolean> listener) {
        this.checkedChangeListeners.add(listener);
    }

    @Override
    public CursorType getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}
