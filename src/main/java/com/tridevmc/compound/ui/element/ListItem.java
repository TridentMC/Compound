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
import java.util.Objects;
import java.util.function.Supplier;

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

    private ListItem(Builder builder) {
        this.label = builder.label;
        this.selected = builder.selected;
        this.hovered = builder.hovered;
        this.selectedBackgroundColor = builder.selectedBackgroundColor;
        this.hoveredBackgroundColor = builder.hoveredBackgroundColor;
        this.selectedTextColor = builder.selectedTextColor;
        this.defaultTextColor = builder.defaultTextColor;
    }

    public static Builder builder(Component label) {
        return new Builder(label);
    }

    public static final class Builder {
        private final Component label;
        private Supplier<Boolean> selected = () -> false;
        private Supplier<Boolean> hovered = () -> false;
        private Supplier<Integer> selectedBackgroundColor = () -> 0xFF606060;
        private Supplier<Integer> hoveredBackgroundColor = () -> 0xFF404040;
        private Supplier<Integer> selectedTextColor = () -> 0xFFFFFFFF;
        private Supplier<Integer> defaultTextColor = () -> 0xFFFFFFFF;

        private Builder(Component label) {
            this.label = Objects.requireNonNull(label);
        }

        public Builder selected(Supplier<Boolean> selected) {
            this.selected = Objects.requireNonNull(selected);
            return this;
        }

        public Builder hovered(Supplier<Boolean> hovered) {
            this.hovered = Objects.requireNonNull(hovered);
            return this;
        }

        public Builder selectedBackgroundColor(Supplier<Integer> color) {
            this.selectedBackgroundColor = Objects.requireNonNull(color);
            return this;
        }

        public Builder hoveredBackgroundColor(Supplier<Integer> color) {
            this.hoveredBackgroundColor = Objects.requireNonNull(color);
            return this;
        }

        public Builder selectedTextColor(Supplier<Integer> color) {
            this.selectedTextColor = Objects.requireNonNull(color);
            return this;
        }

        public Builder defaultTextColor(Supplier<Integer> color) {
            this.defaultTextColor = Objects.requireNonNull(color);
            return this;
        }

        public ListItem build() {
            return new ListItem(this);
        }
    }

    @Override
    public void compose(ICompositionScope scope) {
        scope.onClick(event -> {
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
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
