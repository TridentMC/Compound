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

import com.tridevmc.compound.ui.layout.Alignment;
import com.tridevmc.compound.ui.layout.Bounds;
import com.tridevmc.compound.ui.layout.Constraints;
import com.tridevmc.compound.ui.layout.LayoutProperties;
import com.tridevmc.compound.ui.layout.Size;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A single selectable row inside a list, composed from higher-level elements.
 */
public class ListItem extends BaseElement implements IComposableElement {

    private final Component label;
    private final Supplier<Boolean> selected;
    private final Supplier<Boolean> hovered;
    private final Supplier<Integer> selectedBackgroundColor;
    private final Supplier<Integer> hoveredBackgroundColor;
    private final Supplier<Integer> selectedTextColor;
    private final Supplier<Integer> defaultTextColor;
    private final List<Runnable> clickHandlers = new ArrayList<>();
    private Runnable hoverEnterHandler = () -> {};
    private Runnable hoverExitHandler = () -> {};

    /**
     * Creates a list item with dynamic selection/hover colors.
     *
     * @param label                   the item label
     * @param selected                supplier for whether this item is selected
     * @param hovered                 supplier for whether this item is hovered
     * @param selectedBackgroundColor background color when selected
     * @param hoveredBackgroundColor  background color when hovered
     * @param selectedTextColor       text color when selected/hovered
     * @param defaultTextColor        text color when neither selected nor hovered
     */
    public ListItem(Component label,
                    Supplier<Boolean> selected,
                    Supplier<Boolean> hovered,
                    Supplier<Integer> selectedBackgroundColor,
                    Supplier<Integer> hoveredBackgroundColor,
                    Supplier<Integer> selectedTextColor,
                    Supplier<Integer> defaultTextColor) {
        this.label = label;
        this.selected = selected;
        this.hovered = hovered;
        this.selectedBackgroundColor = selectedBackgroundColor;
        this.hoveredBackgroundColor = hoveredBackgroundColor;
        this.selectedTextColor = selectedTextColor;
        this.defaultTextColor = defaultTextColor;
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.onClick(event -> {
            if (event.button() != 0) return false;
            this.clickHandlers.forEach(Runnable::run);
            return true;
        });
        scope.onMouseEnter(() -> this.hoverEnterHandler.run());
        scope.onMouseExit(() -> this.hoverExitHandler.run());

        scope.e(new Stack(), stack -> {
            stack.layout().fillMax();

            stack.e(new Rect(() -> {
                if (this.selected.get()) return this.selectedBackgroundColor.get();
                if (this.hovered.get()) return this.hoveredBackgroundColor.get();
                return 0x00000000;
            }), bg -> bg.layout().fillMax());

            stack.e(new Box(), content -> {
                content.layout().fillMax().margin(4, 0, 4, 0).contentAlignment(Alignment.CENTER_LEFT);
                content.e(new Label(this.label,
                        () -> this.selected.get() || this.hovered.get()
                                ? this.selectedTextColor.get() : this.defaultTextColor.get(), () -> true));
            });
        });
    }

    public void setClickHandler(Runnable handler) {
        this.clickHandlers.add(handler);
    }

    public void setHoverHandlers(Runnable enter, Runnable exit) {
        this.hoverEnterHandler = enter;
        this.hoverExitHandler = exit;
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
}
