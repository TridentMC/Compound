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

public class RadioButtonGroup extends BaseElement implements IComposableElement {

    private static final int SPACING = 4;
    private static final int BOX_SIZE = 17;
    private static final IScreenSprite BOX = IScreenSprite.of(Identifier.withDefaultNamespace("widget/checkbox"));
    private static final IScreenSprite SELECTED = IScreenSprite.of(Identifier.withDefaultNamespace("widget/checkbox_selected"));
    private static final IScreenSprite HIGHLIGHTED = IScreenSprite.of(Identifier.withDefaultNamespace("widget/checkbox_highlighted"));
    private static final IScreenSprite SELECTED_HIGHLIGHTED = IScreenSprite.of(Identifier.withDefaultNamespace("widget/checkbox_selected_highlighted"));

    private final State<Integer> selectedIndex = new StateImpl<>(-1);
    private final State<Boolean> enabled = new StateImpl<>(true);
    private final List<RadioOption> options = Lists.newArrayList();
    private final List<State<Boolean>> optionHoverStates = Lists.newArrayList();
    private Consumer<Integer> onSelectionChanged;
    private int optionSpacing = 4;
    private int labelColor = 0xFFFFFF;

    public RadioButtonGroup() {
    }

    @Override
    public void compose(ICompositionScope scope) {

        while (this.optionHoverStates.size() < this.options.size()) {
            this.optionHoverStates.add(new StateImpl<>(false));
        }
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

        scope.e(new Column(), column -> {
            column.layout().spacing(this.optionSpacing);

            for (int i = 0; i < this.options.size(); i++) {
                final int index = i;
                RadioOption option = this.options.get(i);
                State<Boolean> hoverState = this.optionHoverStates.get(index);

                column.e(new Row(), row -> {
                    row.layout().fixedHeight(BOX_SIZE).spacing(SPACING).verticalAlignment(Alignment.CENTER);

                    row.onMouseEnter(() -> hoverState.set(true));
                    row.onMouseExit(() -> hoverState.set(false));

                    row.e(new Sprite(() -> {
                        boolean selected = this.selectedIndex.get() == index;
                        boolean highlighted = this.enabled.get()
                                && (hoverState.get() || scope.isFocused() && selected);
                        return selected ? highlighted ? SELECTED_HIGHLIGHTED : SELECTED
                                : highlighted ? HIGHLIGHTED : BOX;
                    }), sprite -> sprite.layout().fixedSize(BOX_SIZE, BOX_SIZE));

                    row.e(new Label(option.label, () -> {
                        if (!this.enabled.get()) return 0x808080;
                        return this.labelColor;
                    }, () -> false));

                    row.onClick(event -> {
                        if (!this.enabled.get() || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
                        scope.requestFocus();
                        this.select(index);
                        return true;
                    });
                });
            }
        });
    }

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

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= this.options.size()) return;
        if (this.selectedIndex.get() != index) {
            this.selectedIndex.set(index);
            if (this.onSelectionChanged != null) {
                this.onSelectionChanged.accept(index);
            }
        }
    }

    public void addOption(String label) {
        this.options.add(new RadioOption(Component.literal(label)));
        this.optionHoverStates.add(new StateImpl<>(false));
        this.invalidateComposition();
    }

    public void addOption(Component label) {
        this.options.add(new RadioOption(label));
        this.optionHoverStates.add(new StateImpl<>(false));
        this.invalidateComposition();
    }

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

    public int getSelectedIndex() {
        return this.selectedIndex.get();
    }

    public void setOnSelectionChanged(Consumer<Integer> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public void setOptionSpacing(int spacing) {
        this.optionSpacing = spacing;
        this.invalidateComposition();
    }

    public void setLabelColor(int color) {
        this.labelColor = color;
    }

    public int getOptionCount() {
        return this.options.size();
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
        return new Size(0, 0);
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

    private static class RadioOption {
        Component label;

        RadioOption(Component label) {
            this.label = label;
        }
    }
}
