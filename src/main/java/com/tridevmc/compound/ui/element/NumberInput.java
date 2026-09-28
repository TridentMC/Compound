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

import com.tridevmc.compound.ui.layout.*;
import com.tridevmc.compound.ui.scope.ICompositionScope;
import com.tridevmc.compound.ui.state.State;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A numeric editor composed around a retained {@link TextInput}.
 * Incomplete input such as a minus sign leaves the numeric value unchanged. Valid edits
 * update the clamped numeric value; committing rewrites the text to that value.
 * Decimals are allowed by default, and the initial range is unbounded.
 */
public class NumberInput extends Element implements IComposableElement {

    private final TextInput textInput;
    private final State<Double> value = State.of(0.0);
    private double minValue = Double.NEGATIVE_INFINITY;
    private double maxValue = Double.POSITIVE_INFINITY;
    private boolean allowDecimals = true;
    private Consumer<Double> onValueChanged;

    /**
     * Creates an editor displaying zero.
     */
    public NumberInput() {
        this.textInput = new TextInput("0");
        this.setupFilter();
    }

    /**
     * Creates an editor with the given finite value.
     *
     * @param initialValue the initial value.
     * @throws IllegalArgumentException if the value is not finite.
     */
    public NumberInput(double initialValue) {
        this.textInput = new TextInput();
        this.setupFilter();
        this.setValue(initialValue);
    }

    private void setupFilter() {
        this.textInput.setFilter(text -> {
            if (text == null || text.isEmpty()) return true;
            if (text.equals("-") && this.minValue < 0) return true;
            if (this.allowDecimals && text.equals(".")) return true;
            if (this.allowDecimals && this.minValue < 0 && text.equals("-.")) return true;

            try {
                double val = Double.parseDouble(text);
                return Double.isFinite(val) && (this.allowDecimals || val == Math.rint(val));
            } catch (NumberFormatException e) {
                return false;
            }
        });

        this.textInput.setResponder(text -> {
            if (text == null || text.isEmpty() || text.equals("-") || text.equals(".") ||
                    text.equals("-.")) {
                return;
            }

            try {
                double val = Double.parseDouble(text);
                double clamped = this.clamp(val);
                if (Double.compare(this.value.get(), clamped) != 0) {
                    this.value.set(clamped);
                    if (this.onValueChanged != null) this.onValueChanged.accept(clamped);
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.textInput.setOnCommit(text -> {
            try {
                this.setValue(Double.parseDouble(text));
            } catch (NumberFormatException ignored) {
                this.setValue(this.value.get());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void compose(ICompositionScope scope) {
        scope.e(this.textInput);
    }

    /**
     * Clamps and formats a value through the text editor, notifying the callback if it changes.
     *
     * @param value the finite value to set.
     * @throws IllegalArgumentException if the value is not finite.
     */
    public void setValue(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Numeric input value must be finite");
        double clamped = this.clamp(value);
        this.textInput.setValue(this.allowDecimals ? String.valueOf(clamped) : String.format(Locale.ROOT, "%.0f", clamped));
    }

    private double clamp(double value) {
        double clamped = Math.clamp(value, this.minValue, this.maxValue);
        if (!this.allowDecimals) {
            clamped = Math.clamp(Math.rint(clamped), Math.ceil(this.minValue), Math.floor(this.maxValue));
        }
        return clamped;
    }

    /**
     * Returns the last valid numeric value, independent of incomplete editing text.
     *
     * @return the current clamped value.
     */
    public double getValue() {
        return this.value.get();
    }

    /**
     * Rounds the current value and converts it to an int.
     *
     * @return the rounded value cast to int.
     */
    public int getIntValue() {
        return (int) Math.round(this.value.get());
    }

    /**
     * Sets the inclusive range and reapplies it to the current value.
     *
     * @param min the lower bound; negative infinity permits no lower bound.
     * @param max the upper bound; positive infinity permits no upper bound.
     * @throws IllegalArgumentException if the bounds are unordered, NaN, or contain no permitted value.
     */
    public void setRange(double min, double max) {
        if (Double.isNaN(min) || Double.isNaN(max) || min > max || min == Double.POSITIVE_INFINITY || max == Double.NEGATIVE_INFINITY
                || (!this.allowDecimals && Math.ceil(min) > Math.floor(max))) {
            throw new IllegalArgumentException("Numeric input range must be ordered and contain a permitted value");
        }
        this.minValue = min;
        this.maxValue = max;
        this.setValue(this.value.get());
    }

    /**
     * Changes whether fractional values are allowed and reformats the current value.
     * Integer mode rounds to the nearest integer using ties-to-even rounding.
     *
     * @param allow whether to allow decimals.
     * @throws IllegalArgumentException if integer mode is requested for a range containing no integer.
     */
    public void setAllowDecimals(boolean allow) {
        if (!allow && Math.ceil(this.minValue) > Math.floor(this.maxValue)) {
            throw new IllegalArgumentException("Numeric input range contains no integer");
        }
        this.allowDecimals = allow;
        this.setValue(this.value.get());
    }

    /**
     * Replaces the callback for valid numeric changes, including programmatic changes.
     *
     * @param onValueChanged the callback, or null to remove it.
     */
    public void setOnValueChanged(Consumer<Double> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }

    /**
     * Returns the owned editor for presentation and interaction configuration.
     * Replacing its filter, responder, or commit handler overrides numeric validation or synchronization.
     *
     * @return the retained child editor; do not compose it separately.
     */
    public TextInput getTextInput() {
        return this.textInput;
    }

    /**
     * Sets the child editor's text length limit.
     *
     * @param maxLength the nonnegative maximum UTF-16 length.
     * @throws IllegalArgumentException if maxLength is negative.
     */
    public void setMaxLength(int maxLength) {
        this.textInput.setMaxLength(maxLength);
    }

    /**
     * Sets the hint shown while the child editor is empty.
     *
     * @param hint the hint, or null to omit it.
     */
    public void setHint(Component hint) {
        this.textInput.setHint(hint);
    }

    /** {@inheritDoc} */
    @Override
    public Size measure(Constraints constraints, LayoutProperties props, List<Size> measuredChildren) {
        if (!measuredChildren.isEmpty()) {
            return measuredChildren.get(0);
        }
        return new Size(
                Math.min(200, constraints.maxWidth()),
                Math.min(20, constraints.maxHeight())
        );
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
