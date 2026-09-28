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
 * A vertical single-selection group using vanilla checkbox sprites.
 * The same instance retains its options and selected index. Clicks and arrow keys select
 * options; programmatic selection also invokes the selection callback when it changes.
 */
public class RadioButtonGroup extends Element implements IComposableElement {

    private static final int SPACING = 4;
    private static final int BOX_SIZE = 17;
    private static final IScreenSprite BOX = IScreenSprite.of(Identifier.withDefaultNamespace("widget/checkbox"));
    private static final IScreenSprite SELECTED = IScreenSprite.of(Identifier.withDefaultNamespace("widget/checkbox_selected"));
    private static final IScreenSprite HIGHLIGHTED = IScreenSprite.of(Identifier.withDefaultNamespace("widget/checkbox_highlighted"));
    private static final IScreenSprite SELECTED_HIGHLIGHTED = IScreenSprite.of(Identifier.withDefaultNamespace("widget/checkbox_selected_highlighted"));

    private final State<Integer> selectedIndex = State.of(-1);
    private final State<Boolean> enabled = State.of(true);
    private final List<RadioOption> options = Lists.newArrayList();
    private final List<State<Boolean>> optionHoverStates = Lists.newArrayList();
    private Consumer<Integer> onSelectionChanged;
    private int optionSpacing = 4;
    private int labelColor = 0xFFFFFF;

    /**
     * Creates an empty group with no selection.
     */
    public RadioButtonGroup() {
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        while (this.optionHoverStates.size() < this.options.size()) {
            this.optionHoverStates.add(State.of(false));
        }
        this.registerKeyboardInput(scope);
        scope.e(new Column(), column -> {
            column.layout().spacing(this.optionSpacing);
            for (int i = 0; i < this.options.size(); i++) {
                this.composeOption(column, scope, i);
            }
        });
    }

    private void registerKeyboardInput(ICompositionScope scope) {
        scope.onKeyPress(event -> {
            if (!this.enabled.get() || this.options.isEmpty()) return false;
            int step = switch (event.keyCode()) {
                case InputConstants.KEY_RIGHT, InputConstants.KEY_DOWN -> 1;
                case InputConstants.KEY_LEFT, InputConstants.KEY_UP -> -1;
                default -> 0;
            };
            if (step == 0) return false;
            this.select(Math.floorMod(this.selectedIndex.get() + step, this.options.size()));
            return true;
        });
    }

    private void composeOption(ICompositionScope content, ICompositionScope owner, int index) {
        var option = this.options.get(index);
        var hovered = this.optionHoverStates.get(index);
        content.e(new Row(), row -> {
            row.layout().fixedHeight(BOX_SIZE).spacing(SPACING).verticalAlignment(Alignment.CENTER);
            row.onMouseEnter(() -> hovered.set(true));
            row.onMouseExit(() -> hovered.set(false));
            row.e(new Sprite(() -> this.getOptionSprite(index, hovered.get(), owner.isFocused())),
                    sprite -> sprite.layout().fixedSize(BOX_SIZE, BOX_SIZE));
            row.e(new Label(option.label, () -> this.enabled.get() ? this.labelColor : 0x808080, () -> false));
            row.onClick(event -> {
                if (!this.enabled.get() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                owner.requestFocus();
                this.select(index);
                return true;
            });
        });
    }

    private IScreenSprite getOptionSprite(int index, boolean hovered, boolean focused) {
        boolean selected = this.selectedIndex.get() == index;
        boolean highlighted = this.enabled.get() && (hovered || focused && selected);
        if (selected) return highlighted ? SELECTED_HIGHLIGHTED : SELECTED;
        return highlighted ? HIGHLIGHTED : BOX;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFocusable() { return this.enabled.get(); }

    private void select(int index) {
        if (index < 0 || index >= this.options.size()) return;

        int oldIndex = this.selectedIndex.get();
        if (oldIndex != index) {
            this.selectedIndex.set(index);

            SoundManager soundManager = Minecraft.getInstance().getSoundManager();
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            if (this.onSelectionChanged != null) {
                this.onSelectionChanged.accept(index);
            }
        }
    }

    /**
     * Selects an option and notifies the callback if the selection changes.
     *
     * @param index the zero-based index; invalid indices are ignored.
     */
    public void setSelectedIndex(int index) {
        if (index < 0 || index >= this.options.size()) return;
        if (this.selectedIndex.get() != index) {
            this.selectedIndex.set(index);
            if (this.onSelectionChanged != null) {
                this.onSelectionChanged.accept(index);
            }
        }
    }

    /**
     * Appends an option without changing selection.
     *
     * @param label the option label.
     */
    public void addOption(String label) {
        this.options.add(new RadioOption(Component.literal(label)));
        this.optionHoverStates.add(State.of(false));
        this.invalidateComposition();
    }

    /**
     * Appends an option without changing selection.
     *
     * @param label the option label.
     */
    public void addOption(Component label) {
        this.options.add(new RadioOption(label));
        this.optionHoverStates.add(State.of(false));
        this.invalidateComposition();
    }

    /**
     * Removes an option, clearing or shifting selection as needed without invoking the callback.
     *
     * @param index the zero-based index; invalid indices are ignored.
     */
    public void removeOption(int index) {
        if (index >= 0 && index < this.options.size()) {
            this.options.remove(index);
            this.optionHoverStates.remove(index);
            if (this.selectedIndex.get() == index) {
                this.selectedIndex.set(-1);
            } else if (this.selectedIndex.get() > index) {
                this.selectedIndex.set(this.selectedIndex.get() - 1);
            }
            this.invalidateComposition();
        }
    }

    /**
     * Returns the selected option index.
     *
     * @return the zero-based index, or -1 when no option is selected.
     */
    public int getSelectedIndex() {
        return this.selectedIndex.get();
    }

    /**
     * Replaces the callback for user and programmatic selection changes.
     *
     * @param onSelectionChanged the callback receiving the index, or null to remove it.
     */
    public void setOnSelectionChanged(Consumer<Integer> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    /**
     * Sets the vertical gap between options.
     *
     * @param spacing the gap in GUI pixels.
     */
    public void setOptionSpacing(int spacing) {
        this.optionSpacing = spacing;
        this.invalidateComposition();
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
     * Returns the number of options.
     *
     * @return the option count.
     */
    public int getOptionCount() {
        return this.options.size();
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
    public UICursor getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }

    private static class RadioOption {
        Component label;

        RadioOption(Component label) {
            this.label = label;
        }
    }
}
