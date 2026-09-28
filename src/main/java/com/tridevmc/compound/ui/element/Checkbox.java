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

/**
 * A vanilla checkbox with an optional label. Mouse clicks and Space toggle its retained
 * checked state and notify listeners; {@link #setChecked(boolean)} changes it silently.
 */
public class Checkbox extends Element implements IComposableElement {

    private static final int BOX_SIZE = 17;
    private static final IScreenSprite DEFAULT_BOX_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/checkbox"));
    private static final IScreenSprite DEFAULT_CHECKED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/checkbox_selected"));
    private static final IScreenSprite DEFAULT_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/checkbox_highlighted"));
    private static final IScreenSprite DEFAULT_CHECKED_HIGHLIGHTED_SPRITE = IScreenSprite.of(
            Identifier.withDefaultNamespace("widget/checkbox_selected_highlighted"));

    private final State<Boolean> checked = State.of(false);
    private final State<Boolean> enabled = State.of(true);
    private final State<Boolean> hovered = State.of(false);
    private final List<Consumer<Boolean>> checkedChangeListeners = Lists.newArrayList();
    private Component label;
    private int labelColor = 0xFFFFFF;
    private int spacing = 4;
    private boolean labelRight = true;

    /**
     * Creates an unchecked checkbox without a label.
     */
    public Checkbox() {
    }

    /**
     * Creates an unchecked checkbox.
     *
     * @param label the optional label, or null for none.
     */
    public Checkbox(Component label) {
        this.label = label;
    }

    /**
     * Creates a checkbox with an initial checked state.
     *
     * @param label the optional label, or null for none.
     * @param checked the initial checked state.
     */
    public Checkbox(Component label, boolean checked) {
        this.label = label;
        this.checked.set(checked);
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        this.registerInput(scope);
        scope.e(new Row(), row -> {
            row.layout().fixedHeight(BOX_SIZE).spacing(this.spacing).verticalAlignment(Alignment.CENTER);
            if (!this.labelRight) this.composeLabel(row);
            this.composeIndicator(row, scope);
            if (this.labelRight) this.composeLabel(row);
        });
    }

    private void registerInput(ICompositionScope scope) {
        scope.onMouseEnter(() -> this.hovered.set(true));
        scope.onMouseExit(() -> this.hovered.set(false));
        scope.onClick(event -> {
            if (!this.enabled.get() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
            scope.requestFocus();
            this.toggle();
            return true;
        });
        scope.onKeyPress(event -> {
            if (!this.enabled.get() || event.keyCode() != InputConstants.KEY_SPACE) return false;
            this.toggle();
            return true;
        });
    }

    private void composeLabel(ICompositionScope scope) {
        if (this.label != null) {
            scope.e(new Label(this.label, () -> this.enabled.get() ? this.labelColor : 0x808080, () -> false));
        }
    }

    private void composeIndicator(ICompositionScope content, ICompositionScope owner) {
        content.e(new Stack(), indicator -> {
            indicator.layout().fixedSize(BOX_SIZE, BOX_SIZE);
            indicator.e(new Sprite(() -> this.getIndicatorSprite(owner.isFocused())),
                    sprite -> sprite.layout().fillMax());
        });
    }

    private IScreenSprite getIndicatorSprite(boolean focused) {
        boolean highlighted = this.hovered.get() || focused;
        if (this.checked.get()) {
            return highlighted ? DEFAULT_CHECKED_HIGHLIGHTED_SPRITE : DEFAULT_CHECKED_SPRITE;
        }
        return highlighted ? DEFAULT_HIGHLIGHTED_SPRITE : DEFAULT_BOX_SPRITE;
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
        return List.of(new Bounds(bounds.x(), bounds.y(), bounds.width(), BOX_SIZE));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFocusable() { return this.enabled.get(); }

    private void toggle() {
        boolean newValue = !this.checked.get();
        this.checked.set(newValue);

        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        this.checkedChangeListeners.forEach(listener -> listener.accept(newValue));
    }

    /**
     * Returns the checked state.
     *
     * @return true when checked.
     */
    public boolean isChecked() {
        return this.checked.get();
    }

    /**
     * Changes the checked state without invoking checked-change listeners.
     *
     * @param checked the new checked state.
     */
    public void setChecked(boolean checked) {
        this.checked.set(checked);
    }

    /**
     * Returns the live checked state owned by this checkbox.
     *
     * @return the retained state; direct changes do not invoke checked-change listeners.
     */
    public State<Boolean> getCheckedState() {
        return this.checked;
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
     * Returns the optional label.
     *
     * @return the label, or null when absent.
     */
    public Component getLabel() {
        return this.label;
    }

    /**
     * Replaces the label and rebuilds the row.
     *
     * @param label the label, or null to omit it.
     */
    public void setLabel(Component label) {
        this.label = label;
        this.invalidateComposition();
    }

    /**
     * Returns the enabled label color.
     *
     * @return the text color.
     */
    public int getLabelColor() {
        return this.labelColor;
    }

    /**
     * Sets the enabled label color; disabled labels remain gray.
     *
     * @param color the text color.
     */
    public void setLabelColor(int color) {
        this.labelColor = color;
    }

    /**
     * Returns the gap between the box and label.
     *
     * @return the gap in GUI pixels.
     */
    public int getSpacing() {
        return this.spacing;
    }

    /**
     * Sets the gap between the box and label.
     *
     * @param spacing the gap in GUI pixels.
     */
    public void setSpacing(int spacing) {
        this.spacing = spacing;
        this.invalidateComposition();
    }

    /**
     * Returns which side holds the label.
     *
     * @return true for a label to the right of the box.
     */
    public boolean isLabelRight() {
        return this.labelRight;
    }

    /**
     * Chooses the side of the box on which to place the label.
     *
     * @param right true for the right side, false for the left.
     */
    public void setLabelRight(boolean right) {
        this.labelRight = right;
        this.invalidateComposition();
    }

    /**
     * Replaces all checked-change listeners with one listener for user toggles.
     *
     * @param listener the callback receiving the new checked state.
     */
    public void setOnCheckedChanged(Consumer<Boolean> listener) {
        this.checkedChangeListeners.clear();
        this.checkedChangeListeners.add(listener);
    }

    /**
     * Adds a listener for user toggles.
     *
     * @param listener the callback receiving the new checked state.
     */
    public void addCheckedChangeListener(Consumer<Boolean> listener) {
        this.checkedChangeListeners.add(listener);
    }

    /** {@inheritDoc} */
    @Override
    public UICursor getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }
}
