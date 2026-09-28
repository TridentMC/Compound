/*
 * Copyright 2018 - 2026 TridentMC
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
import java.util.function.IntSupplier;
import java.util.function.BooleanSupplier;

/**
 * A selectable-looking list row configured through {@link #builder(Component)}.
 * Selection, hover, and colors are supplied by the owner; the row does not change those
 * values itself. Click and hover callbacks let a list coordinate its rows.
 */
public class ListItem extends Element implements IComposableElement {

    private final Component label;
    private final BooleanSupplier selected;
    private final BooleanSupplier hovered;
    private final IntSupplier selectedBackgroundColor;
    private final IntSupplier hoveredBackgroundColor;
    private final IntSupplier selectedTextColor;
    private final IntSupplier defaultTextColor;
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

    /**
     * Starts a row builder with unselected, unhovered defaults and white text.
     *
     * @param label the row label.
     * @return a new builder.
     * @throws NullPointerException if the label is null.
     */
    public static Builder builder(Component label) {
        return new Builder(label);
    }

    /**
     * Configures a list row using live state and color suppliers.
     * Unselected rows are transparent; selected and hovered backgrounds default to gray.
     */
    public static final class Builder {
        private final Component label;
        private BooleanSupplier selected = () -> false;
        private BooleanSupplier hovered = () -> false;
        private IntSupplier selectedBackgroundColor = () -> 0xFF606060;
        private IntSupplier hoveredBackgroundColor = () -> 0xFF404040;
        private IntSupplier selectedTextColor = () -> 0xFFFFFFFF;
        private IntSupplier defaultTextColor = () -> 0xFFFFFFFF;

        private Builder(Component label) {
            this.label = Objects.requireNonNull(label);
        }

        /**
         * Sets the live selection supplier; the default is false.
         *
         * @param selected the non-null supplier.
         * @return this builder.
         * @throws NullPointerException if the supplier is null.
         */
        public Builder selected(BooleanSupplier selected) {
            this.selected = Objects.requireNonNull(selected);
            return this;
        }

        /**
         * Sets the live hover supplier; the default is false.
         *
         * @param hovered the non-null supplier.
         * @return this builder.
         * @throws NullPointerException if the supplier is null.
         */
        public Builder hovered(BooleanSupplier hovered) {
            this.hovered = Objects.requireNonNull(hovered);
            return this;
        }

        /**
         * Sets the ARGB background supplier used when selected.
         *
         * @param color the non-null supplier.
         * @return this builder.
         * @throws NullPointerException if the supplier is null.
         */
        public Builder selectedBackgroundColor(IntSupplier color) {
            this.selectedBackgroundColor = Objects.requireNonNull(color);
            return this;
        }

        /**
         * Sets the ARGB background supplier used when hovered but not selected.
         *
         * @param color the non-null supplier.
         * @return this builder.
         * @throws NullPointerException if the supplier is null.
         */
        public Builder hoveredBackgroundColor(IntSupplier color) {
            this.hoveredBackgroundColor = Objects.requireNonNull(color);
            return this;
        }

        /**
         * Sets the text color supplier used when selected or hovered.
         *
         * @param color the non-null supplier.
         * @return this builder.
         * @throws NullPointerException if the supplier is null.
         */
        public Builder selectedTextColor(IntSupplier color) {
            this.selectedTextColor = Objects.requireNonNull(color);
            return this;
        }

        /**
         * Sets the text color supplier used when neither selected nor hovered.
         *
         * @param color the non-null supplier.
         * @return this builder.
         * @throws NullPointerException if the supplier is null.
         */
        public Builder defaultTextColor(IntSupplier color) {
            this.defaultTextColor = Objects.requireNonNull(color);
            return this;
        }

        /**
         * Creates a row retaining the configured suppliers.
         *
         * @return a new list row.
         */
        public ListItem build() {
            return new ListItem(this);
        }
    }

    /** {@inheritDoc} */
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
                if (this.selected.getAsBoolean()) return this.selectedBackgroundColor.getAsInt();
                if (this.hovered.getAsBoolean()) return this.hoveredBackgroundColor.getAsInt();
                return 0x00000000;
            }), bg -> bg.layout().fillMax());

            stack.e(new Box(), content -> {
                content.layout().fillMax().margin(4, 0, 4, 0).contentAlignment(Alignment.CENTER_LEFT);
                content.e(new Label(this.label,
                        () -> this.selected.getAsBoolean() || this.hovered.getAsBoolean()
                                ? this.selectedTextColor.getAsInt() : this.defaultTextColor.getAsInt(), () -> true));
            });
        });
    }

    /**
     * Adds a left-click handler; existing handlers remain registered.
     *
     * @param handler the action to run on a left click.
     */
    public void setClickHandler(Runnable handler) {
        this.clickHandlers.add(handler);
    }

    /**
     * Replaces the pointer entry and exit handlers.
     *
     * @param enter the action on pointer entry.
     * @param exit the action on pointer exit.
     */
    public void setHoverHandlers(Runnable enter, Runnable exit) {
        this.hoverEnterHandler = enter;
        this.hoverExitHandler = exit;
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
}
