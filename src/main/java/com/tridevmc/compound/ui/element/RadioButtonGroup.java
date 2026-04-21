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
 * A group of radio buttons for single selection from mutually exclusive options.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * scope.e(new RadioButtonGroup(), group -> {
 *     group.getElement().addOption("Option 1");
 *     group.getElement().addOption("Option 2");
 *     group.getElement().addOption("Option 3");
 *     group.getElement().setSelectedIndex(0);
 *     group.getElement().setOnSelectionChanged(index -> {
 *         System.out.println("Selected: " + index);
 *     });
 * });
 * </pre>
 */
public class RadioButtonGroup extends BaseElement implements IComposableElement {

    private static final int RADIO_SIZE = 12;
    private static final int SPACING = 4;

    private final State<Integer> selectedIndex = new StateImpl<>(-1);
    private final State<Boolean> enabled = new StateImpl<>(true);
    private final List<RadioOption> options = Lists.newArrayList();
    private Consumer<Integer> onSelectionChanged;
    private int optionSpacing = 8;

    public RadioButtonGroup() {
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.bind(this.enabled);
        scope.bind(this.selectedIndex);

        scope.e(new Column(), column -> {
            column.layout().spacing(this.optionSpacing);

            for (int i = 0; i < this.options.size(); i++) {
                final int index = i;
                RadioOption option = this.options.get(i);

                column.e(new Row(), row -> {
                    row.layout().spacing(SPACING).verticalAlignment(Alignment.CENTER);

                    row.e(new Stack(), radioStack -> {
                        radioStack.layout().fixedSize(RADIO_SIZE, RADIO_SIZE);

                        int outerColor = this.enabled.get() ? 0xFFFFFFFF : 0xFF808080;
                        radioStack.e(new Rect(outerColor), outer -> outer.layout().fillMax());

                        if (this.selectedIndex.get() == index) {
                            int innerColor = this.enabled.get() ? 0xFF3366CC : 0xFF404040;
                            radioStack.e(new Rect(innerColor), inner -> inner.layout().margin(3).fillMax());
                        }
                    });

                    row.e(new Label(option.label, () -> this.enabled.get() ? 0xFFFFFF : 0x808080, () -> false));

                    scope.onClick(event -> {
                        if (!this.enabled.get()) return false;
                        this.select(index);
                        return true;
                    });
                });
            }
        });
    }

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

    public void addOption(String label) {
        this.options.add(new RadioOption(Component.literal(label)));
    }

    public void addOption(Component label) {
        this.options.add(new RadioOption(label));
    }

    public void removeOption(int index) {
        if (index >= 0 && index < this.options.size()) {
            this.options.remove(index);
            if (this.selectedIndex.get() == index) {
                this.selectedIndex.set(-1);
            } else if (this.selectedIndex.get() > index) {
                this.selectedIndex.set(this.selectedIndex.get() - 1);
            }
        }
    }

    public void setSelectedIndex(int index) {
        this.select(index);
    }

    public int getSelectedIndex() {
        return this.selectedIndex.get();
    }

    public void setOnSelectionChanged(Consumer<Integer> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public void setOptionSpacing(int spacing) {
        this.optionSpacing = spacing;
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
    public CursorType getCursor(int x, int y) {
        return this.enabled.get() ? CompoundCursors.HAND : null;
    }

    private static class RadioOption {
        Component label;

        RadioOption(Component label) {
            this.label = label;
        }
    }
}
